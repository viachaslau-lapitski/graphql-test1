package com.example.orders.controller;

import com.example.orders.model.Order;
import com.example.orders.model.OrderStatus;
import com.example.orders.repository.OrderRepository;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * OrderController — resolves the Query type fields for order-service.
 *
 * This controller demonstrates:
 *
 * 1. ENUM ARGUMENT: `orders(status: OrderStatus)` — GraphQL enums map directly
 *    to Java enums. Spring converts the string "OPEN" → OrderStatus.OPEN.
 *
 * 2. OPTIONAL ARGUMENT: `status` is nullable in the schema (no `!`).
 *    If the client omits it, Spring passes null, and we return all orders.
 *
 * 3. UNDER-FETCHING SOLUTION: Each Order has a `stock` field that is resolved
 *    by StockBatchController via @BatchMapping (see that class for N+1 explanation).
 *    In the full federation setup, the Apollo Router resolves Order.stock by
 *    calling market-data-service. Either way, the client gets stock data in ONE query.
 */
@Controller
public class OrderController {

    private final OrderRepository orderRepository;

    public OrderController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Resolves: Query { orders(status: OrderStatus): [Order!]! }
     *
     * Demo queries (try these in GraphiQL at http://localhost:8082/graphiql):
     *
     *   # All orders — notice the schema lets you pick exactly which fields to include
     *   { orders { id symbol type quantity status } }
     *
     *   # Filter by status
     *   { orders(status: OPEN) { id symbol quantity limitPrice } }
     *
     *   # Combined with nested stock data (N+1 demo — see StockBatchController)
     *   { orders(status: OPEN) { id symbol quantity stock { symbol } } }
     */
    @QueryMapping
    public List<Order> orders(@Argument OrderStatus status) {
        if (status != null) {
            return orderRepository.findByStatus(status);
        }
        return orderRepository.findAll();
    }

    /**
     * Resolves: Query { order(id: ID!): Order }
     *
     * Returns null (not an error) if the order doesn't exist.
     * In GraphQL, returning null for a nullable field is valid.
     * For a non-nullable field (with `!`), returning null would cause a GraphQL error.
     *
     * Demo: { order(id: "ORD-006") { id symbol type quantity status } }
     */
    @QueryMapping
    public Order order(@Argument String id) {
        return orderRepository.findById(id).orElse(null);
    }
}
