package com.example.marketdata.controller;

import com.example.marketdata.model.Stock;
import com.example.marketdata.repository.StockRepository;
import org.springframework.graphql.data.federation.EntityMapping;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.stereotype.Controller;

/**
 * StockEntityController — Federation entity resolver for the Stock type.
 *
 * ──────────────────────────────────────────────────────────────────────────────
 * WHY DOES THIS EXIST?
 * ──────────────────────────────────────────────────────────────────────────────
 * In Apollo Federation, every service that declares a type with @key MUST be
 * able to re-fetch instances of that type given only its key field(s).
 *
 * The Apollo Router uses a special built-in query — _entities(representations) —
 * to do this. It passes a list of "representations", each containing the @key
 * fields (e.g., { "__typename": "Stock", "symbol": "AAPL" }), and expects the
 * service to return the full object.
 *
 * This is the "ownership" side of federation:
 *   market-data-service OWNS Stock → must resolve _entities for Stock
 *   order-service REFERENCES Stock → must also have @EntityMapping (to reconstruct
 *                                    the stub so the router can stitch fields)
 *
 * ──────────────────────────────────────────────────────────────────────────────
 * HOW IT WORKS IN SPRING GRAPHQL
 * ──────────────────────────────────────────────────────────────────────────────
 * Spring for GraphQL's @EntityMapping annotation wires this method to the
 * _entities query for the "Stock" type (inferred from return type).
 *
 * @Argument extracts the `symbol` field from the representation map.
 * Spring auto-maps __typename → the Java return type.
 *
 * This method is called by FederationSchemaFactory, which is configured in
 * GraphQlConfig.java. Without FederationSchemaFactory, the @link and @key
 * directives in schema.graphqls would cause a SchemaGenerator error.
 */
@Controller
public class StockEntityController {

    private final StockRepository stockRepository;

    public StockEntityController(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    /**
     * Resolves a Stock entity by its @key field (symbol) for federation queries.
     *
     * Called when the Apollo Router needs to expand Stock references from
     * order-service into full Stock objects. For example, when a client runs:
     *
     *   # Via the supergraph (Apollo Router on :4000):
     *   query {
     *     orders {
     *       id
     *       stock {         ← order-service has this reference
     *         symbol        ← but the Router fetches name/currentPrice from HERE
     *         name
     *         currentPrice
     *       }
     *     }
     *   }
     *
     * The Router:
     *   1. Fetches orders from order-service (gets symbol per order)
     *   2. Calls _entities([{__typename:"Stock", symbol:"AAPL"}, ...]) on THIS service
     *   3. Merges results → client sees a seamless response
     *
     * @param symbol the stock symbol from the representation (e.g., "AAPL")
     * @return the full Stock entity, or null if not found
     */
    @EntityMapping
    public Stock stock(@Argument String symbol) {
        return stockRepository.findById(symbol).orElse(null);
    }
}
