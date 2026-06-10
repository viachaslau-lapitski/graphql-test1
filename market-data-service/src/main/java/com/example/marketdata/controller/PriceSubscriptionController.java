package com.example.marketdata.controller;

import com.example.marketdata.model.PriceHistory;
import com.example.marketdata.model.Stock;
import com.example.marketdata.repository.StockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Random;

/**
 * PriceSubscriptionController — demonstrates GraphQL Subscriptions.
 *
 * WHAT IS A GRAPHQL SUBSCRIPTION?
 * Unlike a Query (one-shot request/response), a Subscription is a long-lived
 * connection where the server pushes data to the client as events occur.
 *
 * GraphQL has three operation types:
 *   query        → fetch data once (read)
 *   mutation     → change data (write)
 *   subscription → stream data continuously (real-time)
 *
 * TRANSPORT: Spring for GraphQL uses Server-Sent Events (SSE) over HTTP.
 * SSE is a one-way push channel from server to client — simpler than WebSockets
 * for this use case (no bidirectional communication needed).
 *
 * HOW TO TEST:
 *   1. Open GraphiQL at http://localhost:8081/graphiql
 *   2. Run: subscription { priceUpdated(symbol: "AAPL") { price changePercent timestamp } }
 *   3. Watch price ticks arrive in real-time every second
 *
 * HOW IT WORKS INTERNALLY:
 *   - The method returns Flux<PriceHistory> — a Reactor stream
 *   - Spring for GraphQL subscribes to this Flux and emits each item as an SSE event
 *   - When the client disconnects, the Flux is cancelled (no resource leak)
 *
 * PRODUCTION NOTE: In a real system, the Flux would not use Flux.interval() —
 * it would subscribe to a message broker (Kafka, Redis Pub/Sub, RabbitMQ) or
 * a reactive database change stream. See README.md for architecture notes.
 */
@Controller
public class PriceSubscriptionController {

    private static final Logger log = LoggerFactory.getLogger(PriceSubscriptionController.class);
    private static final Random random = new Random();

    private final StockRepository stockRepository;

    public PriceSubscriptionController(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    /**
     * Resolves: Subscription { priceUpdated(symbol: String!): PriceHistory! }
     *
     * @SubscriptionMapping works like @QueryMapping but the method must return
     * a reactive Publisher type (Flux<T> or Mono<T>).
     *
     * The Flux emits one PriceHistory item per second, simulating live price data.
     * Each tick generates a small random price movement (±1% of current price).
     */
    @SubscriptionMapping
    public Flux<PriceHistory> priceUpdated(@Argument String symbol) {
        log.info("[Subscription] Client subscribed to price updates for {}", symbol);

        // Fetch the initial stock price from H2
        Stock stock = stockRepository.findById(symbol)
                .orElseThrow(() -> new IllegalArgumentException("Stock not found: " + symbol));

        // Create a mutable reference to track the "current" price across ticks
        // (a simple simulation of real price movement)
        BigDecimal[] currentPrice = {stock.getCurrentPrice()};

        return Flux
                // Emit a tick every second (index 0, 1, 2, ...)
                .interval(Duration.ofSeconds(1))
                .map(tick -> {
                    // Simulate a small price movement: ±0.5% of current price
                    double changeRatio = (random.nextDouble() - 0.5) * 0.01; // -0.5% to +0.5%
                    BigDecimal change = currentPrice[0]
                            .multiply(BigDecimal.valueOf(changeRatio))
                            .setScale(4, RoundingMode.HALF_UP);

                    BigDecimal newPrice = currentPrice[0].add(change).setScale(4, RoundingMode.HALF_UP);
                    double changePercent = changeRatio * 100;

                    currentPrice[0] = newPrice; // track for next tick

                    log.debug("[Subscription] {} tick={} price={}", symbol, tick, newPrice);
                    return new PriceHistory(symbol, newPrice, OffsetDateTime.now(), changePercent);
                })
                // Log when the client disconnects (Flux is cancelled)
                .doOnCancel(() -> log.info("[Subscription] Client unsubscribed from {}", symbol));
    }
}
