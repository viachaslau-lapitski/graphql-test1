package com.example.marketdata.config;

import com.example.marketdata.model.PriceHistory;
import com.example.marketdata.model.Stock;
import com.example.marketdata.repository.PriceHistoryRepository;
import com.example.marketdata.repository.StockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * DataInitializer — seeds the H2 in-memory database with realistic stock data.
 *
 * ApplicationRunner runs once after the Spring context is fully started.
 * Since H2 is in-memory with ddl-auto=create-drop, data is gone when the app stops
 * and re-seeded fresh on every restart — great for demos.
 *
 * We include stocks from both NYSE and NASDAQ to demonstrate GraphQL argument
 * filtering: stocks(exchange: "NYSE") vs stocks(exchange: "NASDAQ")
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final StockRepository stockRepository;
    private final PriceHistoryRepository priceHistoryRepository;

    public DataInitializer(StockRepository stockRepository,
                           PriceHistoryRepository priceHistoryRepository) {
        this.stockRepository = stockRepository;
        this.priceHistoryRepository = priceHistoryRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Seeding market data...");

        // ── Stocks ─────────────────────────────────────────────────────────────
        var aapl = new Stock("AAPL", "Apple Inc.", "NASDAQ",
                new BigDecimal("189.30"), "USD", new BigDecimal("2950000000000.00"));
        var msft = new Stock("MSFT", "Microsoft Corporation", "NASDAQ",
                new BigDecimal("415.20"), "USD", new BigDecimal("3090000000000.00"));
        var googl = new Stock("GOOGL", "Alphabet Inc.", "NASDAQ",
                new BigDecimal("170.15"), "USD", new BigDecimal("2100000000000.00"));
        var tsla = new Stock("TSLA", "Tesla Inc.", "NASDAQ",
                new BigDecimal("177.80"), "USD", new BigDecimal("568000000000.00"));
        var jpm = new Stock("JPM", "JPMorgan Chase & Co.", "NYSE",
                new BigDecimal("195.40"), "USD", new BigDecimal("563000000000.00"));
        var gs = new Stock("GS", "Goldman Sachs Group Inc.", "NYSE",
                new BigDecimal("463.75"), "USD", new BigDecimal("158000000000.00"));
        var bac = new Stock("BAC", "Bank of America Corp", "NYSE",
                new BigDecimal("36.90"), "USD", new BigDecimal("288000000000.00"));

        stockRepository.saveAll(java.util.List.of(aapl, msft, googl, tsla, jpm, gs, bac));

        // ── Price History — last 6 hours for AAPL (for subscription demo) ──────
        OffsetDateTime now = OffsetDateTime.now();
        priceHistoryRepository.saveAll(java.util.List.of(
                new PriceHistory("AAPL", new BigDecimal("187.50"), now.minusHours(6),  -0.95),
                new PriceHistory("AAPL", new BigDecimal("188.20"), now.minusHours(5),   0.37),
                new PriceHistory("AAPL", new BigDecimal("188.90"), now.minusHours(4),   0.37),
                new PriceHistory("AAPL", new BigDecimal("187.80"), now.minusHours(3),  -0.58),
                new PriceHistory("AAPL", new BigDecimal("188.60"), now.minusHours(2),   0.43),
                new PriceHistory("AAPL", new BigDecimal("189.30"), now.minusHours(1),   0.37),
                new PriceHistory("MSFT", new BigDecimal("412.00"), now.minusHours(6),  -0.77),
                new PriceHistory("MSFT", new BigDecimal("413.50"), now.minusHours(4),   0.36),
                new PriceHistory("MSFT", new BigDecimal("415.20"), now.minusHours(2),   0.41),
                new PriceHistory("JPM",  new BigDecimal("193.10"), now.minusHours(6),  -1.18),
                new PriceHistory("JPM",  new BigDecimal("194.20"), now.minusHours(3),   0.57),
                new PriceHistory("JPM",  new BigDecimal("195.40"), now.minusHours(1),   0.62),
                new PriceHistory("GS",   new BigDecimal("460.00"), now.minusHours(5),  -0.81),
                new PriceHistory("GS",   new BigDecimal("463.75"), now.minusHours(2),   0.82)
        ));

        log.info("Seeded {} stocks and {} price history records",
                stockRepository.count(), priceHistoryRepository.count());
    }
}
