# SBAB event-store library

The `spring-boot-event-store` module is a production-grade event sourcing library for Spring Boot applications.
It’s licensed under the Apache 2.0 License.

**This is the only event sourcing library with automatic event upcasting** — your application always works with the
latest event schema, no matter how old the stored data is.

Older events are transparently upcasted on read using **Avro** and a **Schema Registry** configured with
`BACKWARD_TRANSITIVE` compatibility, which also rejects breaking schema changes at registration time before they reach
production.

A Schema Registry is required to use this framework. Kafka is optional, but built-in event publishing is supported in
two ways:

1. `publish-events: true` — simple Spring property, recommended for local development.
2. Kafka Connect — reads committed events directly from the database, guaranteeing strict ordering even across multiple
   service instances. See [`account-events-oracle-connector.json`](account-command-service/account-events-oracle-connector.json)
   for an example connector configuration using an Oracle database.

This repository also contains a small set of demo services that show how to structure an event-sourced system end-to-end
(command side, query side, stream processing, and a BFF).

---

## Table of contents

- [What this library provides](#what-this-library-provides)
- [Event Sourcing fundamentals](#event-sourcing-fundamentals)
  - [Key concepts](#key-concepts)
  - [Commands, events, and aggregates](#commands-events-and-aggregates)
  - [State reconstruction (projection)](#state-reconstruction-projection)
  - [Optimistic concurrency](#optimistic-concurrency)
  - [Schema evolution and upcasting](#schema-evolution-and-upcasting)
- [Spring integration](#spring-integration)
- [Configuration](#configuration)
- [Demo application modules](#demo-application-modules)
- [BFF (eventual consistency gateway)](#bff-eventual-consistency-gateway)
- [Local Kafka](#local-kafka)
- [View events on Kafka using Kafbat UI](#view-events-on-kafka-using-kafbat-ui)

---

## What this library provides

At a high level, the library helps you build event-sourced domain models in Spring Boot by providing:

- **An event store** backed by a relational database (Liquibase changelog included).
- **A command execution API** (`CommandService.apply` / `CommandService.applyList`) that:
  - loads the current aggregate state by replaying events,
  - executes your domain logic (command handler),
  - persists the resulting event(s) atomically.
- **Concurrency handling** via a unique `(aggregate_id, revision)` constraint and automatic retries.
- **Event publication** to Spring’s `ApplicationEventPublisher` after successful persistence.
- **Kafka/Schema Registry friendly event payloads** using Avro, enabling robust schema evolution.

### Benefits you get by using this library

Compared to implementing event sourcing from scratch, this library aims to reduce accidental complexity by giving you:

- **A consistent execution model** for commands → events → persistence → publication.
- **Safer concurrency** with deterministic revisioning and retry semantics.
- **Better operability** through the ability to read events as JSON, plus consistent headers (`aggregate-id`, `revision`).
- **Schema evolution support** through Avro + Schema Registry and automatic upcasting when event schemas change.
- **An opinionated, production-oriented baseline** (database schema, retries, Spring integration) that still allows you to
  plug in custom behavior where needed.

---

## Event Sourcing fundamentals

Event Sourcing is an architectural pattern where all changes to an aggregate’s state are captured as an ordered stream of
immutable events. The event stream is the system of record; the latest state is derived by replaying events.

![Event flow](doc/events.svg "Event flow")

### Key concepts

- **Event**: A fact that something happened in the past. Events are immutable.
- **Aggregate**: A consistency boundary in the domain (e.g., an Account). Each aggregate has its own event stream.
- **Command**: An intent to perform an action (e.g., Deposit money). Commands are validated against current state.
- **Projection / State**: A derived view of state built by replaying events.
- **Revision**: A monotonically increasing number per aggregate that represents event order and enables optimistic
  concurrency.

### Commands, events, and aggregates

Business logic is modeled as command handling that produces zero, one or more events:

1. A command is sent to the system.
2. A command handler evaluates the command against current aggregate state + business rules.
3. If invalid, an exception is thrown.
4. If accepted, the handler emits zero, one, or more events.

![Command handler](doc/commands.svg "Command handler")

Combining state reconstruction and command handling gives the complete model:

1. Derive current state from all historical events.
2. Apply a command based on that state to produce new events.

![Full model](doc/full-model.svg "Full model")

#### First command for a new aggregate

The first command for a new aggregate is evaluated against an empty (`null`) state because no events exist yet.

### State reconstruction (projection)

This library models state reconstruction explicitly:

- A `RootStateProjector` creates the initial `DomainState` instance given the **first event**.
- The `DomainState` instance then handles subsequent events sequentially via `onEvent` until it represents the current
  state.

![Event impl flow](doc/events2.svg "Event impl flow")

#### Reflective `RootStateProjector`

The library includes a reflective `RootStateProjector` implementation:

- It scans classes implementing `DomainState`.
- It chooses a constructor that takes exactly one `Event` parameter.
- The constructor is used to create the initial state from the first event.
- It scans the same packages that your regular component scan does to find Spring components, but it searches for 
  classes implementing the `DomainState` interface instead.

If you need more control (multiple aggregate types, non-standard initialization, etc.), you can implement your own
`RootStateProjector`.

#### DomainState example

Below is a complete `Account` aggregate that implements `DomainState`.
It models a bank account with a balance and reacts to three domain events.

```kotlin
data class Account(val balance: Int = 0) : DomainState {
  constructor(event: AccountOpenedEvent) : this()

  private fun on(event: MoneyWithdrawnEvent) = copy(balance = balance - event.amount)

  private fun on(event: MoneyDepositedEvent) = copy(balance = balance + event.amount)

  override fun onEvent(event: Event): Account =
    when (event) {
      is MoneyWithdrawnEvent -> on(event)
      is MoneyDepositedEvent -> on(event)
      else -> throw IllegalArgumentException("Unsupported event type")
    }
}
```

##### How projection works — step by step

Consider the following sequence of events for a single account:

```kotlin
val events = listOf(
    AccountOpenedEvent(accountId),       // revision 1
    MoneyDepositedEvent(accountId, 300), // revision 2
    MoneyWithdrawnEvent(accountId, 100), // revision 3
)
val accountState = domainStateProjector.currentState(events) as Account
// accountState.balance == 200
```

`DomainStateProjector.currentState` processes the list in two phases:

| Phase | Event | Mechanism | Resulting state |
|-------|-------|-----------|-----------------|
| **1 — Initial state** | `AccountOpenedEvent` | The **reflective `RootStateProjector`** finds the `Account(event: AccountOpenedEvent)` constructor via reflection and calls it, producing the first `Account` instance (`balance = 0`). | `Account(id=…, balance=0)` |
| **2 — Fold remaining events** | `MoneyDepositedEvent(300)` | `account.onEvent(event)` dispatches to `on(MoneyDepositedEvent)`, returning a new copy with `balance = 300`. | `Account(id=…, balance=300)` |
| | `MoneyWithdrawnEvent(100)` | `account.onEvent(event)` dispatches to `on(MoneyWithdrawnEvent)`, returning a new copy with `balance = 200`. | `Account(id=…, balance=200)` |

Key design points:

- **The constructor is the entry point for the first event only.** The reflective `RootStateProjector` scans all
  classes implementing `DomainState`, finds a constructor that accepts exactly one `Event` parameter, and invokes it.
  This means the constructor should only capture the data carried by the opening event (e.g. the account ID).
- **`onEvent` must reject the opening event on subsequent calls.** Because `AccountOpenedEvent` is handled by the
  constructor, an `Account` should already exist if that event is seen again — throwing an exception is the correct
  defensive behaviour.
- **Each `onEvent` call returns a new immutable copy** (via Kotlin's `data class` `copy`). State is never mutated
  in-place, making the fold over the event list safe and predictable.

#### Command execution API

Use `CommandService.apply` when exactly one event should be created, or `CommandService.applyList` when the command may
produce zero to many events.

A command handler is typically implemented as a function/class that takes the current state and returns event(s). For
example:

```kotlin
class DepositMoneyCommandHandler(private val account: Account?) {
   fun handle(command: DepositMoneyCommand): MoneyDepositedEvent {
      checkNotNull(account) { "Account not found" }
      require(command.depositAmount > 0) { "Amount to deposit cannot be zero or negative" }
      return MoneyDepositedEvent(command.accountId, command.depositAmount)
   }
}
```

And executed like this:

```kotlin
@Operation(summary = "Deposit money to an account")
@PutMapping("/{accountId}/deposit")
fun depositMoneyToAccount(
   @Schema(example = "b4e37836-049b-40d3-b872-330d863fc2b9", required = true)
   @PathVariable("accountId") accountId: AccountId,
   @RequestBody request: DepositMoneyRequest
) {
   val command = request.toCommand(accountId)
   commandService.apply(command.accountId) { account -> DepositMoneyCommandHandler(account).handle(command) }
}
```

### Optimistic concurrency

Event-sourced systems commonly use optimistic concurrency per aggregate.
This library uses a unique database constraint (`aggregate_unique_constraint`) on `(aggregate_id, revision)`.

If two commands try to write the same next revision concurrently:

- one transaction succeeds,
- the other fails on the constraint,
- the library retries the failed command,
- the retried command is evaluated against the new state that includes the concurrently written event(s).

This keeps the aggregate consistent without global locks.

### Schema evolution and upcasting

All events used by this library should be encoded as **Avro** and registered in **Schema Registry** using the
`TopicRecordNameStrategy` and compatability mode `BACKWARD_TRANSITIVE`. The topic must match the value in the
`events-payload-topic` property. For example:

![Schema Registry](doc/schema-registry.png "Schema Registry")

With Avro + Schema Registry you can:

- evolve event schemas using compatibility rules,
- deserialize older events with newer code,
- automatically upcast from older schema versions.

When used correctly, all events should be serializable/deserializable to/from the event store while supporting automatic
upcasting when events are updated.

---

## Spring integration

When an event is successfully saved to the event store, both:

- the event payload, and
- the full `EventEntity`

are published via Spring’s `ApplicationEventPublisher`.

You can handle these using `@TransactionalEventListener`.

### Publishing to Kafka in local development

For local development using an in-memory H2 database, you can use the `dev` profile to publish events to Kafka by
setting:

- `publish-events: true`

Event listeners are commonly used to:

- start/stop BPMN processes,
- publish messages to Kafka or other brokers,
- integrate with external systems.

### HTTP response headers

In a Servlet-based environment (not WebFlux), the library sets the HTTP headers:

- `aggregate-id`
- `revision`

The `revision` value is the highest revision number persisted within the current transaction.

### Read events as JSON

Use `EventsService.getEvents()` to read all events from the event store in JSON format.

### Publishing Oracle events via Kafka Connect

If events written to the central Oracle database should be published to Kafka, use a Kafka Connect connector as in the
[account-events.json](spring-boot-event-store/src/test/resources/httprequests/connectors/account-events.json) example.

---

## Configuration

This library is based on Spring, and it will autoconfigure itself when included. To use it:

- Import the spring-boot-eventy-store module as a Maven dependency
- Create one or more domain state classes that implement the `DomainState` interface
- Include `eventsourcing-changelog.yaml` in your Liquibase changelog (or create the objects manually in the database):

```yaml
databaseChangeLog:
  - include:
      file: classpath:/se/sbab/credit/eventsourcing/db/liquibase/eventsourcing-changelog.yaml
```

**NOTE**: Some versions of Liquibase have problems referring to a relative file included on the Java classpath.
This functionality has been tested using `liquibase-core` version `4.22.0`.

### Schema Registry configuration

Add Schema Registry configuration to your Spring `application.yml`:

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

- `events-payload-topic` is the Kafka topic used by the Avro serializer to find the Subject when using
  [`TopicRecordNameStrategy`](https://docs.confluent.io/current/schema-registry/serdes-develop/index.html#overview).
- Use `publish-events: true` to publish events to Kafka when using the `dev` profile (without Kafka Connect).

**NOTE**: The example above is intended for local development.
Outside local development environments, `auto.register.schemas` should typically be `false` and schema registration should
be part of your controlled deployment process.

Recommended Schema Registry settings:

- strategy: `TopicRecordNameStrategy`
- compatibility: `BACKWARD_TRANSITIVE`

Using `TopicRecordNameStrategy` allows multiple event types on the same topic.
`BACKWARD_TRANSITIVE` ensures all previously stored events can be read by the latest version of the event JAR.

It is also possible to use an in-memory mock Schema Registry for local testing:
`schema.registry.url=mock://localhost:8081`.

### Batch inserts

If a command handler returns multiple events, configure Hibernate batch inserts:

```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 20
```

The batch size should match the maximum number of events returned by a single command handler.

To validate batch inserts during development, you can enable statistics:

```yaml
spring:
  jpa:
    properties:
      hibernate:
        generate_statistics: true
```

More information about batch inserts:
https://www.baeldung.com/spring-data-jpa-batch-inserts

### Oracle notes (`RAW` UUID mapping)

When using an **Oracle** database, the `aggregate_id` column is mapped to a `RAW` type.
The helper functions `uuid_to_raw` and `raw_to_uuid` added by this library can be used to handle this:

```sql
SELECT ID, raw_to_uuid(AGGREGATE_ID) AS AGGREGATE_ID, REVISION, OCCURRED_AT, PAYLOAD
FROM EVENTS
WHERE AGGREGATE_ID = uuid_to_raw('b4e37836-049b-40d3-b872-330d863fc2b9');
```

This library also creates a database view named `EVENTS_VIEW`:

```sql
SELECT *
FROM EVENTS_VIEW
WHERE AGGREGATE_ID = 'b4e37836-049b-40d3-b872-330d863fc2b9';
```

Result:

|  ID | AGGREGATE_ID                       | REVISION | OCCURRED_AT                 | SCHEMA_ID | PAYLOAD    |
|----:|------------------------------------|---------:|-----------------------------|----------:|:----------:|
|   1 |b4e37836-049b-40d3-b872-330d863fc2b9|        1 |2023-02-21 12:01:33,011803000|         1 |   (BLOB)   |
|   2 |b4e37836-049b-40d3-b872-330d863fc2b9|        2 |2023-02-21 12:01:33,097938000|         2 |   (BLOB)   |

Note that `EVENTS_VIEW.AGGREGATE_ID` is a `VARCHAR2` column and cannot use the unique index based on the `RAW`
`AGGREGATE_ID` and `REVISION`.
This may reduce performance for some queries that filter by `AGGREGATE_ID`.

---

## Demo application modules

The `spring-boot-event-store` module is the usable library artifact in this multi-module project.
The other modules are libraries/services that demonstrate how to structure and run a complete example of an
event-driven architecture.

---

## BFF (eventual consistency gateway)

The `account-bff` service demonstrates how the `revision` header can be used to manage eventual consistency between the
write side (`account-command-service`) and the read side (`account-query-service`).

When you deposit money into an account, the request is routed to the write side.
The `account-bff` translates the `revision` response header into a `poll-url` header.
Clients can poll until the read side has processed events up to (at least) that revision.

Example:

```
curl -i -X 'PUT' \
  'http://localhost:8888/account-bff/api/v1/command/accounts/0112d278-19d2-483f-9f0c-4658bbcedae0/deposit' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
  "amount": 100
}'
HTTP/1.1 204 No Content
aggregate-id: 0112d278-19d2-483f-9f0c-4658bbcedae0
revision: 7
Date: Wed, 15 Apr 2026 10:19:27 GMT
poll-url: /api/v1/accounts/0112d278-19d2-483f-9f0c-4658bbcedae0/revision/7
```

Poll until the read model is up-to-date:

```
curl -i -X 'GET' \
  'http://localhost:8888/account-bff/api/v1/accounts/0112d278-19d2-483f-9f0c-4658bbcedae0/revision/7'
HTTP/1.1 200 OK
Content-Length: 0
Date: Wed, 15 Apr 2026 10:19:36 GMT
```

A `200` response means the read model is up-to-date with at least revision `7` for aggregate ID
`0112d278-19d2-483f-9f0c-4658bbcedae0`.

To more easily observe eventual consistency, request a higher revision:

```
curl -i -X 'GET' \
  'http://localhost:8888/account-bff/api/v1/accounts/0112d278-19d2-483f-9f0c-4658bbcedae0/revision/8'
HTTP/1.1 204 No Content
Date: 15 Apr 2026 10:19:39 GMT
```

Here, `204` indicates that revision `8` has not yet arrived over Kafka to the read side.

**NOTE**: This library has been tested with Kotlin and Java.

---

## Local Kafka

Use the official Docker Compose configuration
[cp-all-in-one](https://github.com/confluentinc/cp-all-in-one/blob/v7.7.1/cp-all-in-one/docker-compose.yml)
to start a local Kafka cluster.

---

## View events on Kafka using Kafbat UI

Download and install [Kafbat UI](https://github.com/kafbat/kafka-ui) to view events published to Kafka.
