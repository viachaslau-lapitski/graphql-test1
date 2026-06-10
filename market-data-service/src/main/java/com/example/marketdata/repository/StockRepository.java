package com.example.marketdata.repository;

import com.example.marketdata.model.Stock;
import com.example.marketdata.projection.StockSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * StockRepository — Spring Data JPA repository for Stock entities.
 *
 * By extending JpaRepository<Stock, String>, we get standard CRUD operations
 * for free: findById, findAll, save, deleteById, etc.
 *
 * Spring Data's method name convention:
 *   findBy[FieldName][Condition](param) → generates the SQL automatically.
 * Example: findByExchange("NYSE") → SELECT * FROM stock WHERE exchange = 'NYSE'
 */
@Repository
public interface StockRepository extends JpaRepository<Stock, String> {

    /**
     * Finds all stocks on the given exchange.
     * Spring Data generates: SELECT * FROM stock WHERE exchange = ?
     *
     * Used by GraphQL query: stocks(exchange: "NYSE") { ... }
     */
    List<Stock> findByExchange(String exchange);

    /**
     * Projection query — only fetches symbol, name, currentPrice columns.
     *
     * Spring Data recognizes StockSummary as a projection interface and generates:
     *   SELECT s.symbol, s.name, s.current_price FROM stock s
     *
     * This is the database-level optimization that complements GraphQL's
     * field-selection at the API level. See StockSummary.java for full explanation.
     */
    List<StockSummary> findAllProjectedBy();
}
