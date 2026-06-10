package com.example.orders.controller;

import com.example.orders.model.Stock;
import org.springframework.graphql.data.federation.EntityMapping;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.stereotype.Controller;

/**
 * StockEntityController — handles Apollo Federation entity resolution for Stock.
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 * HOW APOLLO FEDERATION ENTITY RESOLUTION WORKS
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * Apollo Federation enables a "schema stitching" pattern where a single GraphQL
 * query can span multiple services. Here's what happens step-by-step when a
 * client queries through the Apollo Router:
 *
 * 1. Client sends:
 *    { orders { id symbol stock { name currentPrice } } }
 *    to Apollo Router at http://localhost:4000/graphql
 *
 * 2. Router's query plan (generated from the supergraph schema):
 *    Step 1: Fetch { orders { id symbol } } from order-service
 *            → router gets back orders with symbols: ["AAPL", "AAPL", "MSFT", ...]
 *    Step 2: Fetch _entities for those stocks from market-data-service:
 *            { _entities(representations: [{__typename: "Stock", symbol: "AAPL"}, ...]) {
 *                ... on Stock { name currentPrice }
 *            }}
 *    Step 3: Merge the results and return to client
 *
 * 3. @EntityMapping in order-service handles the edge case where the router
 *    needs to confirm that order-service "knows" a Stock by its symbol.
 *    For Stock, since all data lives in market-data-service, we just return the stub.
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 * WHY THIS SERVICE NEEDS AN @EntityMapping FOR STOCK
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * The order-service schema declares:
 *   type Stock @key(fields: "symbol") { symbol: String! }
 *
 * This tells the Router: "order-service participates in resolving Stock entities
 * by symbol." The @EntityMapping here is what fulfills that promise — it's called
 * when the Router sends an _entities query with a Stock representation.
 *
 * Without this, the Router would complain that the @key is declared but no
 * entity resolver is present.
 * ═══════════════════════════════════════════════════════════════════════════════
 */
@Controller
public class StockEntityController {

    /**
     * Resolves the Stock entity from its @key representation.
     *
     * The Apollo Router calls this via the hidden _entities query:
     *   { _entities(representations: [{ __typename: "Stock", symbol: "AAPL" }]) {
     *       ... on Stock { symbol }
     *   }}
     *
     * Spring for GraphQL's @EntityMapping annotation handles the _entities query
     * automatically — you just implement the business logic.
     *
     * @param symbol — the @key field extracted from the entity representation
     */
    @EntityMapping
    public Stock stock(@Argument String symbol) {
        // In order-service, Stock is just a reference — we only know its symbol.
        // The actual Stock data (name, price, etc.) is resolved by market-data-service.
        return new Stock(symbol);
    }
}
