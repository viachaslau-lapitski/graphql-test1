package com.example.marketdata;

import com.example.marketdata.config.DataInitializer;
import com.example.marketdata.config.GraphQlConfig;
import com.example.marketdata.controller.StockController;
import com.example.marketdata.controller.StockEntityController;
import com.example.marketdata.model.Stock;
import com.example.marketdata.projection.StockSummary;
import com.example.marketdata.repository.PriceHistoryRepository;
import com.example.marketdata.repository.StockRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * StockControllerTest — tests the GraphQL layer in isolation.
 *
 * @GraphQlTest is a slice test annotation (like @WebMvcTest but for GraphQL):
 *   - Only loads GraphQL-related beans (@Controller, schema files, RuntimeWiringConfigurer)
 *   - Does NOT load the full Spring context or database
 *   - Uses @MockitoBean to provide fake implementations for dependencies
 *
 * This tests that:
 *   1. The GraphQL schema is correctly wired to our controllers
 *   2. The right fields are returned in the response
 *   3. Error handling works as expected
 *
 * GraphQlTester is the test client — it sends GraphQL documents and
 * provides fluent assertions on the response JSON path.
 */
@GraphQlTest(controllers = {StockController.class, StockEntityController.class})
@Import(GraphQlConfig.class)  // brings in RuntimeWiringConfigurer with custom scalars (BigDecimal, DateTime)
class StockControllerTest {

    @Autowired
    private GraphQlTester graphQlTester;

    // Mock the repositories — @GraphQlTest does not load JPA/database
    @MockitoBean
    private StockRepository stockRepository;
    @MockitoBean
    private PriceHistoryRepository priceHistoryRepository;
    @MockitoBean
    private DataInitializer dataInitializer; // avoid ApplicationRunner side effects

    @Test
    void stocks_returnsAllStocks() {
        // ARRANGE: mock repository to return two stocks
        given(stockRepository.findAll()).willReturn(List.of(
                new Stock("AAPL", "Apple Inc.", "NASDAQ",
                        new BigDecimal("189.30"), "USD", new BigDecimal("2950000000000")),
                new Stock("JPM", "JPMorgan Chase & Co.", "NYSE",
                        new BigDecimal("195.40"), "USD", new BigDecimal("563000000000"))
        ));

        // ACT & ASSERT: execute GraphQL query and verify response
        // GraphQlTester uses JSON path syntax ("$.data.stocks[0].symbol")
        graphQlTester.document("""
                    query {
                        stocks {
                            symbol
                            name
                            exchange
                        }
                    }
                """)
                .execute()
                .errors().verify()          // assert no GraphQL errors
                .path("stocks").entityList(java.util.Map.class)
                .hasSize(2)
                .satisfies(stocks -> {
                    assertThat(stocks.get(0).get("symbol")).isEqualTo("AAPL");
                    assertThat(stocks.get(1).get("symbol")).isEqualTo("JPM");
                });
    }

    @Test
    void stock_returnsCorrectStock() {
        // ARRANGE
        given(stockRepository.findById("MSFT")).willReturn(Optional.of(
                new Stock("MSFT", "Microsoft Corporation", "NASDAQ",
                        new BigDecimal("415.20"), "USD", new BigDecimal("3090000000000"))
        ));
        given(priceHistoryRepository.findByStockSymbolOrderByTimestampDesc(any(), any()))
                .willReturn(List.of());

        // ACT & ASSERT
        graphQlTester.document("""
                    query {
                        stock(symbol: "MSFT") {
                            symbol
                            name
                            currentPrice
                        }
                    }
                """)
                .execute()
                .errors().verify()
                .path("stock.symbol").entity(String.class).isEqualTo("MSFT")
                .path("stock.currentPrice").entity(String.class).isEqualTo("415.20");
    }

    @Test
    void stock_notFound_returnsGraphQLError() {
        // ARRANGE: stock does not exist
        given(stockRepository.findById("XYZ")).willReturn(Optional.empty());

        // ACT & ASSERT: expect a GraphQL error (not an exception, not HTTP 404)
        // GraphQL errors appear in the "errors" array while data may still be partial
        graphQlTester.document("""
                    query {
                        stock(symbol: "XYZ") {
                            symbol
                            name
                        }
                    }
                """)
                .execute()
                .errors()
                .expect(error -> error.getMessage().contains("XYZ"))
                .verify();
    }

    @Test
    void stockSummaries_returnsProjection() {
        // ARRANGE: Mock the projection interface
        StockSummary summary = new StockSummary() {
            public String getSymbol() { return "AAPL"; }
            public String getName() { return "Apple Inc."; }
            public BigDecimal getCurrentPrice() { return new BigDecimal("189.30"); }
        };
        given(stockRepository.findAllProjectedBy()).willReturn(List.of(summary));

        // ACT & ASSERT
        graphQlTester.document("""
                    query {
                        stockSummaries {
                            symbol
                            name
                            currentPrice
                        }
                    }
                """)
                .execute()
                .errors().verify()
                .path("stockSummaries[0].symbol").entity(String.class).isEqualTo("AAPL");
    }
}
