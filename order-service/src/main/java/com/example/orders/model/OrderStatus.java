package com.example.orders.model;

/**
 * OrderStatus — the lifecycle state of a stock order.
 *
 * A typical order lifecycle:
 *   PENDING → OPEN → FILLED
 *             OPEN → CANCELLED
 *
 * PENDING:   Order received but not yet submitted to the exchange
 * OPEN:      Order is live on the exchange, waiting to be matched
 * FILLED:    Order was fully executed (shares were bought or sold)
 * CANCELLED: Order was cancelled before being filled
 *
 * GRAPHQL FILTERING DEMO:
 *   query { orders(status: OPEN) { id symbol quantity } }
 *   query { orders(status: FILLED) { id symbol limitPrice } }
 */
public enum OrderStatus {
    PENDING,
    OPEN,
    FILLED,
    CANCELLED
}
