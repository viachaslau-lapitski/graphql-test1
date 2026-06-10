package com.example.graphqlclient.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.web.client.RestClient;

/**
 * Creates an HttpSyncGraphQlClient bean for the /demo1 endpoints.
 *
 * HttpSyncGraphQlClient (Spring for GraphQL 1.3+) uses RestClient under the hood
 * with a fully blocking execution chain. The key is using retrieveSync() on the
 * request spec — this returns RetrieveSyncSpec whose toEntity()/toEntityList()
 * methods return values directly (no Mono, no reactive types).
 */
@Configuration
public class HttpSyncGraphQlClientConfig {

    @Bean
    public HttpSyncGraphQlClient marketDataSyncGraphQlClient(
            @Value("${market-data.graphql-url}") String graphqlUrl) {

        RestClient restClient = RestClient.create(graphqlUrl);
        return HttpSyncGraphQlClient.create(restClient);
    }
}
