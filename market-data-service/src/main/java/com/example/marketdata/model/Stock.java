package com.example.marketdata.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Stock — the core entity of this subgraph.
 *
 * FEDERATION NOTE: This entity is declared with @key(fields: "symbol") in
 * the GraphQL schema. This means "symbol" is the unique identifier that the
 * Apollo Router uses to reference a Stock from other subgraphs (e.g., order-service).
 *
 * JPA NOTE: The JPA entity doubles as the GraphQL type. In a more complex app
 * you'd have separate DTO/GraphQL types, but for this demo they are the same class.
 *
 * OVER-FETCHING NOTE: Even though this JPA entity has ALL fields, GraphQL clients
 * can choose to request only what they need. The GraphQL schema is the contract —
 * not the Java class structure.
 */
@Entity
@Table(name = "stock")
public class Stock {

    /**
     * The stock ticker symbol — serves as both the primary key in the DB
     * and the @key field in the Apollo Federation schema.
     * Example values: "AAPL", "MSFT", "GOOGL"
     */
    @Id
    private String symbol;

    /** Full company name */
    private String name;

    /** Exchange where the stock is listed: "NYSE", "NASDAQ" */
    private String exchange;

    /**
     * Current price — using BigDecimal, NOT Double/Float.
     * Financial calculations must use exact decimal arithmetic.
     * Float/Double have binary representation errors (0.1 + 0.2 ≠ 0.3 in float).
     */
    @Column(name = "current_price", precision = 19, scale = 4)
    private BigDecimal currentPrice;

    /** ISO 4217 currency code, e.g. "USD" */
    private String currency;

    /**
     * Market capitalization — a large number (trillions for big companies).
     * BigDecimal handles arbitrarily large precise numbers.
     */
    @Column(name = "market_cap", precision = 25, scale = 2)
    private BigDecimal marketCap;

    // ── JPA requires a no-arg constructor ─────────────────────────────────────
    protected Stock() {}

    public Stock(String symbol, String name, String exchange,
                 BigDecimal currentPrice, String currency, BigDecimal marketCap) {
        this.symbol = symbol;
        this.name = name;
        this.exchange = exchange;
        this.currentPrice = currentPrice;
        this.currency = currency;
        this.marketCap = marketCap;
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public String getSymbol()        { return symbol; }
    public String getName()          { return name; }
    public String getExchange()      { return exchange; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public String getCurrency()      { return currency; }
    public BigDecimal getMarketCap() { return marketCap; }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }
}
