package com.example.graphqlclient.client;

import com.example.graphqlclient.model.StockDto;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * MarketDataClient — typed GraphQL client for market-data-service.
 *
 * GraphQL requests are plain HTTP POST with a JSON body:
 *   { "query": "...", "variables": { ... } }
 *
 * The response always follows the GraphQL envelope:
 *   { "data": { ... }, "errors": [...] }
 *
 * We use Spring's RestClient (synchronous, Servlet-stack) with Jackson for
 * serialisation — no WebFlux or reactive types involved.
 *
 * Each method extracts the relevant field from "data" and maps it to typed DTOs.
 */
@Component
public class MarketDataClient {

    // GraphQL query/response envelope keys
    private static final String DATA = "data";

    private final RestClient restClient;

    public MarketDataClient(RestClient marketDataRestClient) {
        this.restClient = marketDataRestClient;
    }

    // ── stocks ────────────────────────────────────────────────────────────────

    /**
     * Queries all stocks, optionally filtered by exchange.
     * Null exchange means "no filter" — the GraphQL schema treats it as absent.
     */
    public List<StockDto> fetchStocks(String exchange) {
        String query = """
                query Stocks($exchange: String) {
                    stocks(exchange: $exchange) {
                        symbol
                        name
                        exchange
                        currentPrice
                        currency
                    }
                }
                """;

        Map<String, Object> response = post(query, Map.of("exchange", exchange == null ? "" : exchange));

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.get(DATA);

        return parseList((List<?>) data.get("stocks"));
    }

    // ── stock (with price history) ────────────────────────────────────────────

    /**
     * Queries a single stock by symbol, including its 5 most recent price history entries.
     * Returns null if the GraphQL response contains errors (e.g. stock not found).
     */
    public StockDto fetchStock(String symbol) {
        String query = """
                query Stock($symbol: String!) {
                    stock(symbol: $symbol) {
                        symbol
                        name
                        exchange
                        currentPrice
                        currency
                        marketCap
                        priceHistory(limit: 5) {
                            price
                            timestamp
                            changePercent
                        }
                    }
                }
                """;

        Map<String, Object> response = post(query, Map.of("symbol", symbol));

        if (response.containsKey("errors")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> errors = (List<Map<String, Object>>) response.get("errors");
            if (!errors.isEmpty()) {
                String msg = (String) errors.get(0).get("message");
                throw new GraphQlClientException(msg);
            }
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.get(DATA);

        @SuppressWarnings("unchecked")
        Map<String, Object> stockMap = (Map<String, Object>) data.get("stock");

        return stockMap == null ? null : StockDto.fromMap(stockMap);
    }

    // ── HTTP transport ────────────────────────────────────────────────────────

    private Map<String, Object> post(String query, Map<String, Object> variables) {
        // GraphQL over HTTP: POST with JSON body { query, variables }
        Map<String, Object> body = Map.of("query", query, "variables", variables);

        return restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    // ── Response mapping helpers ──────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<StockDto> parseList(List<?> raw) {
        if (raw == null) return List.of();
        return raw.stream()
                .map(item -> StockDto.fromMap((Map<String, Object>) item))
                .toList();
    }

    /**
     * Thrown when the GraphQL response contains errors (e.g. "Stock not found").
     */
    public static class GraphQlClientException extends RuntimeException {
        public GraphQlClientException(String message) {
            super(message);
        }
    }
}
