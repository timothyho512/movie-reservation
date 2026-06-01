# Checkout Payment

This is the practical Stripe Checkout flow used by the backend.

The frontend should treat this as the production-style booking path:

1. lock seats
2. create a Stripe Checkout Session
3. redirect the customer to Stripe
4. let Stripe call the webhook
5. poll checkout status
6. show the finalized reservation

`POST /checkout/confirm` still exists as a legacy fake-payment path for development/tests. New frontend booking work should use `POST /checkout/session` and the Stripe webhook flow.

## Local Setup

Start the backend and Stripe listener as described in `docs/local-development.md`.

The local webhook command is:

```sh
stripe listen --forward-to localhost:8080/checkout/webhook/stripe
```

Copy the printed `whsec_...` value into `.env` as `STRIPE_WEBHOOK_SECRET`, then restart the backend.

## Core Rules

- Redis is the active temporary hold store before payment succeeds.
- Postgres `SeatLock` rows are audit/history records, not the live availability authority.
- `CheckoutSession` stores the internal payment attempt and Stripe IDs.
- `Reservation` is created only after a verified `checkout.session.completed` webhook.
- The Stripe webhook is authoritative; the frontend success redirect does not create a reservation.
- Guest ownership uses `guestEmail + sessionId`.
- Authenticated ownership uses the JWT principal only.
- Duplicate Stripe webhooks are safe: repeated completed/expired events do not create duplicate reservations.

## POST `/checkout/lock`

Creates temporary Redis seat holds and writes legacy Postgres `seat_locks` audit rows.

Guest request:

```json
{
  "showtimeId": 1,
  "seatIds": [10, 11],
  "guestEmail": "guest@example.com"
}
```

Authenticated request:

```json
{
  "showtimeId": 1,
  "seatIds": [10, 11]
}
```

Response:

```json
{
  "showtimeId": 1,
  "lockedSeatIds": [10, 11],
  "sessionId": "guest-session-id",
  "expiresAt": "2026-05-28T19:30:00",
  "message": "Seats locked successfully"
}
```

For authenticated users, `sessionId` is `null`.

## POST `/checkout/session`

Creates the internal checkout session, creates the Stripe Checkout Session, stores Stripe IDs, and returns the redirect URL.

Clients should send one stable `Idempotency-Key` header per checkout attempt:

```http
Idempotency-Key: checkout-attempt-abc-123
```

If the frontend retries the same request with the same key after a timeout, the
backend returns the original `checkoutReference`, Stripe Checkout Session ID,
and `checkoutUrl`. If the same key is reused for a different showtime, seat
selection, or checkout owner, the backend returns `409 Conflict`.

Guest request:

```json
{
  "showtimeId": 1,
  "seatIds": [10, 11],
  "guestEmail": "guest@example.com",
  "sessionId": "guest-session-id"
}
```

Authenticated request:

```json
{
  "showtimeId": 1,
  "seatIds": [10, 11]
}
```

Response:

```json
{
  "checkoutReference": "chk_1776170000000abcdef",
  "stripeCheckoutSessionId": "cs_test_123",
  "checkoutUrl": "https://checkout.stripe.com/c/pay/cs_test_123",
  "status": "PENDING_PAYMENT",
  "expiresAt": "2026-05-28T19:30:00",
  "message": "Checkout session created successfully"
}
```

Important behavior:

- all requested seats must already be actively locked by the caller
- no reservation is created here
- Redis holds remain active while the customer is on Stripe
- checkout expiry is based on the earliest Redis hold expiry
- `checkoutReference` is the frontend-safe reference used for status polling
- `stripeCheckoutSessionId` is stored for webhook matching
- API-level idempotency protects checkout session creation before Stripe redirects
- Stripe webhook idempotency protects reservation finalization after payment

## Stripe Redirect

After `POST /checkout/session`, redirect the customer to `checkoutUrl`.

Do not create a reservation from the frontend success URL. The success URL only tells the frontend to poll the backend.

## POST `/checkout/webhook/stripe`

Receives Stripe webhooks.

Handled events:

- `checkout.session.completed`
- `checkout.session.expired`

Behavior:

- verifies the Stripe signature
- ignores unhandled event types safely
- ignores unknown Stripe session IDs safely
- handles duplicate events idempotently

Completed webhook behavior:

1. load checkout session by `stripeCheckoutSessionId`
2. lock that database row while finalizing
3. exit safely if it is already `FINALIZED`
4. fail safely if it was `CANCELLED`, `EXPIRED`, or `FAILED`
5. validate the Redis holds are still active and owned by the checkout owner
6. create reservation with `CONFIRMED + PAID`
7. release Redis holds and mark audit locks `CONVERTED_TO_RESERVATION`
8. link reservation to checkout session
9. mark checkout session `FINALIZED`

Expired webhook behavior:

1. load checkout session by `stripeCheckoutSessionId`
2. lock that database row while expiring
3. exit safely if it is already `FINALIZED`, `CANCELLED`, or `EXPIRED`
4. release the active Redis holds for the checkout session and mark audit locks `EXPIRED`
5. mark checkout session `EXPIRED`

## Transactional Outbox Events

Reservation and checkout state changes also write durable outbox events in the same Postgres transaction as the business update. This lets the backend publish follow-up side effects, such as email or analytics, without losing the event if publishing fails after the database commit.

Current events:

- `ReservationCreated`
- `ReservationCancelled`
- `CheckoutPaymentFinalized`
- `CheckoutSessionExpired`

The publisher sends due outbox events to RabbitMQ. A scheduled worker polls due outbox rows, marks them `PROCESSING`, publishes them to the `movie-reservation.events` topic exchange, then marks them `PUBLISHED` or `FAILED`. Failed rows are retried until their max attempt count is reached.

The first async consumer logs booking email work. Real email delivery is intentionally outside the first RabbitMQ integration.

See `docs/async/rabbitmq-outbox-async-work.md` for the broker-level design.

This is internal backend behavior. It does not change request/response shapes for checkout, reservation, or webhook endpoints.

## GET `/checkout/session/{checkoutReference}`

Returns checkout status for frontend polling after the Stripe redirect.

This endpoint treats `checkoutReference` as a bearer-style polling credential. The reference is randomly generated and included in the Stripe success/cancel redirect URL, so the frontend can poll status after a full page reload without needing the guest `sessionId` from in-memory state.

```http
GET /checkout/session/chk_1776170000000abcdef
```

No JWT, guest email, or guest session ID is required for checkout status polling. Reservation detail/history endpoints still enforce normal ownership checks.

Pending response:

```json
{
  "checkoutReference": "chk_1776170000000abcdef",
  "status": "PENDING_PAYMENT",
  "reservationId": null,
  "bookingReference": null,
  "message": "Checkout session is awaiting payment"
}
```

Finalized response:

```json
{
  "checkoutReference": "chk_1776170000000abcdef",
  "status": "FINALIZED",
  "reservationId": 42,
  "bookingReference": "BK1776170000000",
  "message": "Checkout session finalized successfully"
}
```

## Statuses

`CheckoutSessionStatus` values:

- `PENDING_PAYMENT`: customer has a Stripe Checkout Session but no completed webhook yet
- `FINALIZED`: payment completed and reservation was created
- `EXPIRED`: Stripe checkout expired or local checkout timed out
- `CANCELLED`: user cancelled the lock before payment finalized
- `FAILED`: Stripe reported completion after the checkout could no longer be finalized safely
- `PAID`: available enum value, not the normal final state in the current flow

`LockStatus` values for Postgres audit rows:

- `LOCKED`: Redis hold was created and an audit row was recorded
- `CONVERTED_TO_RESERVATION`: held seat became a paid reservation
- `EXPIRED`: hold was released, cancelled, or timed out
- `PROCESSING`: legacy value kept for old data/tests; not used by the Redis flow

Redis hold keys expire naturally after `app.seat-lock.ttl-seconds`, currently 900 seconds.

## Lifecycle

Happy path:

```text
POST /checkout/lock
POST /checkout/session
redirect to Stripe checkoutUrl
Stripe sends checkout.session.completed
backend creates reservation
GET /checkout/session/{checkoutReference}
GET /api/reservations/{id}
```

Abandoned payment:

```text
POST /checkout/lock
POST /checkout/session
customer does not pay
Stripe sends checkout.session.expired or local cleanup expires the checkout
Redis holds are released or expire naturally
no reservation is created
```

Duplicate webhook:

```text
Stripe sends checkout.session.completed twice
first webhook finalizes the checkout
second webhook sees FINALIZED and returns 200 without creating another reservation
```

## Test Coverage

Current backend tests cover:

- guest and authenticated checkout session creation
- checkout reference and Stripe session ID persistence
- wrong owner/session rejection
- checkout status lookup
- Stripe signature parsing
- invalid Stripe signature rejection
- completed webhook reservation finalization
- duplicate completed webhook idempotency
- expired webhook lock release
- duplicate expired webhook idempotency
- unknown/unhandled webhook events returning safely
- cancelled/expired checkout completion failure paths
