# Real-Time Telecom Alarm & Notification Service

A Spring Boot backend that accepts telecom network alarm events, maintains their current state and complete history in PostgreSQL, and optionally dispatches asynchronous HTTP notifications.

## Technology stack

- Java 21, Spring Boot 4, Maven
- Spring MVC, Spring Data JPA / Hibernate, Bean Validation
- PostgreSQL 17, Flyway migrations
- Docker Compose
- JUnit 5, Mockito, Spring Boot integration tests, Testcontainers (PostgreSQL)

## Startup instructions

**Requirements:** Docker with Docker Compose. For local development without Docker - Java 21 and Maven are also required.

```bash
git clone https://github.com/DimasDev/alarm-service.git

cd alarm-service

cp .env.example .env 

docker compose up --build -d
```

- The API is available at `http://localhost:8080`.
- PostgreSQL is published to `127.0.0.1:5432` on the host.
- Flyway applies database migrations on application startup;
- Stop the application with `docker compose down`. Add `-v` only when you intentionally 
want to delete the database volume.

For a local Java process, start PostgreSQL and 
configure `DB_URL=jdbc:postgresql://localhost:5432/alarmdb` with the same credentials, 
then run `./mvnw spring-boot:run` 
(or `mvn spring-boot:run` if the repository does not include the Maven wrapper).

## Sample Requests

### Submit an alarm

```bash
curl -i -X POST http://localhost:8080/api/alarms \
  -H 'Content-Type: application/json' \
  -d '{
    "eventId": "EVT-10001",
    "elementId": "BTS-100",
    "alarmCode": "CELL_DOWN",
    "severity": "CRITICAL",
    "status": "RAISED",
    "timestamp": 1791800000000,
    "description": "Cell is not responding"
  }'
```

A newly accepted event returns **201 Created** with an ingestion result, for example:

```json
{
  "created": true,
  "notificationStatus": "QUEUED"
}
```

The response distinguishes alarm persistence from notification handling:

| `notificationStatus` |                                            Meaning                                                |
| -------------------- | ------------------------------------------------------------------------------------------------- |
| `QUEUED`             | Alarm saved, Notification accepted into the in-memory dispatcher; **not** a delivery confirmation |
| `QUEUE_FULL`         | Alarm saved, but no notification slot was available; notification skipped                         |
| `DISABLED`           | Alarm saved; notifications are disabled                                                           |
| `DUPLICATE`          | Previously accepted `eventId`; no new history record or notification                              |

- A duplicate returns **200 OK** with `created: false` and `notificationStatus: "DUPLICATE"`.
- Invalid input returns **400 Bad Request**.
- Clearing a nonexistent active alarm returns **409 Conflict**.
- Database/persistence failures are not treated as successful ingestion.
- When the queue is full, **the alarm is still committed** and the API returns 
**201 Created** with `notificationStatus: "QUEUE_FULL"`.

### Clear an alarm

Use a **new** `eventId`, the same `elementId` and `alarmCode`, and `status: "CLEARED"`:

```bash
curl -i -X POST http://localhost:8080/api/alarms \
  -H 'Content-Type: application/json' \
  -d '{
    "eventId": "EVT-10002",
    "elementId": "BTS-100",
    "alarmCode": "CELL_DOWN",
    "severity": "INFO",
    "status": "CLEARED",
    "timestamp": 1791800060000,
    "description": "Cell recovered"
  }'
```

### Query endpoints

| Method |              Endpoint              |                          Purpose                          |
| ------ | ---------------------------------- | --------------------------------------------------------- |
| `GET`  | `/api/alarms/active`               | All currently active alarms                               |
| `GET`  | `/api/alarms/elements/{elementId}` | Active alarms for one network element                     |
| `GET`  | `/api/alarms/history`              | Alarm event history, optionally filtered/paginated/sorted |
| `GET`  | `/api/alarms/statistics`           | Counts of active alarms by severity                       |

Examples:

```bash
curl http://localhost:8080/api/alarms/active
curl http://localhost:8080/api/alarms/elements/BTS-100
curl 'http://localhost:8080/api/alarms/history?elementId=BTS-100&page=0&size=20&sortBy=timestamp&direction=desc'
curl http://localhost:8080/api/alarms/statistics
```

- History pagination defaults to page `0`, size `20`; 
- Supported page sizes are from `1` to `100`. 
- Supported sort fields: `id`, `timestamp`, `severity`, `status`, `elementId`, and `alarmCode`. 
- Direction is `asc` or `desc`.

## Design decisions

Two tables separate the history from current state:

- **`alarm_history`** records every unique accepted event. A unique constraint on `event_id` enforces idempotency at the database level.
- **`active_alarms`** stores at most one row per `(element_id, alarm_code)`, enforced by a unique constraint. `RAISED` creates or updates this row; `CLEARED` removes it. History is never deleted as part of a clear operation.

History insertion and active-state mutation occur in one Spring-managed database transaction. A failure during persistence rolls back the database changes together.

## Concurrency guarantees and known limitations

### Alarms persistence

PostgreSQL transaction-scoped advisory locks serialize conflicting database operations. Locks keyed by `eventId` protect concurrent duplicate submissions; locks keyed by the alarm identity protect simultaneous updates to the same active alarm. A local per-`elementId` lock spans persistence and notification enqueueing when notifications are enabled, preserving per-element enqueue order within one application instance.

**Processing order policy:** for conflicting events, the order in which they are serialized and accepted by the service determines the final active state. Incoming event timestamps are stored as supplied, but are **not** used to reorder or reject events. Consequently, an older-timestamp `RAISED` received after a `CLEARED` may reactivate the alarm. This is an intentional, deterministic processing-order policy rather than event-time reconciliation.

Different elements can be processed and notified concurrently. Notification tasks for the **same** element execute sequentially in FIFO order within the dispatcher. These in-memory ordering guarantees are scoped to a single service instance; PostgreSQL locks protect shared database state, but do not coordinate notification dispatch across multiple application instances.

### Asynchronous notifications and backpressure

After a new event is committed, the ingestion path attempts to submit its notification to an in-memory dispatcher. HTTP delivery happens on worker threads, not on the request thread. A per-element task queue preserves sequential delivery attempts for the same element; multiple workers permit parallel processing across elements.

A semaphore bounds total outstanding notification tasks, **including running and waiting tasks**. Capacity is reserved before persistence when available, then released after task completion. If no capacity is available, persistence still proceeds and the response explicitly reports `QUEUE_FULL`. A duplicate event never generates another notification, including when the original notification was skipped.

HTTP connection and read timeouts are configurable. Delivery failures are logged; they do not terminate notification workers. The current dispatcher performs **no automatic retries**. `QUEUED` confirms only acceptance into volatile memory; it does not mean the receiver accepted or processed the notification.

**Reliability trade-off:** alarm persistence is prioritized over best-effort notifications. Notification work can be lost on process restart, between database commit and enqueue, on queue saturation, or after a failed HTTP delivery. The API makes queue saturation visible, but it cannot guarantee delivery.

### Known limitations

- The service handles synthetic telecom data; no authentication, authorization, or subscriber-data integration is included.
- `eventId` is globally unique. A replay is treated as a duplicate even if its payload differs from the original event.
- Alarm state follows **processing order**, not event timestamp order.
- Notification delivery is **best-effort**, without retries or durable buffering.
- Queue saturation does not reject the alarm; it explicitly reports a skipped notification.
- In-memory per-element locks and notification queues are single-instance mechanisms. Multiple application instances would require coordinated notification ordering.
- In-memory per-element queue/lock entries are retained; eviction or lifecycle cleanup would be needed for a very large number of distinct elements.
- A failure after the database commit but before or during notification submission may leave a saved alarm without a queued notification. A later duplicate submission will not repair that omission.
- No delivery status history, dead-letter queue, or operational metrics for notification failures is persisted.

## Tests

Integration tests use JUnit 5, Spring Boot, Mockito and a real PostgreSQL 17 instance via Testcontainers. Docker must be running for the integration suite.

```bash
./mvnw test
# or: mvn test
```

The suite covers request validation and HTTP responses, alarm state transitions, event-id deduplication (including concurrent requests), clearing unknown alarms, history and query APIs, per-element notification ordering, concurrency across elements, and queue saturation.

The queue-full tests intentionally block the notification worker using `CountDownLatch`, then fill the bounded capacity. They assert that the extra alarm **is persisted**, the service returns `QUEUE_FULL`, and the REST API returns **201 Created**. A duplicate replay of the skipped event must not create another history record or trigger a notification.

## Possible production improvements

**Transactional Outbox** is the primary reliability improvement. In the same database transaction as the history and active-state updates, persist a notification-outbox record. A separate worker would claim pending records, send notifications, retry transient failures with backoff, and record delivery state. Recovery after restart would then be possible without losing committed notification intents.

With durable retry handling, monitoring, and a reachable receiver, this approach supports **at-least-once delivery attempts**, rather than the current best-effort model. It does **not** imply exactly-once delivery: an HTTP request may succeed while its acknowledgment is lost, so the receiver should deduplicate by `eventId`. A multi-instance design would additionally need coordinated per-element sequencing/claiming (or partitioning by `elementId`), retry limits, and dead-letter handling.

Other potential improvements include notification metrics and alerts, structured error reporting, graceful drain on shutdown, and lifecycle cleanup of per-element in-memory structures.

## AI assistance disclosure

AI-assisted tools were used for brainstorming, discussing architecture and concurrency trade-offs, drafting documentation, and writing tests.
Design choices, implementation, and final verification was my responsibility.