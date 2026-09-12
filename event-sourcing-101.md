# Event Sourcing 101 with `spring-boot-event-store`

This guide explains Event Sourcing fundamentals and how this framework keeps the implementation simple.

The target audience is developers who want the benefits of Event Sourcing without adopting a large framework-specific programming model.

---

## What this framework optimizes for

`spring-boot-event-store` is opinionated about one thing: keep application code simple and let the framework handle event-store plumbing.

In practice, that means:

- You write normal Spring Boot Kotlin/Java code
- Events are Avro records (`SpecificRecord`)
- Aggregates implement one small interface (`DomainState`)
- Command flow is explicit (`CommandService.apply` / `applyList`)

And the framework handles:

- Event persistence and state replay
- Optimistic concurrency retry on revision conflicts
- Event publication after persistence
- Read-time schema evolution support (upcasting)
- Consistent response headers (`aggregate-id`, `revision`) in servlet apps

---

## Mental model in 30 seconds

Event Sourcing stores facts, not current state snapshots.

Instead of writing:

```text
Account(balance = 60)
```

you append facts:

```text
AccountOpenedEvent
MoneyDepositedEvent(amount = 100)
MoneyWithdrawnEvent(amount = 40)
```

Current state is reconstructed by replaying events in order.

This gives you:

- Complete history
- Deterministic replay
- Easier debugging of "what happened"
- Rebuildable read models

---

## Key framework abstractions

### `DomainState`

```kotlin
interface DomainState {
    fun onEvent(event: Event): DomainState
}
```

Your aggregate state class implements this and defines deterministic transitions for each event type.

### `Event`

```kotlin
typealias Event = org.apache.avro.specific.SpecificRecord
```

Events are Avro generated records. This is the foundation for schema compatibility and upcasting.

### `AggregateId`

```kotlin
typealias AggregateId = java.util.function.Supplier<java.util.UUID>
```

Aggregate identity is UUID-based and strongly typed in domain code.

### `CommandService`

```kotlin
interface CommandService<T : DomainState> {
    fun apply(aggregateId: AggregateId, updateFunction: (T?) -> Event): Event
    fun applyList(aggregateId: AggregateId, updateFunction: (T?) -> List<Event>): List<Event>
    fun applyListWithoutState(aggregateId: AggregateId, updateFunction: () -> List<Event>): List<Event>
}
```

This is the main command execution API.

### `EventsService`

Use `EventsService.getEvents(aggregateId)` to read persisted events as JSON (`EventDto`) for diagnostics and history views.

---

## Example aggregate (from demo)

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

What matters:

- State is immutable (`copy(...)`)
- Replay is deterministic (same stream => same state)
- First event initializes aggregate (`constructor(event: AccountOpenedEvent)`)

---

## Commands vs events (important)

- **Commands** are intent and can be rejected
- **Events** are facts and must not be mutated once persisted

Invariants belong in command handling, not in replay logic.

Example command handler:

```kotlin
class DepositMoneyCommandHandler(private val account: Account?) {
    fun handle(command: DepositMoneyCommand): MoneyDepositedEvent {
        checkNotNull(account) { "Account not found" }
        require(command.depositAmount > 0) { "Amount to deposit cannot be zero or negative" }
        return MoneyDepositedEvent(command.accountId, command.depositAmount)
    }
}
```

If bad data was historically persisted, fix forward with compensating events rather than mutating history.

---

## Command flow in this framework

Application service code stays small:

```kotlin
@Service
class AccountCommandService(private val commandService: CommandService<Account>) {
    fun apply(command: DepositMoneyCommand): Event =
        commandService.apply(command.accountId) { account -> DepositMoneyCommandHandler(account).handle(command) }
}
```

At runtime, `apply` does this:

1. read current stream and reconstruct state
2. run your update function/handler
3. append emitted event(s) with next revision(s)
4. publish events transactionally to Spring listeners
5. set response headers `aggregate-id` and `revision` (servlet apps)

---

## Replay and projection mechanics

The internal replay algorithm is simple:

1. `RootStateProjector` creates initial state from the first event
2. remaining events are folded using `DomainState.onEvent`

This is implemented by `DomainStateProjector.currentState(events)`.

Because replay is deterministic, read models can be rebuilt from the event log whenever needed.

---

## Concurrency, idempotency, and revisions

### Optimistic concurrency

The event store enforces uniqueness on `(aggregate_id, revision)`.

If two writers race for the same next revision:

- One write succeeds
- One write fails with data integrity violation
- Framework retries automatically by reloading current state

This behavior is implemented in `CommandServiceImpl.retryOnDataIntegrityViolationException`.

### Revision as an integration contract

The `revision` header is useful for eventual consistency workflows.

Typical pattern:

1. command endpoint returns `revision`
2. client polls read-side status endpoint until read model has reached that revision

The demo query service exposes this as:

- `GET /accounts/{accountId}/revision/{revision}` -> `200` when caught up, otherwise `204`

### Idempotency note

For consumers/projections, revision-aware handling gives a clean way to guard against duplicates and out-of-order processing.

---

## Event schemas, versioning, and upcasting

Because events are Avro records, schema evolution is explicit.

Recommended setup:

- Subject naming strategy: `TopicRecordNameStrategy`
- Compatibility mode: `BACKWARD_TRANSITIVE`

When configured correctly:

- Incompatible schema changes are blocked at registration
- Old stored events remain readable by newer code
- Framework deserialization/upcasting keeps application code focused on latest event model

Schema Registry config example:

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

For local development without Kafka infrastructure, you can use:

```yaml
spring:
  kafka:
    properties:
      schema:
        registry:
          url: mock://localhost:8081
publish-events: false
```

---

## Operational tooling and debugging history

Two practical capabilities matter in production:

1. inspect event history quickly
2. explain aggregate behavior from facts

The demo command service exposes:

- `GET /events/{accountId}`

which returns a JSON list with event name, timestamp, revision, and payload.

This is enough to answer common questions like:

- What happened to this aggregate?
- In which order did events occur?
- Which revision introduced the current state?

---

## Quick hands-on flow

From repository root:

```bash
mvn clean install
java -jar account-command-service/target/account-command-service-1.0-SNAPSHOT.jar --spring.profiles.active=mock
```

Then use Swagger:

- `http://localhost:8080/account-command-service/swagger-ui.html`

Basic scenario:

1. `POST /accounts` (returns account id)
2. `PUT /accounts/{accountId}/deposit` with `{"amount": 100}`
3. `PUT /accounts/{accountId}/withdraw` with `{"amount": 40}`
4. `GET /events/{accountId}` to inspect history

---

## Common mistakes to avoid

- Validating business invariants inside replay methods (`onEvent`) instead of command handlers
- Mutating/deleting historical events instead of appending compensating events
- Treating projection/read models as source of truth
- Evolving Avro schemas without strict compatibility discipline
- Overusing Event Sourcing for simple CRUD domains

---

## How to think about this framework

Use it when you want Event Sourcing benefits with a lightweight development model:

- Explicit history and auditability
- Robust schema evolution over time
- Replayable state and rebuildable projections
- Clean eventual consistency contracts with revisions

In short: the framework should stay mostly invisible, while your domain model stays explicit and easy to reason about.
