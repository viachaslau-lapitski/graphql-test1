package com.example.graphqlclient.controller;

import com.example.graphqlclient.client.MarketDataClient;
import com.example.graphqlclient.model.StockDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * DemoController — REST API that demonstrates GraphQL client usage.
 *
 * Each endpoint triggers a GraphQL query to market-data-service via MarketDataClient,
 * which uses Spring's RestClient under the hood. Fully synchronous — no Mono/Flux.
 *
 * Run market-data-service first:  ./gradlew :market-data-service:bootRun
 * Then this service:              ./gradlew :graphql-client-service:bootRun
 *
 * Demo endpoints:
 *   GET http://localhost:8083/demo/stocks
 *   GET http://localhost:8083/demo/stocks?exchange=NASDAQ
 *   GET http://localhost:8083/demo/stock/AAPL
 *   GET http://localhost:8083/demo/stock/XYZ  → 404 (not found)
 */
@RestController
@RequestMapping("/demo")
public class DemoController {

    private final MarketDataClient marketDataClient;

    public DemoController(MarketDataClient marketDataClient) {
        this.marketDataClient = marketDataClient;
    }

    /**
     * Returns all stocks, optionally filtered by exchange.
     *
     * Demonstrates:
     *   - Parameterised GraphQL query with variables
     *   - List response mapped to typed DTOs
     */
    @GetMapping("/stocks")
    public List<StockDto> stocks(@RequestParam(required = false) String exchange) {
        return marketDataClient.fetchStocks(exchange);
    }

    /**
     * Returns a single stock with its last 5 price history entries.
     *
     * Demonstrates:
     *   - Nested GraphQL field selection (stock + priceHistory in one query)
     *   - Error handling: GraphQL "Stock not found" error → HTTP 404
     */
    @GetMapping("/stock/{symbol}")
    public ResponseEntity<?> stock(@PathVariable String symbol) {
        try {
            StockDto stock = marketDataClient.fetchStock(symbol);
            if (stock == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(stock);
        } catch (MarketDataClient.GraphQlClientException ex) {
            return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
        }
    }
}

