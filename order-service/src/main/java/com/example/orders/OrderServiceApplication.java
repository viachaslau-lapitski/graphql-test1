package com.example.orders;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Order Service — Spring Boot Application entry point.
 *
 * This service is a GraphQL subgraph in an Apollo Federation setup.
 * It owns the "Order" entity and demonstrates:
 *   - Query: orders(status), order(id)
 *   - @BatchMapping: solves the N+1 problem for Order.stock field
 *   - @EntityMapping: handles Apollo Federation entity resolution for Stock
 *
 * Running standalone:  ./gradlew :order-service:bootRun
 * GraphiQL IDE:        http://localhost:8082/graphiql
 * Actuator metrics:    http://localhost:8082/actuator/metrics
 *
 * Federation demo (full stack):
 *   docker compose up    → starts both services + Apollo Router
 *   Apollo Router:       http://localhost:4000/graphql
 */
@SpringBootApplication
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
