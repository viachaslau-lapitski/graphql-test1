package com.example.orders.model;

/**
 * OrderType — whether the order is to buy or sell the stock.
 *
 * GraphQL ENUM: This Java enum maps directly to the GraphQL enum type:
 *   enum OrderType { BUY SELL }
 *
 * Enums in GraphQL are serialized as strings ("BUY", "SELL") in JSON.
 * In the schema, clients can use them directly:
 *   query { orders(status: OPEN) { type quantity } }
 */
public enum OrderType {
    BUY,   // Client wants to purchase shares
    SELL   // Client wants to sell shares
}
