package com.example.graphqlclient.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for a PriceHistory entry returned by market-data-service's GraphQL API.
 */
public record PriceHistoryDto(
        BigDecimal price,
        LocalDateTime timestamp,
        double changePercent
) {
    public static PriceHistoryDto fromMap(Map<String, Object> m) {
        Object ts = m.get("timestamp");
        return new PriceHistoryDto(
                toBigDecimal(m.get("price")),
                ts == null ? null : LocalDateTime.parse(ts.toString()),
                ((Number) m.get("changePercent")).doubleValue()
        );
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal bd) return bd;
        return new BigDecimal(value.toString());
    }
}

