package com.example.graphqlclient.client;

import com.example.graphqlclient.model.PriceHistoryDto;
import com.example.graphqlclient.model.StockDto;
import org.springframework.graphql.client.FieldAccessException;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Demo1MarketDataClient — same queries as MarketDataClient but implemented
 * using Spring for GraphQL's HttpSyncGraphQlClient.
 *
 * The critical difference from the failed earlier attempt:
 *   Use retrieveSync(path)  → RetrieveSyncSpec  → toEntityList() returns List<T> directly
 *   NOT retrieve(path)      → RetrieveSpec       → toEntityList() returns Mono<List<T>>
 *
 * HttpSyncGraphQlClient handles:
 *   - JSON serialisation of { query, variables } request body
 *   - JSON deserialisation of { data, errors } response envelope
 *   - Mapping response fields directly to typed Java records (via Jackson)
 *   - Surfacing GraphQL field errors as FieldAccessException
 */
@Component
public class Demo1MarketDataClient {

    private final HttpSyncGraphQlClient graphQlClient;

    public Demo1MarketDataClient(HttpSyncGraphQlClient marketDataSyncGraphQlClient) {
        this.graphQlClient = marketDataSyncGraphQlClient;
    }

    // ── stocks ────────────────────────────────────────────────────────────────

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

        return graphQlClient.document(query)
                .variable("exchange", exchange)
                .retrieveSync("stocks")          // sync variant — returns RetrieveSyncSpec
                .toEntityList(StockDto.class);   // returns List<StockDto> directly, no Mono
    }

    // ── stock (with price history) ────────────────────────────────────────────

    /**
     * Returns null if the stock field is null with no errors.
     * Throws FieldAccessException if the GraphQL response contains errors (e.g. not found).
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

        return graphQlClient.document(query)
                .variable("symbol", symbol)
                .retrieveSync("stock")           // sync variant
                .toEntity(StockDto.class);        // returns StockDto directly, no Mono
    }
}
