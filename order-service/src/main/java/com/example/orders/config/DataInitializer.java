package com.example.orders.config;

import com.example.orders.model.Order;
import com.example.orders.model.OrderStatus;
import com.example.orders.model.OrderType;
import com.example.orders.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DataInitializer — seeds the H2 in-memory database with realistic order data.
 *
 * We create orders for different stocks and in different statuses to enable
 * filtering demos:
 *   query { orders(status: OPEN) { ... } }     ← only open orders
 *   query { orders(status: FILLED) { ... } }   ← only filled orders
 *   query { orders { ... } }                   ← all orders
 *
 * Multiple orders reference the same stock (e.g., several AAPL orders)
 * to demonstrate the N+1 problem when resolving Order.stock fields.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final OrderRepository orderRepository;

    public DataInitializer(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Seeding order data...");

        LocalDateTime now = LocalDateTime.now();

        orderRepository.saveAll(List.of(
                // ── Filled orders (historical trades) ───────────────────────────
                new Order("ORD-001", "AAPL", OrderType.BUY,  100,
                        new BigDecimal("188.50"), OrderStatus.FILLED, now.minusDays(3)),
                new Order("ORD-002", "MSFT", OrderType.SELL,  50,
                        new BigDecimal("420.00"), OrderStatus.FILLED, now.minusDays(2)),
                new Order("ORD-003", "GOOGL", OrderType.BUY,  30,
                        new BigDecimal("168.00"), OrderStatus.FILLED, now.minusDays(1)),
                new Order("ORD-004", "AAPL", OrderType.SELL,  25,
                        new BigDecimal("192.00"), OrderStatus.FILLED, now.minusDays(1)),
                new Order("ORD-005", "JPM",  OrderType.BUY,  75,
                        new BigDecimal("194.00"), OrderStatus.FILLED, now.minusHours(20)),

                // ── Open orders (active, waiting to be matched) ─────────────────
                // Multiple AAPL orders to demonstrate N+1 batching
                new Order("ORD-006", "AAPL", OrderType.BUY,  200,
                        new BigDecimal("186.00"), OrderStatus.OPEN, now.minusHours(5)),
                new Order("ORD-007", "MSFT", OrderType.BUY,  80,
                        new BigDecimal("410.00"), OrderStatus.OPEN, now.minusHours(4)),
                new Order("ORD-008", "TSLA", OrderType.SELL, 60,
                        new BigDecimal("180.00"), OrderStatus.OPEN, now.minusHours(3)),
                new Order("ORD-009", "GS",   OrderType.BUY,  15,
                        new BigDecimal("460.00"), OrderStatus.OPEN, now.minusHours(2)),
                new Order("ORD-010", "AAPL", OrderType.BUY,  50,
                        new BigDecimal("187.50"), OrderStatus.OPEN, now.minusHours(1)),

                // ── Pending orders (not yet submitted to exchange) ───────────────
                new Order("ORD-011", "BAC",  OrderType.BUY,  300,
                        new BigDecimal("36.50"), OrderStatus.PENDING, now.minusMinutes(30)),
                new Order("ORD-012", "JPM",  OrderType.SELL, 40,
                        new BigDecimal("198.00"), OrderStatus.PENDING, now.minusMinutes(15)),

                // ── Cancelled orders ─────────────────────────────────────────────
                new Order("ORD-013", "TSLA", OrderType.BUY,  100,
                        new BigDecimal("175.00"), OrderStatus.CANCELLED, now.minusDays(2)),
                new Order("ORD-014", "GOOGL", OrderType.SELL, 20,
                        null,                     OrderStatus.CANCELLED, now.minusDays(1)) // market order
        ));

        log.info("Seeded {} orders", orderRepository.count());
    }
}
