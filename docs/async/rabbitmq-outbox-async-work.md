# RabbitMQ Async Work

The checkout webhook should stay short: verify Stripe, finalize internal state, and commit the transactional outbox event. Slow or failure-prone side effects run after the database transaction through RabbitMQ workers.

## Flow

1. Stripe calls `POST /checkout/webhook/stripe`.
2. The backend verifies the signature and locks the checkout session row.
3. The backend creates or updates booking state inside Postgres.
4. The same transaction writes an `outbox_events` row.
5. The scheduled outbox worker claims due rows and publishes them to RabbitMQ.
6. RabbitMQ routes the event to worker queues.
7. Workers handle side effects such as booking email, cancellation email, reporting projections, or audit consumers.

The first RabbitMQ worker logs the email action only. Real email delivery is intentionally a later integration step, but the worker already has consumer-side idempotency so duplicate RabbitMQ delivery will not duplicate the side effect.

## Broker Shape

- Exchange: `movie-reservation.events`
- Exchange type: topic
- Queues:
  - `movie-reservation.email`
  - `movie-reservation.reporting`
  - `movie-reservation.audit`
- Routing keys:
  - `reservation.created`
  - `reservation.cancelled`
  - `checkout.payment.finalized`
  - `checkout.session.expired`

Messages use the outbox payload JSON as the body. Metadata is sent as headers: `eventId`, `eventType`, `aggregateType`, and `aggregateId`.

## Failure Behavior

The outbox processor owns publish retries in this version. If RabbitMQ publishing fails, the event remains retryable in Postgres and is scheduled for another attempt. RabbitMQ dead-lettering is not part of the first version because the immediate reliability boundary is database-to-broker publishing.

Consumers are idempotent because RabbitMQ delivery is at-least-once. The email worker uses the `eventId` header and `consumerName` to insert a row into `processed_outbox_events` before processing:

```text
event_id + consumer_name
```

That pair is unique. If the same event is delivered to the same consumer again, the insert conflicts and the worker skips the duplicate. Different consumers can still process the same event independently because they use different `consumerName` values.

## Related Diagrams

- [RabbitMQ outbox async work](../diagrams/async/rabbitmq-outbox-async-work.puml)
- [RabbitMQ outbox retry flow](../diagrams/async/rabbitmq-outbox-retry-flow.puml)
