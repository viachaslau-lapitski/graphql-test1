package com.example.marketdata.projection;

import java.math.BigDecimal;

/**
 * StockSummary — a Spring Data interface projection.
 *
 * WHAT IS A PROJECTION?
 * Instead of loading the full Stock entity (symbol, name, exchange, currentPrice,
 * currency, marketCap, ...), Spring Data can generate a SQL query that only
 * SELECTs the columns you declare here. This is a database-level optimization.
 *
 * HOW IT CONNECTS TO GRAPHQL:
 * When a GraphQL client queries `stockSummaries { symbol name currentPrice }`,
 * only those 3 columns are fetched from the database. This demonstrates how
 * GraphQL's "ask only what you need" philosophy can be pushed all the way down
 * to the database query — zero over-fetching at any layer.
 *
 * If you enable `spring.jpa.show-sql=true` (see application.yml), you'll see
 * in the logs that H2 executes:
 *   SELECT symbol, name, current_price FROM stock
 * instead of:
 *   SELECT symbol, name, exchange, current_price, currency, market_cap FROM stock
 *
 * HOW IT WORKS:
 * Spring Data recognizes that when a repository method returns an interface type
 * (not the entity class itself), it should create a proxy that only exposes
 * the interface methods. Hibernate then generates a projection query.
 */
public interface StockSummary {

    /** Getter naming convention: get + PascalCase of the entity field name */
    String getSymbol();
    String getName();
    BigDecimal getCurrentPrice();
}
