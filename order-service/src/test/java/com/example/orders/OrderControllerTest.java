package com.example.orders;

import com.example.orders.config.DataInitializer;
import com.example.orders.config.GraphQlConfig;
import com.example.orders.controller.OrderController;
import com.example.orders.controller.StockBatchController;
import com.example.orders.controller.StockEntityController;
import com.example.orders.model.Order;
import com.example.orders.model.OrderStatus;
import com.example.orders.model.OrderType;
import com.example.orders.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * OrderControllerTest — GraphQL layer tests for order-service.
 *
 * @GraphQlTest slice only loads: controllers, schema files, RuntimeWiringConfigurer.
 * All repository dependencies are mocked — no real database used in tests.
 */
@GraphQlTest(controllers = {OrderController.class, StockBatchController.class, StockEntityController.class})
@Import(GraphQlConfig.class)  // brings in RuntimeWiringConfigurer with custom scalars
class OrderControllerTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private OrderRepository orderRepository;
    @MockitoBean
    private DataInitializer dataInitializer;

    private Order sampleOrder(String id, String symbol, OrderStatus status) {
        return new Order(id, symbol, OrderType.BUY, 100,
                new BigDecimal("188.50"), status, LocalDateTime.now());
    }

    @Test
    void orders_returnsAllOrders() {
        given(orderRepository.findAll()).willReturn(List.of(
                sampleOrder("ORD-001", "AAPL", OrderStatus.FILLED),
                sampleOrder("ORD-006", "AAPL", OrderStatus.OPEN)
        ));

        graphQlTester.document("""
                    {
                        orders {
                            id
                            symbol
                            type
                            status
                        }
                    }
                """)
                .execute()
                .errors().verify()
                .path("orders").entityList(java.util.Map.class)
                .hasSize(2)
                .satisfies(orders -> {
                    assertThat(orders.get(0).get("symbol")).isEqualTo("AAPL");
                    assertThat(orders.get(0).get("type")).isEqualTo("BUY");
                });
    }

    @Test
    void orders_filteredByStatus() {
        given(orderRepository.findByStatus(OrderStatus.OPEN)).willReturn(List.of(
                sampleOrder("ORD-006", "AAPL", OrderStatus.OPEN),
                sampleOrder("ORD-007", "MSFT", OrderStatus.OPEN)
        ));

        // GRAPHQL ENUM ARGUMENT: status is passed as a plain identifier (no quotes)
        graphQlTester.document("""
                    {
                        orders(status: OPEN) {
                            id
                            status
                        }
                    }
                """)
                .execute()
                .errors().verify()
                .path("orders").entityList(java.util.Map.class)
                .hasSize(2)
                .satisfies(orders -> {
                    assertThat(orders.get(0).get("status")).isEqualTo("OPEN");
                    assertThat(orders.get(1).get("status")).isEqualTo("OPEN");
                });
    }

    @Test
    void order_singleById() {
        given(orderRepository.findById("ORD-006")).willReturn(
                Optional.of(sampleOrder("ORD-006", "AAPL", OrderStatus.OPEN))
        );

        // Test the nested stock field — @BatchMapping in StockBatchController handles this
        graphQlTester.document("""
                    {
                        order(id: "ORD-006") {
                            id
                            symbol
                            stock {
                                symbol
                            }
                        }
                    }
                """)
                .execute()
                .errors().verify()
                .path("order.id").entity(String.class).isEqualTo("ORD-006")
                .path("order.stock.symbol").entity(String.class).isEqualTo("AAPL");
    }

    @Test
    void orders_batchMapping_stockFieldResolved() {
        // N+1 DEMO TEST: multiple orders, each gets stock resolved via @BatchMapping
        given(orderRepository.findAll()).willReturn(List.of(
                sampleOrder("ORD-001", "AAPL", OrderStatus.FILLED),
                sampleOrder("ORD-006", "MSFT", OrderStatus.OPEN),
                sampleOrder("ORD-007", "GOOGL", OrderStatus.OPEN)
        ));

        // All three orders' stock fields should be resolved in ONE @BatchMapping call
        // (verify in logs: "BatchMapping called with 3 orders — single batch call!")
        graphQlTester.document("""
                    {
                        orders {
                            id
                            symbol
                            stock {
                                symbol
                            }
                        }
                    }
                """)
                .execute()
                .errors().verify()
                .path("orders[0].stock.symbol").entity(String.class).isEqualTo("AAPL")
                .path("orders[1].stock.symbol").entity(String.class).isEqualTo("MSFT")
                .path("orders[2].stock.symbol").entity(String.class).isEqualTo("GOOGL");
    }
}
