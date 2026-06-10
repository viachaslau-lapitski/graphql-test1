# GraphQL + Spring Boot Showcase

A learning/demo project demonstrating **GraphQL with Spring Boot 3.5** for developers
new to GraphQL. Uses a **financial trading domain** (stocks & orders) to illustrate
real-world use cases.

## What You'll Learn

| Concept | Where |
|---------|-------|
| Basic queries & field selection | Scenarios 1–2 |
| Avoiding over-fetching | Scenario 2 |
| Avoiding under-fetching (nested queries) | Scenario 3 |
| Variables & fragments | Scenario 4 |
| Aliases | Scenario 5 |
| N+1 problem & DataLoader fix (`@BatchMapping`) | Scenario 6 |
| Real-time Subscriptions (SSE) | Scenario 7 |
| Apollo Federation (cross-service joins) | Scenario 8 |
| Observability (Micrometer + Actuator + Prometheus) | Scenario 9 |
| **GraphQL client (service-to-service via RestClient)** | **Scenario 10** |
| **GraphQL client using `HttpSyncGraphQlClient` (Spring for GraphQL)** | **Scenario 11** |
| Spring Data JPA as a DataFetcher backend | Built into both services |
| Spring Data interface projections | `stockSummaries` query |
| Custom scalars (BigDecimal, DateTime) | All queries with prices |
| GraphQL error handling | `stock(symbol: "XYZ")` |
| WebGraphQlInterceptor (request logging) | Built into both services |

---

## Architecture

```
Browser / curl / GraphiQL
         │
         ▼
   Apollo Router :4000          ← unified supergraph gateway
   /graphql
    ┌─────┴──────────────────┐
    ▼                        ▼
market-data-service      order-service
     :8081                    :8082
  (stocks, prices,        (orders, portfolio,
   subscriptions)          DataLoader, federation)
    └─────────────────────────┘
         both use H2 in-memory DB

graphql-client-service :8083   ← GraphQL CLIENT demo (Scenario 10)
   REST API → RestClient → POST /graphql → market-data-service :8081
```

**Apollo Federation** allows the two independent services to form one unified
GraphQL API. Clients query `http://localhost:4000` and get data from both services
in a single response — the router handles the coordination transparently.

---

## Project Structure

```
graphql-showcase/
├── market-data-service/         Spring Boot subgraph #1 (port 8081)
│   ├── src/main/java/com/example/marketdata/
│   │   ├── model/               JPA entities: Stock, PriceHistory
│   │   ├── repository/          Spring Data JPA repositories
│   │   ├── controller/          GraphQL resolvers: @QueryMapping, @SubscriptionMapping
│   │   ├── projection/          StockSummary interface (Spring Data projection)
│   │   ├── config/              GraphQlConfig (scalars, interceptor), DataInitializer
│   │   └── exception/           StockNotFoundException + GraphQL error resolver
│   └── src/main/resources/
│       ├── graphql/schema.graphqls
│       └── application.yml
│
├── order-service/               Spring Boot subgraph #2 (port 8082)
│   ├── src/main/java/com/example/orders/
│   │   ├── model/               JPA entities: Order; enums: OrderType, OrderStatus
│   │   │                        Stock.java — federation stub (only @key field)
│   │   ├── repository/          Spring Data JPA repositories
│   │   ├── controller/
│   │   │   ├── OrderController.java         @QueryMapping
│   │   │   ├── StockBatchController.java    @BatchMapping (N+1 demo)
│   │   │   └── StockEntityController.java   @EntityMapping (federation)
│   │   └── config/              GraphQlConfig, DataInitializer
│   └── src/main/resources/
│       ├── graphql/schema.graphqls
│       └── application.yml
│
├── graphql-client-service/      Spring Boot GraphQL CLIENT demo (port 8083)
│   ├── src/main/java/com/example/graphqlclient/
│   │   ├── client/              MarketDataClient — RestClient + GraphQL query logic
│   │   ├── config/              GraphQlClientConfig — RestClient bean
│   │   ├── controller/          DemoController — REST endpoints → GraphQL queries
│   │   └── model/               StockDto, PriceHistoryDto — response POJOs
│   └── src/main/resources/
│       └── application.yml
│
├── docker-compose.yml           Both services + Apollo Router + optional monitoring
├── router.yaml                  Apollo Router configuration
├── supergraph.graphql           Pre-composed federation schema
└── monitoring/                  Prometheus + Grafana for optional observability stack
```

---

## Quick Start

### Option A: Run services individually (simplest)

```bash
# Build and run market-data-service
./gradlew :market-data-service:bootRun

# In another terminal, run order-service
./gradlew :order-service:bootRun
```

Open GraphiQL:
- market-data-service: http://localhost:8081/graphiql
- order-service: http://localhost:8082/graphiql

### Option B: Full federation stack with Docker Compose

```bash
# Step 1: Build JAR files
./gradlew :market-data-service:bootJar :order-service:bootJar

# Step 2: Start everything (Apollo Router + both services)
docker compose up

# Optional: with monitoring (Prometheus + Grafana)
docker compose --profile monitoring up
```

Open:
- **Apollo Router** (unified): http://localhost:4000
- market-data-service standalone: http://localhost:8081/graphiql
- order-service standalone: http://localhost:8082/graphiql
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (admin / demo)

### Run Tests

```bash
./gradlew test
```

---

## Demo Scenarios

> All queries below can be run in the GraphiQL browser IDE.
> GraphiQL has autocomplete, schema explorer, and history — use it!

---

> **📌 NB — Scenario 10 (GraphQL Client) is a key scenario worth running first**
> if you want to see how a real-world service *consumes* a GraphQL API programmatically
> rather than just exposing one. It requires only `market-data-service` and the new
> `graphql-client-service` — no GraphiQL needed, just `curl`.
> [Jump to Scenario 10 ↓](#scenario-10--graphql-client-service-to-service-via-restclient)

---

### Scenario 1 — Hello GraphQL: Your First Query

**Where:** http://localhost:8081/graphiql

```graphql
# The simplest possible query — fetch all stocks with all fields
{
  stocks {
    symbol
    name
    exchange
    currentPrice
    currency
  }
}
```

**What you see:** A JSON response with exactly the fields you requested.
Notice: there's no `marketCap` in the response — because you didn't ask for it.
That's GraphQL's fundamental contract: **you get exactly what you ask for**.

Compare with REST: `GET /api/stocks` would return a fixed JSON structure
with ALL fields regardless of whether you need them.

---

### Scenario 2 — Avoiding Over-Fetching: Ask Only What You Need

**Where:** http://localhost:8081/graphiql

The "over-fetching" problem: REST APIs return a fixed response shape.
If you only need prices for a stock ticker widget, you still get all fields.

```graphql
# REST equivalent: GET /api/stocks (returns everything)
# GraphQL: request ONLY symbol and price
{
  stocks {
    symbol
    currentPrice
  }
}
```

```graphql
# Filter by exchange — REST would need: GET /api/stocks?exchange=NYSE
{
  stocks(exchange: "NYSE") {
    symbol
    name
    currentPrice
  }
}
```

```graphql
# The "NASDAQ tech stocks" widget only needs these 3 fields:
{
  stocks(exchange: "NASDAQ") {
    symbol
    currentPrice
    currency
  }
}
```

**What to notice:** Different clients can request different fields from the same
endpoint. No API versioning, no separate endpoints for mobile vs desktop.

---

### Scenario 3 — Avoiding Under-Fetching: One Query for Nested Data

**Where:** http://localhost:8081/graphiql

The "under-fetching" problem: REST often requires multiple requests.
To show a stock with its price history, you'd need:
1. `GET /api/stocks/AAPL`
2. `GET /api/stocks/AAPL/price-history?limit=5`

With GraphQL — **one request**:

```graphql
{
  stock(symbol: "AAPL") {
    name
    currentPrice
    currency
    priceHistory(limit: 5) {
      price
      timestamp
      changePercent
    }
  }
}
```

Try changing `limit` — the server fetches only N records from the database.

```graphql
# Stock details for a dashboard — NASDAQ stocks with last 3 price points
{
  stocks(exchange: "NASDAQ") {
    symbol
    name
    currentPrice
    priceHistory(limit: 3) {
      price
      changePercent
    }
  }
}
```

**GraphQL resolves nested fields lazily:** `priceHistory` is only fetched when
a client asks for it. If you omit it, no DB query for price history is made.

---

### Scenario 4 — Variables and Fragments: Reusable Query Components

**Where:** http://localhost:8081/graphiql

Hard-coding values in queries is messy. **Variables** parameterize queries:

```graphql
# Define the query with a variable
query GetStock($symbol: String!) {
  stock(symbol: $symbol) {
    name
    exchange
    currentPrice
    currency
  }
}
```

In the GraphiQL "Variables" panel (bottom left), add:
```json
{ "symbol": "MSFT" }
```

**Fragments** let you define reusable field selections:

```graphql
# Define a fragment once, use it in multiple queries
fragment StockDetails on Stock {
  symbol
  name
  currentPrice
  currency
}

query {
  aaplStock: stock(symbol: "AAPL") {
    ...StockDetails
    marketCap        # extend the fragment with extra fields
  }
  jpmorgan: stock(symbol: "JPM") {
    ...StockDetails  # reuse the same fragment
  }
}
```

---

### Scenario 5 — Aliases: Multiple Queries in One Request

**Where:** http://localhost:8081/graphiql

Need data from two different exchanges? With REST you'd make two requests.
With GraphQL **aliases**, one request fetches both:

```graphql
# Fetch NYSE and NASDAQ stocks in a SINGLE request
{
  nyseStocks: stocks(exchange: "NYSE") {
    symbol
    currentPrice
  }
  nasdaqStocks: stocks(exchange: "NASDAQ") {
    symbol
    currentPrice
  }
}
```

```graphql
# Compare prices of two specific stocks side-by-side
{
  apple: stock(symbol: "AAPL") {
    name
    currentPrice
  }
  microsoft: stock(symbol: "MSFT") {
    name
    currentPrice
  }
}
```

**Key point:** Aliases let you call the same field multiple times with different
arguments in a single request. All resolved in parallel.

---

### Scenario 6 — N+1 Problem and the DataLoader Solution

**Where:** http://localhost:8082/graphiql

The N+1 problem is one of GraphQL's most common performance pitfalls.

**The problem:** When you query a list and include a nested field, GraphQL
might resolve that nested field N times — once per parent object.

```graphql
# Query 4 open orders, each with a stock field
{
  orders(status: OPEN) {
    id
    symbol
    stock {
      symbol
    }
  }
}
```

**Without DataLoader (naive):**
- 1 query: `SELECT * FROM stock_order WHERE status = 'OPEN'` → 4 orders
- 4 calls: `resolveStock("AAPL")`, `resolveStock("MSFT")`, ... (one per order)
= **5 total calls** — the "N+1 problem"

**With `@BatchMapping` (DataLoader pattern):**
- 1 query: fetch all open orders
- 1 batch call: `resolveStocks(["AAPL", "MSFT", "TSLA", "GS"])` — all at once
= **2 total calls** — the DataLoader solution

**Watch the logs while running this query:**
```
🔄 @BatchMapping: resolving stocks for 4 orders — single batch call! (symbols: [AAPL, MSFT, TSLA, GS])
```

See `StockBatchController.java` for the implementation. The key is:
- `@SchemaMapping` (naive): `Stock stock(Order order)` — called N times
- `@BatchMapping` (DataLoader): `Map<Order, Stock> stock(List<Order> orders)` — called once

Spring for GraphQL uses `graphql-java`'s DataLoader internally.
In the federation setup, Apollo Router also batches entity resolution automatically.

---

### Scenario 7 — Subscriptions: Real-Time Price Updates

**Where:** http://localhost:8081/graphiql

GraphQL has three operation types:
- `query` — fetch data once
- `mutation` — change data (not in this demo)
- `subscription` — **stream data continuously**

```graphql
# Subscribe to live Apple price ticks (one per second)
subscription {
  priceUpdated(symbol: "AAPL") {
    price
    changePercent
    timestamp
  }
}
```

**What you see:** Price updates arrive in real-time, one per second.
The connection stays open — the server pushes data to you.

**Transport:** Spring for GraphQL uses **Server-Sent Events (SSE)** over HTTP.
SSE is a one-way push channel (server → client). Simpler than WebSockets
for this use case since we don't need bidirectional communication.

**In production:** Replace `Flux.interval()` with a real event source:
- Kafka topic subscription
- Redis Pub/Sub
- Database change streams

See `PriceSubscriptionController.java` for the implementation using Reactor `Flux`.

---

### Scenario 8 — Apollo Federation: Transparent Cross-Service Joins

**Where:** http://localhost:4000 (Apollo Router — requires `docker compose up`)

Federation allows querying data from multiple services as if it were one schema.

```graphql
# Query through Apollo Router — data comes from BOTH services
{
  orders(status: OPEN) {
    id
    symbol
    quantity
    status
    stock {
      # These fields come from market-data-service! ──────────────────────┐
      name         #                                                       │
      currentPrice # Apollo Router fetches these transparently             │
      exchange     # by calling market-data-service's _entities query      │
    }              # ──────────────────────────────────────────────────────┘
  }
}
```

**What happens behind the scenes:**
1. Router receives the query at `:4000`
2. Router's query plan:
   - Fetch `{ orders { id symbol quantity status } }` from **order-service**
   - Fetch `{ _entities(representations: [{ __typename: "Stock", symbol: "..." }]) }` from **market-data-service**
3. Router merges the results and returns ONE response to the client

The client doesn't know or care that the data comes from two different services.

**Key schema pieces:**
- In `market-data-service/schema.graphqls`: `type Stock @key(fields: "symbol")` — owns the entity
- In `order-service/schema.graphqls`: `type Stock @key(fields: "symbol")` — references the entity
- `StockEntityController.java` in order-service: handles `@EntityMapping` for Stock

---

### Scenario 9 — Observability: GraphQL Metrics in Action

**Where:** http://localhost:8081/actuator/metrics

Spring for GraphQL 1.4 + Micrometer automatically records metrics for every
GraphQL operation. No code required — just having Actuator on the classpath is enough.

**Step 1:** Run a few queries (Scenarios 2–3 above) at http://localhost:8081/graphiql

**Step 2:** Check the metrics:

```bash
# See all available GraphQL metrics
curl http://localhost:8081/actuator/metrics | jq '.names[] | select(startswith("graphql"))'

# Timing for all GraphQL requests
curl http://localhost:8081/actuator/metrics/graphql.request | jq .

# Per-field resolver timings (which fields are slowest?)
curl http://localhost:8081/actuator/metrics/graphql.datafetcher | jq .
```

**Step 3:** Prometheus format (for scraping):
```
http://localhost:8081/actuator/prometheus
```
Look for `graphql_request_seconds_*` metrics.

**Key metrics to watch in production:**
| Metric | Why it matters |
|--------|---------------|
| `graphql.request` count/time | Overall API throughput and latency |
| `graphql.datafetcher` by field | Which resolvers are slowest (optimization targets) |
| `graphql.error` | Error rate by operation |

**Optional — Start the monitoring stack:**
```bash
docker compose --profile monitoring up
```
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (admin / demo)

---

### Scenario 10 — GraphQL Client: Service-to-Service via RestClient

**Where:** `graphql-client-service` (port **8083**) calls `market-data-service` (port **8081**)

This scenario flips the perspective: instead of *exploring* a GraphQL API in GraphiQL,
you see how a **Spring Boot service acts as a GraphQL client** — sending queries
programmatically from Java code and mapping responses to typed DTOs.

**Stack:** Spring's `RestClient` (synchronous, no WebFlux) posts the standard GraphQL
JSON envelope `{ "query": "...", "variables": { ... } }` and deserialises the response.

**Step 1:** Start market-data-service (if not already running):
```bash
./gradlew :market-data-service:bootRun
```

**Step 2:** In another terminal, start the client service:
```bash
./gradlew :graphql-client-service:bootRun
```

**Step 3:** Trigger the demo scenarios via REST:

```bash
# All stocks — sends: query { stocks { symbol name exchange currentPrice currency } }
curl http://localhost:8083/demo/stocks | jq .

# Filter by exchange — sends the same query with variable exchange="NASDAQ"
curl "http://localhost:8083/demo/stocks?exchange=NASDAQ" | jq .

# Single stock with price history — nested GraphQL query in one round-trip
# query { stock(symbol: $symbol) { ... priceHistory(limit: 5) { ... } } }
curl http://localhost:8083/demo/stock/AAPL | jq .

# Error handling — GraphQL "Stock not found" error maps to HTTP 404
curl -i http://localhost:8083/demo/stock/XYZ
```

**What to look for:**
- The `graphql-client-service` log shows the outgoing `POST /graphql` request
- The `market-data-service` log shows the incoming GraphQL operation being resolved
- `/demo/stock/AAPL` returns nested `priceHistory` in a **single** HTTP round-trip (no under-fetching)
- `/demo/stock/XYZ` returns `{ "error": "Stock not found: XYZ" }` with HTTP 404 — GraphQL errors are translated to proper REST semantics

**Key implementation files:**
| File | What it shows |
|------|--------------|
| `client/MarketDataClient.java` | GraphQL query strings, variable passing, `{ data, errors }` parsing |
| `config/GraphQlClientConfig.java` | `RestClient` bean wired to the target GraphQL URL |
| `controller/DemoController.java` | REST → GraphQL orchestration, error translation |
| `application.yml` | `market-data.graphql-url` property (override for different environments) |

---

### Scenario 11 — GraphQL Client: `HttpSyncGraphQlClient` (Spring for GraphQL Native)

**Where:** `graphql-client-service` (port **8083**) — routes `/demo1/*`

This is the recommended way to consume a GraphQL API from a Spring Boot service.
`HttpSyncGraphQlClient` is the **official Spring for GraphQL HTTP client** (added in 1.3).
It is backed by `RestClient`, requires zero WebFlux, and gives you a clean typed API
with interceptor support — no manual JSON construction or envelope parsing needed.

The key difference from raw `RestClient` (Scenario 10): use **`retrieveSync()`** (not
`retrieve()`) to get `RetrieveSyncSpec` whose `toEntity()` / `toEntityList()` return
values directly — no `Mono`, no `.block()`.

**Step 1:** Start market-data-service:
```bash
./gradlew :market-data-service:bootRun
```

**Step 2:** Start the client service:
```bash
./gradlew :graphql-client-service:bootRun
```

**Step 3:** Try the `/demo1` endpoints:

```bash
# All stocks
curl http://localhost:8083/demo1/stocks

# Filter by exchange
curl "http://localhost:8083/demo1/stocks?exchange=NASDAQ"
curl "http://localhost:8083/demo1/stocks?exchange=NYSE"

# Single stock with last 5 price history entries (nested query, one round-trip)
curl http://localhost:8083/demo1/stock/AAPL
curl http://localhost:8083/demo1/stock/MSFT

# GraphQL error → HTTP 404
curl -i http://localhost:8083/demo1/stock/XYZ
```

**What to look for:**
- `Demo1MarketDataClient` calls `.retrieveSync("stocks")` / `.retrieveSync("stock")` — fully synchronous, no reactive types
- Jackson deserialises the GraphQL `data` field directly into `StockDto` / `List<StockDto>` — no manual map parsing
- `FieldAccessException` is thrown automatically when the GraphQL response contains errors — the controller maps it to HTTP 404

**Comparison with Scenario 10 (`/demo`):**

| | Scenario 10 `/demo` | Scenario 11 `/demo1` |
|---|---|---|
| HTTP transport | `RestClient` (manual) | `RestClient` (via `HttpSyncGraphQlClient`) |
| Request building | Manual `Map.of("query", ..., "variables", ...)` | `graphQlClient.document(query).variable(...)` |
| Response parsing | Manual `Map<String, Object>` unwrapping | `.retrieveSync(path).toEntityList(Class)` |
| Error handling | Check `response.get("errors")` manually | `FieldAccessException` thrown automatically |
| Interceptors | No | Yes — add auth, logging, retry via `SyncGraphQlClientInterceptor` |

**Key implementation files:**
| File | What it shows |
|------|--------------|
| `client/Demo1MarketDataClient.java` | `retrieveSync()` → direct typed results, no Mono |
| `config/HttpSyncGraphQlClientConfig.java` | `HttpSyncGraphQlClient.create(restClient)` bean |
| `controller/Demo1Controller.java` | REST → GraphQL, `FieldAccessException` → 404 |

---

### Bonus — Spring Data Projection Demo

**Where:** http://localhost:8081/graphiql

```graphql
# stockSummaries uses a Spring Data interface projection
# Only 3 columns are fetched from the DB (not all Stock columns)
{
  stockSummaries {
    symbol
    name
    currentPrice
  }
}
```

With `spring.jpa.show-sql=true` (enabled in application.yml), check the logs:
- `stockSummaries`: `SELECT s.symbol, s.name, s.current_price FROM stock s` (3 columns)
- `stocks`:         `SELECT s.* FROM stock s` (all columns)

This demonstrates how GraphQL's field selection philosophy can be pushed all the
way down to the database query, eliminating over-fetching at every layer.

See `StockSummary.java` (projection interface) and `StockRepository.java`.

---

### Bonus — H2 Console: Browse the In-Memory Database

Both services expose the H2 console:
- market-data-service: http://localhost:8081/h2-console (JDBC URL: `jdbc:h2:mem:marketdata`)
- order-service: http://localhost:8082/h2-console (JDBC URL: `jdbc:h2:mem:orderdb`)

Useful for verifying what data was seeded and correlating DB queries with GraphQL queries.

---

## Key Technology Deep Dive

### Spring for GraphQL Annotations

| Annotation | Purpose | Example |
|------------|---------|---------|
| `@QueryMapping` | Maps method to Query type field | `stocks()` → `Query.stocks` |
| `@SchemaMapping` | Maps method to any type field | `priceHistory(Stock stock)` → `Stock.priceHistory` |
| `@BatchMapping` | Batch resolver (DataLoader) | `stock(List<Order> orders)` — called once for N orders |
| `@SubscriptionMapping` | Maps to Subscription field, returns `Flux<T>` | `priceUpdated()` → `Subscription.priceUpdated` |
| `@EntityMapping` | Federation entity resolver | `stock(@Argument String symbol)` |
| `@Argument` | Extracts a GraphQL argument | `@Argument String symbol` |

### Custom Scalars

GraphQL's default scalars (`String`, `Int`, `Float`, `Boolean`, `ID`) are not
enough for financial data. We use `graphql-java-extended-scalars`:

| Scalar | Java type | Why |
|--------|-----------|-----|
| `BigDecimal` | `java.math.BigDecimal` | Exact decimal arithmetic — **never use Float for money** |
| `DateTime` | `java.time.LocalDateTime` | ISO-8601 format: `"2025-06-09T14:30:00"` |

Registered in `GraphQlConfig.java` via `RuntimeWiringConfigurer`.

### Federation Concepts

| Concept | What it does |
|---------|-------------|
| `@key(fields: "symbol")` | Marks a type as a federated entity identified by symbol |
| `_service { sdl }` query | Returns the subgraph's schema (auto-handled by Spring) |
| `_entities(representations: [...])` query | Resolves entity stubs (auto-handled by Spring + `@EntityMapping`) |
| Apollo Router | Composes subgraph schemas and plans cross-service queries |
| `supergraph.graphql` | The composed unified schema used by the router |

### Regenerating the Supergraph Schema

The `supergraph.graphql` file was pre-composed for this demo. In real projects,
regenerate it with [Rover CLI](https://www.apollographql.com/docs/rover/):

```bash
# Install Rover
curl -sSL https://rover.apollo.dev/nix/latest | sh

# Introspect both running services and compose
rover supergraph compose --config supergraph-config.yaml > supergraph.graphql
```

---

## What's NOT in this Demo (but worth knowing)

### Native GraphQL Support in Data Drivers
As of 2025, several Spring Data modules have GraphQL-aware features:
- **Spring Data REST** can expose repositories directly as GraphQL endpoints
- **Spring Data MongoDB** has `@Query` support with GraphQL selection sets
- See [Spring Data + GraphQL docs](https://docs.spring.io/spring-data/commons/docs/current/reference/html/#graphql)

### GraphQL Client (service-to-service)
See **Scenario 10** — `graphql-client-service` demonstrates exactly this using
Spring's `RestClient` to POST GraphQL queries to `market-data-service`.
For reactive use cases, Spring for GraphQL also provides `HttpGraphQlClient`
(backed by `WebClient`) and `RSocketGraphQlClient`.

### `@defer` and `@stream` Directives
Emerging GraphQL features for **incremental delivery** — the server can send
partial results before the full query completes. Watch this space:
- `@defer` on a fragment: sends the deferred fragment as a separate chunk
- Spring for GraphQL plans to support this in upcoming versions

### Mutations
This demo focuses on queries and subscriptions. Mutations (creating/modifying data)
follow the same annotation pattern: `@MutationMapping` on a method. Spring for
GraphQL handles them identically to queries at the framework level.

### Query Complexity & Depth Limiting
In production, protect your GraphQL API from abusive queries:
```yaml
spring:
  graphql:
    schema:
      inspection:
        enabled: true  # detect missing resolvers
```
Use libraries like `graphql-java-extended-validation` for field-level validation
and `graphql-query-complexity` for query cost analysis.

### Security
- Field-level security: `@PreAuthorize` works with `@SchemaMapping` methods
- JWT: Extract claims in `WebGraphQlInterceptor` and add to `GraphQLContext`
- Rate limiting: Implement in `WebGraphQlInterceptor` or at the Apollo Router level

---

## Running Tests

```bash
# All tests
./gradlew test

# With test report
./gradlew test --tests "*" --info

# Specific service
./gradlew :market-data-service:test
./gradlew :order-service:test
```

Test reports: `build/reports/tests/test/index.html`

---

## References

- [Spring for GraphQL Reference](https://docs.spring.io/spring-graphql/docs/current/reference/html/)
- [Spring for GraphQL 1.4 Release Notes](https://github.com/spring-projects/spring-graphql/wiki/Spring-for-GraphQL-1.4)
- [Apollo Federation v2 Docs](https://www.apollographql.com/docs/federation/)
- [graphql-java-extended-scalars](https://github.com/graphql-java/graphql-java-extended-scalars)
- [Apollo Router Configuration](https://www.apollographql.com/docs/router/configuration/overview)
- [Micrometer GraphQL Observations](https://docs.spring.io/spring-graphql/docs/current/reference/html/#observability)
