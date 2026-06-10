package com.example.marketdata.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * PriceHistory — records a snapshot of a Stock's price at a point in time.
 *
 * This entity demonstrates how GraphQL handles nested/relational data without
 * multiple round-trips. A single GraphQL query can fetch:
 *
 *   query {
 *     stock(symbol: "AAPL") {
 *       currentPrice
 *       priceHistory(limit: 5) {  ← nested field, resolved lazily
 *         price
 *         changePercent
 *         timestamp
 *       }
 *     }
 *   }
 *
 * With REST you'd need: GET /stocks/AAPL  then  GET /stocks/AAPL/price-history
 * With GraphQL: one request, exactly the fields you need.
 */
@Entity
@Table(name = "price_history")
public class PriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Foreign key to the Stock entity.
     * Note: we store the symbol string rather than a @ManyToOne JPA relationship
     * to keep the demo simpler and avoid lazy-loading complications in GraphQL.
     */
    @Column(name = "stock_symbol", nullable = false)
    private String stockSymbol;

    /** The stock price at this moment in time */
    @Column(precision = 19, scale = 4)
    private BigDecimal price;

    /** When this price was recorded */
    private OffsetDateTime timestamp;

    /**
     * Percentage change from the previous price snapshot.
     * Positive = price went up, Negative = price went down.
     */
    @Column(name = "change_percent")
    private Double changePercent;

    protected PriceHistory() {}

    public PriceHistory(String stockSymbol, BigDecimal price,
                        OffsetDateTime timestamp, Double changePercent) {
        this.stockSymbol = stockSymbol;
        this.price = price;
        this.timestamp = timestamp;
        this.changePercent = changePercent;
    }

    public Long getId()               { return id; }
    public String getStockSymbol()    { return stockSymbol; }
    public BigDecimal getPrice()      { return price; }
    public OffsetDateTime getTimestamp() { return timestamp; }
    public Double getChangePercent()  { return changePercent; }
}
