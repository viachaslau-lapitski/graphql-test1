package com.example.orders.model;

/**
 * Stock — a Federation stub in order-service.
 *
 * APOLLO FEDERATION: In a federated schema, entities can span multiple services.
 * The "Stock" type is OWNED by market-data-service (it has all the fields there).
 * In order-service, Stock is just a reference — we only know its symbol (@key field).
 *
 * Why does order-service need a Stock class at all?
 *   - The Order entity has a `stock` field: `{ ..., stock: Stock }`
 *   - When the Apollo Router resolves an Order's stock field, it:
 *     1. Gets the symbol from order-service
 *     2. Calls market-data-service's _entities query with that symbol
 *     3. Merges the result back into the Order response
 *
 * The @EntityMapping in StockEntityController.java handles step 2.
 * This class is the "representation" — just the @key field(s).
 */
public class Stock {

    /** The @key field — the only field order-service knows about a Stock */
    private final String symbol;

    public Stock(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }
}
