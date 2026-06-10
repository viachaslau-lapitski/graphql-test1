package com.example.graphqlclient.controller;

import com.example.graphqlclient.client.Demo1MarketDataClient;
import com.example.graphqlclient.model.StockDto;
import org.springframework.graphql.client.FieldAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Demo1Controller — same scenarios as DemoController but uses HttpSyncGraphQlClient
 * from Spring for GraphQL instead of a raw RestClient.
 *
 * Routes: /demo1/stocks, /demo1/stock/{symbol}
 *
 * Run:
 *   ./gradlew :market-data-service:bootRun
 *   ./gradlew :graphql-client-service:bootRun
 *
 *   curl http://localhost:8083/demo1/stocks
 *   curl "http://localhost:8083/demo1/stocks?exchange=NASDAQ"
 *   curl http://localhost:8083/demo1/stock/AAPL
 *   curl -i http://localhost:8083/demo1/stock/XYZ   → 404
 */
@RestController
@RequestMapping("/demo1")
public class Demo1Controller {

    private final Demo1MarketDataClient client;

    public Demo1Controller(Demo1MarketDataClient client) {
        this.client = client;
    }

    @GetMapping("/stocks")
    public List<StockDto> stocks(@RequestParam(required = false) String exchange) {
        return client.fetchStocks(exchange);
    }

    @GetMapping("/stock/{symbol}")
    public ResponseEntity<?> stock(@PathVariable String symbol) {
        try {
            StockDto stock = client.fetchStock(symbol);
            if (stock == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(stock);
        } catch (FieldAccessException ex) {
            String message = ex.getResponse().getErrors().isEmpty()
                    ? "Stock not found: " + symbol
                    : ex.getResponse().getErrors().get(0).getMessage();
            return ResponseEntity.status(404).body(Map.of("error", message));
        }
    }
}
