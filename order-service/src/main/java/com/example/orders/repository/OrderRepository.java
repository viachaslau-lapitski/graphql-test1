package com.example.orders.repository;

import com.example.orders.model.Order;
import com.example.orders.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * OrderRepository — Spring Data JPA repository for the Order entity.
 *
 * The key method here is findByStatus — it maps to the GraphQL argument:
 *   query { orders(status: OPEN) { ... } }
 *
 * Spring Data generates the SQL from the method name:
 *   SELECT * FROM stock_order WHERE status = 'OPEN'
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

    /**
     * Find all orders with the given status.
     * Called by OrderController.orders() when the client provides a status argument.
     *
     * Generated SQL: SELECT * FROM stock_order WHERE status = ?
     */
    List<Order> findByStatus(OrderStatus status);

    /**
     * Find orders by a list of stock symbols.
     * This is useful for the DataLoader pattern — fetch orders for multiple
     * symbols in a single query instead of one query per symbol.
     *
     * Generated SQL: SELECT * FROM stock_order WHERE symbol IN (?, ?, ...)
     */
    List<Order> findBySymbolIn(List<String> symbols);
}
