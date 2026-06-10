package com.example.marketdata.controller;

import com.example.marketdata.exception.StockNotFoundException;
import com.example.marketdata.model.PriceHistory;
import com.example.marketdata.model.Stock;
import com.example.marketdata.projection.StockSummary;
import com.example.marketdata.repository.PriceHistoryRepository;
import com.example.marketdata.repository.StockRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * StockController — GraphQL resolver for the Query type in market-data-service.
 *
 * Spring for GraphQL uses annotation-based controllers similar to Spring MVC:
 *
 *   @QueryMapping  ← maps to a field in the "Query" type of your schema
 *   @SchemaMapping ← maps to a field in any named type (like Stock.priceHistory)
 *   @Argument      ← extracts a GraphQL argument (like ?symbol=AAPL in REST)
 *
 * No XML, no bean wiring — just annotate a Spring @Controller method.
 *
 * NAMING CONVENTION: By default the method name must match the GraphQL field name.
 * E.g., method `stocks()` maps to `Query.stocks` in the schema.
 * You can override with @QueryMapping(name = "...").
 */
@Controller
public class StockController {

    private final StockRepository stockRepository;
    private final PriceHistoryRepository priceHistoryRepository;

    public StockController(StockRepository stockRepository,
                           PriceHistoryRepository priceHistoryRepository) {
        this.stockRepository = stockRepository;
        this.priceHistoryRepository = priceHistoryRepository;
    }

    // ── Query.stocks ──────────────────────────────────────────────────────────

    /**
     * Resolves: Query { stocks(exchange: String): [Stock!]! }
     *
     * GRAPHQL ARGUMENT: The `exchange` parameter is optional — the schema declares
     * it as nullable (no `!`). GraphQL passes null if the client omits it.
     *
     * OVER-FETCHING DEMO:
     *   REST: GET /api/stocks → always returns ALL fields for ALL stocks
     *   GraphQL: query { stocks { symbol currentPrice } } → returns only 2 fields
     *
     * Try these in GraphiQL at http://localhost:8081/graphiql:
     *
     *   # All stocks, all fields
     *   { stocks { symbol name exchange currentPrice currency marketCap } }
     *
     *   # Only prices (no over-fetching!)
     *   { stocks { symbol currentPrice } }
     *
     *   # Filter by exchange
     *   { stocks(exchange: "NASDAQ") { symbol currentPrice } }
     */
    @QueryMapping
    public List<Stock> stocks(@Argument String exchange, @Argument Integer limit) {
        List<Stock> result;
        if (exchange != null && !exchange.isBlank()) {
            result = stockRepository.findByExchange(exchange);
        } else {
            result = stockRepository.findAll();
        }
        if (limit != null && limit > 0) {
            result = result.stream().limit(limit).toList();
        }
        return result;
    }

    // ── Query.stock ───────────────────────────────────────────────────────────

    /**
     * Resolves: Query { stock(symbol: String!): Stock }
     *
     * GRAPHQL ERRORS: When the stock is not found, we throw StockNotFoundException.
     * The CustomExceptionResolver converts it to a structured GraphQL error:
     *   { "errors": [{ "message": "Stock not found: XYZ", "extensions": {...} }] }
     *
     * Notice: the HTTP response is still 200 OK — errors are part of the GraphQL
     * protocol, not HTTP status codes.
     */
    @QueryMapping
    public Stock stock(@Argument String symbol) {
        return stockRepository.findById(symbol)
                .orElseThrow(() -> new StockNotFoundException(symbol));
    }

    // ── Query.stockSummaries ──────────────────────────────────────────────────

    /**
     * Resolves: Query { stockSummaries: [StockSummary!]! }
     *
     * PROJECTION DEMO: Returns only a subset of Stock fields (symbol, name, price).
     * Spring Data generates a SELECT with only those 3 columns — not all columns.
     *
     * With spring.jpa.show-sql=true in application.yml, you can see the difference:
     *   This method:  SELECT s.symbol, s.name, s.current_price FROM stock s
     *   stocks():     SELECT s.symbol, s.name, s.exchange, ... FROM stock s
     *
     * Try: query { stockSummaries { symbol name currentPrice } }
     */
    @QueryMapping
    public List<StockSummary> stockSummaries() {
        return stockRepository.findAllProjectedBy();
    }

    // ── Stock.priceHistory ────────────────────────────────────────────────────

    /**
     * Resolves: Stock { priceHistory(limit: Int = 10): [PriceHistory!]! }
     *
     * @SchemaMapping connects this method to the "priceHistory" field of the "Stock" type.
     * The `stock` parameter receives the parent Stock object that was already resolved.
     *
     * UNDER-FETCHING DEMO:
     *   REST: GET /stocks/AAPL, then GET /stocks/AAPL/price-history (2 requests)
     *   GraphQL: ONE query fetches both in a single request:
     *
     *   {
     *     stock(symbol: "AAPL") {
     *       currentPrice
     *       priceHistory(limit: 3) {
     *         price
     *         timestamp
     *         changePercent
     *       }
     *     }
     *   }
     *
     * The `limit` argument maps to a database LIMIT clause via PageRequest.
     * Default is 10 (declared in schema with `limit: Int = 10`).
     */
    @SchemaMapping
    public List<PriceHistory> priceHistory(Stock stock, @Argument int limit) {
        return priceHistoryRepository
                .findByStockSymbolOrderByTimestampDesc(stock.getSymbol(), PageRequest.of(0, limit));
    }
}
