package com.example.marketdata.repository;

import com.example.marketdata.model.PriceHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * PriceHistoryRepository — data access for historical price records.
 *
 * The `priceHistory(limit: Int = 10)` argument in GraphQL maps to the
 * Pageable parameter here. Clients can ask for as many or few historical
 * records as they need — this is GraphQL's argument-driven pagination.
 */
@Repository
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

    /**
     * Finds price history for a specific stock, newest records first.
     *
     * Generated SQL: SELECT * FROM price_history
     *                WHERE stock_symbol = ?
     *                ORDER BY timestamp DESC
     *                LIMIT ?
     *
     * @param stockSymbol  the ticker symbol (e.g., "AAPL")
     * @param pageable     controls the LIMIT — driven by the GraphQL `limit` argument
     */
    List<PriceHistory> findByStockSymbolOrderByTimestampDesc(String stockSymbol, Pageable pageable);
}
