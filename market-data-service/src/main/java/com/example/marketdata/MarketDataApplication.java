package com.example.marketdata;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Market Data Service — Spring Boot Application entry point.
 *
 * This service is a GraphQL subgraph in an Apollo Federation setup.
 * It owns the "Stock" entity (identified by symbol) and provides:
 *   - Query: stocks, stock(symbol)
 *   - Query: stockSummaries (demonstrates @ProjectedPayload projections)
 *   - Subscription: priceUpdated(symbol) — real-time price ticks via SSE
 *
 * Running standalone:  ./gradlew :market-data-service:bootRun
 * GraphiQL IDE:        http://localhost:8081/graphiql
 * Actuator metrics:    http://localhost:8081/actuator/metrics
 */
@SpringBootApplication
public class MarketDataApplication {
    public static void main(String[] args) {
        SpringApplication.run(MarketDataApplication.class, args);
    }
}
