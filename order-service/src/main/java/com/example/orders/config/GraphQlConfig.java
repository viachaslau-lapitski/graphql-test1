package com.example.orders.config;

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
 * GraphQlConfig for order-service.
 * Same pattern as market-data-service — registers scalars, federation, and a logging interceptor.
 * See market-data-service/GraphQlConfig.java for detailed comments on each bean.
 */
@Configuration
public class GraphQlConfig {

    private static final Logger log = LoggerFactory.getLogger(GraphQlConfig.class);

    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return builder -> builder
                .scalar(ExtendedScalars.GraphQLBigDecimal)
                .scalar(ExtendedScalars.DateTime);
    }

    /** Enables Apollo Federation schema transformation. See market-data-service/GraphQlConfig for full explanation. */
    @Bean
    public FederationSchemaFactory federationSchemaFactory() {
        return new FederationSchemaFactory();
    }

    @Bean
    public GraphQlSourceBuilderCustomizer federationSupport(FederationSchemaFactory factory) {
        return builder -> builder.schemaFactory(factory::createGraphQLSchema);
    }

    @Bean
    public WebGraphQlInterceptor loggingInterceptor() {
        return new WebGraphQlInterceptor() {
            @Override
            public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
                long start = System.currentTimeMillis();
                String op = request.getOperationName() != null ? request.getOperationName() : "anonymous";

                log.info("[GraphQL] → operation='{}' variables={}", op, request.getVariables());

                return chain.next(request).doOnNext(response -> {
                    long elapsed = System.currentTimeMillis() - start;
                    if (response.getErrors().isEmpty()) {
                        log.info("[GraphQL] ← operation='{}' {}ms ✓", op, elapsed);
                    } else {
                        log.warn("[GraphQL] ← operation='{}' {}ms ✗ errors={}",
                                op, elapsed, response.getErrors().size());
                    }
                });
            }
        };
    }
}
