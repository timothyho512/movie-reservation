# RabbitMQ Async Work Implementation Plan

## Target Flow

1. Checkout webhook finalizes payment and booking state.
2. The same database transaction writes an outbox event.
3. `OutboxEventScheduler` polls due events.
4. `RabbitMqOutboxEventPublisher` publishes the event payload to RabbitMQ.
5. RabbitMQ routes the message to email, reporting, and audit queues.
6. `BookingEmailConsumer` logs the email side effect for now.

## Backend Changes

- Add `spring-boot-starter-amqp`.
- Add RabbitMQ to Docker Compose with ports `5672` and `15672`.
- Configure a topic exchange named `movie-reservation.events`.
- Configure durable queues:
  - `movie-reservation.email`
  - `movie-reservation.reporting`
  - `movie-reservation.audit`
- Map event types to routing keys:
  - `ReservationCreated` -> `reservation.created`
  - `ReservationCancelled` -> `reservation.cancelled`
  - `CheckoutPaymentFinalized` -> `checkout.payment.finalized`
  - `CheckoutSessionExpired` -> `checkout.session.expired`
- Keep `OutboxEventPublisher` as the boundary.
- Enable RabbitMQ publishing with `app.rabbitmq.enabled=true`.
- Disable RabbitMQ in tests with `app.rabbitmq.enabled=false`.

## Testing

- Test the event-type-to-routing-key mapper.
- Test that the RabbitMQ publisher sends the expected exchange, routing key, body, and headers.
- Keep existing outbox processor retry tests unchanged.
- Add a minimal consumer test proving the v1 worker accepts payloads without a real email provider.

## Later Work

- Add a real email provider adapter.
- Add idempotency storage for email sends.
- Add dead-letter queues if consumer failures need broker-level quarantine.
- Add metrics for outbox lag, publish failures, and consumer failures.
