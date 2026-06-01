# Interview Framing

## Short Version

I did not put email sending in the payment webhook request path. The webhook verifies Stripe, finalizes payment and booking state quickly, and writes a durable outbox event in the same transaction. A scheduled publisher sends that event to RabbitMQ, and async workers handle side effects such as booking email, reporting updates, and audit consumers.

## Why This Is Good Backend Design

- The critical payment path stays fast.
- Stripe receives a quick webhook response.
- Side effects can fail and retry without rolling back the booking.
- The database remains the source of truth for booking state.
- The outbox prevents losing events between the database commit and broker publish.
- RabbitMQ lets different workers process the same business event independently.

## RabbitMQ vs Kafka Answer

I chose RabbitMQ because this project needs practical async jobs: emails, reporting updates, and audit side effects. Kafka would be useful for durable replayable event streams, but it adds more operational complexity than this system needs right now.

## Failure Answer

If RabbitMQ is down, the outbox publisher fails and the event remains in Postgres as `FAILED` with a future `nextAttemptAt`. The scheduler retries until the max attempt count is reached. That means the booking can still be committed safely without pretending the email or reporting update succeeded.

## Duplicate Handling Answer

The system assumes at-least-once delivery. Consumers should be idempotent. For example, a real email worker should record the event id or booking reference before sending so retry or redelivery does not send duplicate customer emails.
