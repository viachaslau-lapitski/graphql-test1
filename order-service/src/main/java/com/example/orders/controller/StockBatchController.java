package com.example.orders.controller;

import com.example.orders.model.Order;
import com.example.orders.model.Stock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

/**
 * StockBatchController — demonstrates the DataLoader pattern with @BatchMapping.
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 * THE N+1 PROBLEM — A CRITICAL GRAPHQL PERFORMANCE PITFALL
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * Consider this GraphQL query:
 *   {
 *     orders(status: OPEN) {   ← returns, say, 5 orders
 *       id
 *       symbol
 *       stock {                ← for EACH order, resolve its stock
 *         symbol
 *       }
 *     }
 *   }
 *
 * PROBLEM (naive @SchemaMapping):
 *   GraphQL resolves fields individually. Without batching, it would:
 *   - 1 query to fetch all 5 orders (from OrderController)
 *   - 5 separate calls to resolve `Order.stock` (once per order)
 *   = 1 + 5 = 6 total calls  ← this is the "N+1 problem"
 *
 *   For N=100 orders, that's 101 calls. For N=1000 orders, 1001 calls.
 *   This gets very slow very fast.
 *
 * SOLUTION (@BatchMapping — the DataLoader pattern):
 *   @BatchMapping changes the resolver signature from:
 *     stock(Order order) → called N times for N orders
 *   to:
 *     stock(List<Order> orders) → called ONCE with ALL N orders
 *
 *   Spring for GraphQL uses graphql-java's DataLoader under the hood.
 *   It intercepts the individual resolver calls, accumulates them in a batch,
 *   then fires ONE batch call at the end of the execution step.
 *
 * HOW TO SEE THE DIFFERENCE:
 *   1. Look at the logs when you run:
 *      { orders(status: OPEN) { id symbol stock { symbol } } }
 *   2. You'll see ONE log line: "BatchMapping called with 4 orders"
 *   3. Without @BatchMapping, you'd see 4 separate resolver invocations
 *
 * IN THIS DEMO:
 *   The stock field just returns a stub with the symbol (because the full stock
 *   data lives in market-data-service). In a real system, this would be a batched
 *   HTTP call or database query that fetches all stocks at once.
 *
 * IN THE FEDERATION SETUP (with Apollo Router):
 *   The Router handles the cross-service batching automatically — it collects all
 *   stock references from the orders and calls market-data-service's _entities query
 *   ONCE with all symbols. @BatchMapping is still valuable for local field resolution.
 * ═══════════════════════════════════════════════════════════════════════════════
 */
@Controller
public class StockBatchController {

    private static final Logger log = LoggerFactory.getLogger(StockBatchController.class);

    /**
     * Resolves: Order.stock for ALL orders in a batch.
     *
     * @BatchMapping auto-detects:
     *   - typeName = "Order"     (from the first param type List<Order>)
     *   - field    = "stock"     (from the method name)
     *
     * The return type Map<Order, Stock> maps each input Order to its resolved Stock.
     * Spring for GraphQL distributes the map values back to the individual orders.
     *
     * WATCH THE LOGS: When you query `orders { stock { symbol } }`, you should see
     * exactly ONE log line like "BatchMapping called with N orders — single batch call!"
     */
    @BatchMapping
    public Map<Order, Stock> stock(List<Order> orders) {
        log.info("🔄 @BatchMapping: resolving stocks for {} orders — single batch call! " +
                        "(symbols: {})",
                orders.size(),
                orders.stream().map(Order::getSymbol).distinct().collect(Collectors.toList()));

        // In this demo: create a Stock stub for each order (using just the symbol).
        // In a real microservice setup: make ONE HTTP call to market-data-service
        // with all symbols, then map results back to orders.
        return orders.stream()
                .collect(Collectors.toMap(
                        order -> order,              // key: the Order
                        order -> new Stock(order.getSymbol())  // value: the resolved Stock stub
                ));
    }
}
