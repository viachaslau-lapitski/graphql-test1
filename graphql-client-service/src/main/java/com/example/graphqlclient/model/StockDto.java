package com.example.graphqlclient.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * DTO for a Stock returned by market-data-service's GraphQL API.
 * Fields match GraphQL response field names exactly.
 * The fromMap factory deserialises from the raw Jackson Map<String, Object>.
 */
public record StockDto(
        String symbol,
        String name,
        String exchange,
        BigDecimal currentPrice,
        String currency,
        BigDecimal marketCap,
        List<PriceHistoryDto> priceHistory
) {
    @SuppressWarnings("unchecked")
    public static StockDto fromMap(Map<String, Object> m) {
        Object cap = m.get("marketCap");
        Object history = m.get("priceHistory");
        return new StockDto(
                (String) m.get("symbol"),
                (String) m.get("name"),
                (String) m.get("exchange"),
                toBigDecimal(m.get("currentPrice")),
                (String) m.get("currency"),
                cap == null ? null : toBigDecimal(cap),
                history == null ? List.of()
                        : ((List<?>) history).stream()
                                .map(e -> PriceHistoryDto.fromMap((Map<String, Object>) e))
                                .toList()
        );
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal bd) return bd;
        return new BigDecimal(value.toString());
    }
}

