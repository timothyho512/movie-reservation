# RabbitMQ Async Work

## Why This Comes After The Outbox

The transactional outbox makes event creation reliable. RabbitMQ makes event processing asynchronous.

Without the outbox, the app could commit a booking and then fail before publishing an email or analytics message. With the outbox, the booking state and event row commit together. RabbitMQ then becomes the delivery mechanism for work that should not happen inside the payment webhook request path.

## What This Adds

- A topic exchange for backend domain events.
- Queues for email, reporting, and audit-style consumers.
- A RabbitMQ implementation of `OutboxEventPublisher`.
- A first worker that logs booking email work instead of sending real email.

## Why RabbitMQ Instead Of Kafka

RabbitMQ fits this project because the next work is job-style async processing: send email, update projections, and run side effects after checkout. Kafka is better when the main requirement is long-lived event streams, replay, analytics pipelines, or many independent consumers rebuilding state.

For interviews, RabbitMQ is enough to show the important backend principle: keep critical request paths short and move side effects behind a durable queue.

## V1 Boundary

V1 proves database-to-broker-to-worker flow. It does not add SMTP, SendGrid, dead-letter queues, or a full event replay architecture.
