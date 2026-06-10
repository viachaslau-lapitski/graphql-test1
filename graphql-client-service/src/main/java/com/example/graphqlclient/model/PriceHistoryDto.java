package com.example.graphqlclient.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * DTO for a PriceHistory entry returned by market-data-service's GraphQL API.
 * Uses OffsetDateTime to match the server's OffsetDateTime scalar serialisation
 * (timestamps include timezone offset, e.g. "2025-06-10T08:43:02.558-04:00").
 */
public record PriceHistoryDto(
        BigDecimal price,
        OffsetDateTime timestamp,
        double changePercent
) {
    public static PriceHistoryDto fromMap(Map<String, Object> m) {
        Object ts = m.get("timestamp");
        return new PriceHistoryDto(
                toBigDecimal(m.get("price")),
                ts == null ? null : OffsetDateTime.parse(ts.toString()),
                ((Number) m.get("changePercent")).doubleValue()
        );
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal bd) return bd;
        return new BigDecimal(value.toString());
    }
}


