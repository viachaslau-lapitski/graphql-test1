package com.example.graphqlclient.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configures a RestClient pre-pointed at the market-data-service GraphQL endpoint.
 *
 * Spring Boot 3.2+ ships RestClient as the modern synchronous HTTP client.
 * It handles request/response serialisation via Jackson (already on the classpath)
 * and is the cleanest way to send GraphQL queries without pulling in WebFlux.
 */
@Configuration
public class GraphQlClientConfig {

    @Bean
    public RestClient marketDataRestClient(
            @Value("${market-data.graphql-url}") String graphqlUrl) {

        return RestClient.builder()
                .baseUrl(graphqlUrl)
                .build();
    }
}

