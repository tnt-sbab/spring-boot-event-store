# Spring Boot Event Store

Use Event Sourcing in normal Spring Boot applications without learning a heavy framework abstraction.

This project focuses on a simple programming model:

- Kotlin/Java domain code
- Avro event schemas
- A few framework interfaces
- Production-ready plumbing handled for you

The result is a practical path to Event Sourcing and CQRS that keeps complexity in check!

## Why this framework exists

Many teams like Event Sourcing in theory, but avoid it because of framework complexity and migration risk.

`spring-boot-event-store` is designed to remove that barrier by handling difficult parts out of the box:

- Event versioning
- Automatic upcasting
- Projections and projection rebuilding
- Eventual consistency support via `revision`
- Idempotency and optimistic concurrency behavior
- Schema evolution with Avro + Schema Registry
- Operational visibility through event history APIs and JSON event reads

## The core promise

**Event Sourcing without a framework tax.**

You keep your normal Spring Boot and domain programming model. The framework handles event-store mechanics,
replay/projection flow, concurrency retries, and event publication.

**Design principle: the framework stays in the background while your domain model stays front and center.**

## Framework-neutral domain model (POJO first)

This framework is fully POJO based.

- Your domain classes do not need to extend framework base classes
- Your domain classes do not need framework annotations to work
- Your domain model can run and be tested without runtime coupling to framework internals

Think of it as the same shift many teams made from older container-heavy models (for example classic JEE entity bean
style) to lighter models where your own classes stay in control.

This design keeps the learning curve lower, reduces framework lock-in risk, and makes migration/refactoring easier over
time.

If you are coming from heavier Event Sourcing stacks, the goal here is intentionally different: keep your domain model
plain, and let the framework stay mostly invisible.

## Why this is simpler

Many event-sourcing frameworks ask you to learn a lot of framework-specific concepts before you can write a single
domain rule.

This project takes a lighter approach:

- Your domain model stays plain, testable, and easy to understand
- You work with a small set of interfaces instead of a large framework vocabulary
- Built-in event versioning and automatic upcasting reduce the need for custom migration code
- The learning curve stays lower because the framework supports your model instead of reshaping it

It also feels like a natural part of Spring Boot: you add a few well-defined pieces, and the framework stays in the
background instead of becoming a new application model you must learn from scratch.

That makes it easier to get started, easier to test, and easier to keep the domain code focused on business behavior.

## Why developers adopt this quickly

- **Fast first success**: run with the `mock` profile and start testing commands in minutes
- **Minimal surface area**: `DomainState` + command handlers + Avro events
- **Production-oriented defaults**: Liquibase schema, transactional command flow, retries
- **Built-in evolution story**: old events are transparently read as current schema versions
- **Optional Kafka**: use it when you need it, skip it when you do not

## 5-minute quick start

### Prerequisites

- Java 21
- Maven 3.8+

### 1) Build from repo root

```bash
mvn clean install
```

### 2) Run the command demo service

```bash
java -jar account-command-service/target/account-command-service-1.0-SNAPSHOT.jar --spring.profiles.active=mock
```

The `mock` profile uses:

- In-memory H2 database
- Mock Schema Registry (`mock://localhost:8081`)
- `publish-events: false`

No local Kafka or Schema Registry setup is required for this path.

### 3) Try the API in Swagger

- `http://localhost:8080/account-command-service/swagger-ui.html`

Try this flow:

1. `POST /accounts`
2. `PUT /accounts/{accountId}/deposit` with `{"amount": 100}`
3. `PUT /accounts/{accountId}/withdraw` with `{"amount": 40}`
4. `GET /events` to inspect persisted events in JSON format

Optional H2 console:

- `http://localhost:8080/account-command-service/h2-console`
- Username: `sa`
- Password: `password`

## What it looks like in code

The application model remains simple. A domain aggregate implements `DomainState` and reacts to events.

```kotlin
data class Account(val balance: Int = 0) : DomainState {
  constructor(event: AccountOpenedEvent) : this()

  override fun onEvent(event: Event): Account =
    when (event) {
      is MoneyDepositedEvent -> copy(balance = balance + event.amount)
      is MoneyWithdrawnEvent -> copy(balance = balance - event.amount)
      else -> throw IllegalArgumentException("Unsupported event type")
    }
}
```

Command execution stays explicit and close to standard Spring code:

```kotlin
commandService.apply(command.accountId) { account ->
  DepositMoneyCommandHandler(account).handle(command)
}
```

## What the framework handles for you

For each command:

1. Load current aggregate state by replaying events
2. Execute command handler against current state
3. Persist emitted event(s) atomically
4. Publish persisted event payload and `EventEntity` through Spring events

This gives predictable command flow while keeping domain logic in your own code.

## Automatic upcasting (key differentiator)

As event schemas evolve, old events remain usable.

With Avro + Schema Registry and `BACKWARD_TRANSITIVE` compatibility:

- Incompatible schema changes are rejected at registration time
- Old events can still be deserialized by newer services
- The application works with the latest event schema without hand-written migration code for every change

This is central to long-term maintainability in event-sourced systems.

## Avro events feel like normal generated domain records

Another practical benefit is that the Avro-based domain events can be reused across services with no extra code.

They also support:

- Custom logical types for value objects such as IDs and other domain-specific types
- Kotlin null safety through Avro's `createNullSafeAnnotations`
  [configuration](https://issues.apache.org/jira/browse/AVRO-3641) support

That means the generated events can feel like a natural equivalent of Java records or Kotlin data classes, while still
having the extra capabilities needed for serialization, deserialization, and on-the-fly upcasting.

## Minimal integration in your own service

### 1) Add dependency

```xml
<dependency>
  <groupId>se.sbab.tnt</groupId>
  <artifactId>spring-boot-event-store</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```

### 2) Include Liquibase changelog

```yaml
databaseChangeLog:
  - include:
      file: classpath:/se/sbab/credit/eventsourcing/db/liquibase/eventsourcing-changelog.yaml
```

### 3) Add Schema Registry settings

```yaml
spring:
  kafka:
    properties:
      schema:
        registry:
          url: http://localhost:8081
      auto.register.schemas: true
events-payload-topic: account-events
```

Recommended for domain events:

- Subject naming strategy: `TopicRecordNameStrategy`
- Compatibility mode: `BACKWARD_TRANSITIVE`

## Where BPMN fits

This framework pairs well with BPMN orchestration engines (for example CIB seven or Camunda-based setups) when your
domain includes long-running workflows.

A common split is:

- Event Sourcing: facts and state history
- Kafka: event transport/integration
- BPMN: process orchestration
- Projections: query models

This separation keeps each concern clear while still enabling end-to-end process visibility.

## Demo modules in this repository

- [`spring-boot-event-store`](spring-boot-event-store): reusable library artifact
- [`account-command-service`](account-command-service): write side (REST + event store)
- [`account-query-service`](account-query-service): read side projection (GraphQL)
- [`account-event-stream-processor`](account-event-stream-processor): optional Kafka Streams processor
- [`account-bff`](account-bff): eventual consistency gateway using `revision`

## When to use this framework

Great fit when you need:

- Strong audit/history requirements
- Evolving integration contracts over time
- Explicit handling of eventual consistency
- The ability to rebuild read models safely

Less compelling when your use case is simple CRUD with limited domain behavior.

## Local Kafka (optional)

For local Kafka clusters, use Confluent `cp-all-in-one`:

- <https://github.com/confluentinc/cp-all-in-one/blob/v8.3.1/cp-all-in-one/docker-compose.yml>

To inspect topics visually, you can use Kafbat UI:

- <https://github.com/kafbat/kafka-ui>

## Learn more

- Event Sourcing fundamentals: [`event-sourcing-101.md`](event-sourcing-101.md)
- Oracle Kafka Connect example: [`account-events-oracle-connector.json`](account-command-service/account-events-oracle-connector.json)
- License: [Apache 2.0](LICENSE.txt)

---

Tested with Kotlin and Java.
