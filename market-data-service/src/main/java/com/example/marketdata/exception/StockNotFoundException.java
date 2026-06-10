package com.example.marketdata.exception;

import graphql.ErrorType;
import graphql.GraphQLError;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.stereotype.Component;

/**
 * StockNotFoundException — represents a "not found" business error.
 *
 * We extend RuntimeException so it doesn't need to be declared in method signatures.
 */
public class StockNotFoundException extends RuntimeException {
    private final String symbol;

    public StockNotFoundException(String symbol) {
        super("Stock not found: " + symbol);
        this.symbol = symbol;
    }

    public String getSymbol() { return symbol; }

    // ──────────────────────────────────────────────────────────────────────────

    /**
     * CustomExceptionResolver — translates Java exceptions into GraphQL errors.
     *
     * GraphQL ERROR HANDLING differs from REST:
     *
     * In REST you return HTTP 404 for "not found". In GraphQL:
     *  - HTTP is always 200 (the transport succeeded)
     *  - Errors appear in the "errors" array of the JSON response alongside "data"
     *  - This allows PARTIAL results: some fields succeed, some fail
     *
     * Example GraphQL error response:
     * {
     *   "data": { "stock": null },
     *   "errors": [{ "message": "Stock not found: XYZ", "extensions": { "errorType": "NOT_FOUND" } }]
     * }
     *
     * The DataFetcherExceptionResolverAdapter intercepts exceptions thrown by
     * any @QueryMapping or @SchemaMapping method and converts them to GraphQL errors.
     */
    @Component
    public static class CustomExceptionResolver extends DataFetcherExceptionResolverAdapter {

        @Override
        protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
            if (ex instanceof StockNotFoundException notFound) {
                // Build a structured GraphQL error with the NOT_FOUND error type
                return GraphQLError.newError()
                        .message(notFound.getMessage())
                        .errorType(ErrorType.DataFetchingException)
                        .extensions(java.util.Map.of(
                                "errorType", "NOT_FOUND",
                                "symbol", notFound.getSymbol()
                        ))
                        .build();
            }
            // Return null to let the default resolver handle other exception types
            return null;
        }
    }
}
