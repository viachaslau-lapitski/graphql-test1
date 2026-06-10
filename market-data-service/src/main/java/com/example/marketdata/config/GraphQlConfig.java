package com.example.marketdata.config;

import graphql.scalars.ExtendedScalars;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.graphql.GraphQlSourceBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.data.federation.FederationSchemaFactory;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import reactor.core.publisher.Mono;

/**
 * GraphQlConfig — central configuration for the GraphQL engine.
 *
 * This is where we:
 *   1. Register custom scalars (BigDecimal, DateTime).
 *   2. Enable Apollo Federation via FederationSchemaFactory.
 *   3. Add a WebGraphQlInterceptor for request logging.
 */
@Configuration
public class GraphQlConfig {

    private static final Logger log = LoggerFactory.getLogger(GraphQlConfig.class);

    /**
     * RuntimeWiringConfigurer — registers custom scalar implementations.
     *
     * GraphQL's type system is extensible via custom scalars. The schema declares:
     *   scalar BigDecimal
     *   scalar DateTime
     *
     * Without registering an implementation here, GraphQL-Java won't know how
     * to serialize these types. The graphql-java-extended-scalars library provides
     * production-ready implementations.
     *
     * BigDecimal: serialized as a JSON number with full precision (no float errors)
     * DateTime:   serialized as ISO-8601 string "2025-06-09T14:30:00"
     */
    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return builder -> builder
                .scalar(ExtendedScalars.GraphQLBigDecimal)   // BigDecimal ↔ JSON number
                .scalar(ExtendedScalars.DateTime);            // LocalDateTime ↔ ISO-8601 string
    }

    /**
     * FederationSchemaFactory — enables Apollo Federation v2 subgraph support.
     *
     * When this bean is present, Spring for GraphQL uses it to:
     *   1. Transform the schema with federation directives (@key, @link, etc.)
     *   2. Add the _service { sdl } meta-field (used by Apollo Router for schema introspection)
     *   3. Add the _entities query (used by Apollo Router for federated entity resolution)
     *   4. Scan for @EntityMapping methods and register them as entity resolvers
     *
     * This is why our schema can use `extend schema @link(...)` and `type Stock @key(...)`.
     */
    @Bean
    public FederationSchemaFactory federationSchemaFactory() {
        return new FederationSchemaFactory();
    }

    /**
     * GraphQlSourceBuilderCustomizer — hooks FederationSchemaFactory into the schema builder.
     *
     * GraphQlAutoConfiguration accepts any number of GraphQlSourceBuilderCustomizer beans
     * and applies them when building the GraphQlSource. Here we replace the default
     * SchemaGenerator with FederationSchemaFactory.createGraphQLSchema, which applies
     * the federation schema transformation.
     */
    @Bean
    public GraphQlSourceBuilderCustomizer federationSupport(FederationSchemaFactory factory) {
        return builder -> builder.schemaFactory(factory::createGraphQLSchema);
    }

    /**
     * WebGraphQlInterceptor — intercepts every GraphQL request/response.
     *
     * This is similar to a Servlet Filter or Spring MVC Interceptor, but
     * operates at the GraphQL layer — above HTTP. It receives the parsed
     * GraphQL document and can inspect/modify it before and after execution.
     *
     * Common uses:
     *   - Logging (what operation was executed, timing)
     *   - Authentication / extracting JWT claims into the GraphQL context
     *   - Rate limiting
     *   - Adding response headers
     *
     * Note: This is reactive (returns Mono) even in WebMVC mode.
     * Spring for GraphQL uses Project Reactor internally for interceptor chains.
     */
    @Bean
    public WebGraphQlInterceptor loggingInterceptor() {
        return new WebGraphQlInterceptor() {
            @Override
            public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request,
                                                      Chain chain) {
                long startTime = System.currentTimeMillis();
                String operationName = request.getOperationName() != null
                        ? request.getOperationName() : "anonymous";

                log.info("[GraphQL] → operation='{}' variables={}",
                        operationName, request.getVariables());

                return chain.next(request)
                        .doOnNext(response -> {
                            long elapsed = System.currentTimeMillis() - startTime;
                            if (response.getErrors().isEmpty()) {
                                log.info("[GraphQL] ← operation='{}' {}ms ✓", operationName, elapsed);
                            } else {
                                log.warn("[GraphQL] ← operation='{}' {}ms ✗ errors={}",
                                        operationName, elapsed, response.getErrors().size());
                            }
                        });
            }
        };
    }
}
