package com.example.orders.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Order — a stock order (buy or sell) placed by a trader.
 *
 * This is both a JPA entity (stored in H2) and the backing type for the
 * GraphQL `Order` type. The `stock` field is NOT stored here — it's a
 * federated reference resolved via Apollo Router → market-data-service.
 *
 * IMPORTANT: "order" is a reserved SQL keyword.
 * We use @Table(name = "stock_order") to avoid SQL errors.
 *
 * N+1 PROBLEM SETUP:
 * Each Order has a `symbol` field. When a client queries:
 *   { orders { id symbol stock { name currentPrice } } }
 *
 * Without DataLoader: for N orders, we'd call the stock resolver N times.
 * With @BatchMapping in StockBatchController: all N symbols resolved ONCE.
 * See StockBatchController.java for the full explanation and demo.
 */
@Entity
@Table(name = "stock_order")
public class Order {

    @Id
    private String id;

    /**
     * The ticker symbol of the stock being traded.
     * This is the @key that ties this Order's stock to the Stock entity
     * in market-data-service via Apollo Federation.
     */
    @Column(nullable = false)
    private String symbol;

    /**
     * BUY or SELL.
     * EnumType.STRING stores "BUY"/"SELL" as strings in the DB (not ordinal integers).
     * Always prefer STRING over ORDINAL — adding enum values won't break existing data.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType type;

    /** Number of shares to buy or sell */
    @Column(nullable = false)
    private Integer quantity;

    /**
     * The limit price — the maximum price willing to pay (BUY) or
     * minimum price willing to accept (SELL). Null for market orders.
     * Nullable field in GraphQL: `limitPrice: BigDecimal` (no `!`)
     */
    @Column(name = "limit_price", precision = 19, scale = 4)
    private BigDecimal limitPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Order() {}

    public Order(String id, String symbol, OrderType type, Integer quantity,
                 BigDecimal limitPrice, OrderStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.symbol = symbol;
        this.type = type;
        this.quantity = quantity;
        this.limitPrice = limitPrice;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getId()              { return id; }
    public String getSymbol()          { return symbol; }
    public OrderType getType()         { return type; }
    public Integer getQuantity()       { return quantity; }
    public BigDecimal getLimitPrice()  { return limitPrice; }
    public OrderStatus getStatus()     { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
