# Checkout Payment

This is the practical Stripe Checkout flow used by the backend.

The frontend should treat this as the production-style booking path:

1. lock seats
2. create a Stripe Checkout Session
3. redirect the customer to Stripe
4. let Stripe call the webhook
5. poll checkout status
6. show the finalized reservation

`POST /checkout/confirm` exists only when `app.legacy-checkout.enabled=true`.
The `dev` and `test` profiles enable it for development and tests; production
explicitly disables it. New frontend booking work should use
`POST /checkout/session` and the Stripe webhook flow.

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
- New booking activity closes 10 minutes before the showtime starts.
- Redis locks, Postgres audit locks, the local checkout, and Stripe Checkout use one coordinated payment deadline.
- A paid checkout that cannot be finalized safely is automatically refunded.

## Booking And Expiration Policy

The default configuration is:

```text
Booking cutoff:        10 minutes before showtime
Seat-lock/payment TTL: 1860 seconds (31 minutes)
Cleanup frequency:     60 seconds
```

Creating a checkout does not restart the 31-minute timer. The checkout uses the
earliest expiration timestamp of its selected Redis locks, and Stripe receives
that same timestamp.

Example:

```text
14:00 seats locked
14:20 Stripe Checkout created
14:31 Redis locks, local checkout, and Stripe Checkout reach their deadline
```

If selected locks have different expiration timestamps, the earliest timestamp
is used because the purchase is unsafe as soon as any requested seat is no
longer held.

### Stripe Minimum Expiration Constraint

Stripe requires a hosted Checkout Session's `expires_at` to be at least 30
minutes after the Stripe Session creation request. Because the current lock
deadline is only 31 minutes after the earlier lock request, creating Stripe
Checkout more than roughly one minute later can leave less than Stripe's
required 30-minute minimum.

This is a known limitation of the current timing configuration. Before relying
on this policy in production, increase the lock/payment window enough to cover
normal seat-selection delay plus Stripe's minimum, or create the Stripe Session
earlier in the flow. Local expiration and late-payment refunds protect
consistency after a Session exists, but they do not make an invalid
`expires_at` acceptable to Stripe during Session creation.

## POST `/checkout/lock`

Creates temporary Redis seat holds and writes legacy Postgres `seat_locks` audit rows.

Clients should send one stable `Idempotency-Key` header per lock attempt:

```http
Idempotency-Key: lock-attempt-abc-123
```

If the frontend retries the same lock request after a timeout, the backend
returns the original lock response, including the guest `sessionId`. If the
same key is reused for a different showtime, seat selection, or checkout owner,
the backend returns `409 Conflict`. If the original lock attempt has expired,
the backend returns `410 Gone`.

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
2. acquire a pessimistic database lock on that checkout row while finalizing
3. exit safely if it is already `FINALIZED`
4. exit safely if it is already `REFUNDED`
5. start an idempotent refund if it is `CANCELLED`, `EXPIRED`, `FAILED`, or `REFUND_PENDING`
6. validate the Redis holds are still active and owned by the checkout owner
7. validate the seats have not been reserved elsewhere
8. create reservation with `CONFIRMED + PAID`
9. release Redis holds and mark audit locks `CONVERTED_TO_RESERVATION`
10. link reservation to checkout session
11. mark checkout session `FINALIZED`

The pessimistic checkout-row lock is held for the webhook transaction. If two
deliveries for the same Stripe Checkout Session arrive simultaneously, the
second waits, then observes the `FINALIZED` status written by the first. It does
not create another reservation or refund the original payment.

If payment succeeds but finalization cannot safely create the reservation, the
checkout moves through:

```text
REFUND_PENDING -> REFUNDED
```

Refund requests use a stable Stripe idempotency key based on the checkout
reference. A temporary refund failure leaves the checkout in `REFUND_PENDING`,
records `refundError`, and is retried by the cleanup scheduler.

Expired webhook behavior:

1. load and lock the checkout by `stripeCheckoutSessionId`
2. exit safely for an already terminal checkout
3. release Redis holds owned by this checkout
4. mark matching Postgres audit locks `EXPIRED`
5. evict the cached seat map
6. mark checkout session `EXPIRED`

## Local Expiration

The cleanup scheduler searches for:

```text
status = PENDING_PAYMENT
expiresAt < current time
```

For every stale checkout it:

1. asks Stripe to expire the hosted Checkout Session
2. explicitly releases the checkout owner's Redis locks
3. marks the Postgres audit locks `EXPIRED`
4. evicts the showtime seat-map cache
5. marks the local checkout `EXPIRED`
6. records `CheckoutSessionExpired` in the transactional outbox

Asking Stripe to expire a session calls Stripe's Checkout Session expiration
API using the stored `stripeCheckoutSessionId`. This closes the hosted payment
page so it cannot normally accept a new payment after the local deadline.

Stripe is an external system, so expiration can race with payment completion or
fail temporarily. Local cleanup still proceeds so seats are not held forever.
If Stripe later reports a successful payment for an expired checkout, the
backend does not create a reservation and instead starts the refund flow.

## Redis And Checkout Relationship

The three related records have different responsibilities:

| Record | Purpose |
| --- | --- |
| Redis seat lock | Fast active ownership check that prevents concurrent seat selection |
| Postgres `seat_locks` row | Durable audit record of the lock and its final status |
| Postgres `checkout_sessions` row | Payment attempt, selected-seat snapshot, Stripe IDs, and payment lifecycle |

Redis TTL enforcement is immediate, while the Postgres checkout changes to
`EXPIRED` when the scheduler runs. A short interval can therefore exist where:

```text
Redis lock: missing
Checkout: PENDING_PAYMENT with an elapsed expiresAt
```

A completed webhook in this interval cannot prove seat ownership. It creates no
reservation and refunds the payment. This deliberately prioritizes preventing
double booking.

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
- `FAILED`: payment/finalization failure that did not produce a reservation
- `REFUND_PENDING`: Stripe accepted payment but reservation finalization failed; refund is still pending
- `REFUNDED`: the late or unfulfillable payment was refunded
- `PAID`: available enum value, not the normal final state in the current flow

`LockStatus` values for Postgres audit rows:

- `LOCKED`: Redis hold was created and an audit row was recorded
- `CONVERTED_TO_RESERVATION`: held seat became a paid reservation
- `EXPIRED`: hold was released, cancelled, or timed out
- `PROCESSING`: legacy value kept for old data/tests; not used by the Redis flow

Redis hold keys expire naturally after `app.seat-lock.ttl-seconds`, currently
1860 seconds. Checkout cleanup also releases them explicitly.

## Showtime Lifecycle

`ShowtimeLifecycleScheduler` runs every 60 seconds and applies idempotent bulk
transitions:

```text
UPCOMING and startTime <= now < endTime -> ONGOING
UPCOMING or ONGOING and endTime <= now -> COMPLETED
paid CONFIRMED reservations for completed showtimes -> COMPLETED
```

Cancelled showtimes and cancelled reservations remain cancelled.

When a showtime starts or completes, lifecycle cleanup also:

- expires remaining `PENDING_PAYMENT` checkouts for that showtime
- asks Stripe to expire their hosted sessions
- releases remaining Redis locks
- expires active Postgres audit locks
- evicts the showtime seat-map cache

Customer showtime lists return only active `UPCOMING` showtimes that start more
than 10 minutes in the future. The customer seat-map endpoint also rejects
closed showtimes. Admin seat-layout management uses separate `/api/admin/**`
endpoints and is not blocked by the customer booking cutoff.

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
- cancelled/expired checkout late-payment refunds
- missing or expired Redis lock refunds
- failed refund retry from `REFUND_PENDING` to `REFUNDED`
- booking cutoff rejection and customer showtime filtering
- exact showtime start/end boundaries and repeated scheduler execution
- paid reservation completion
- cancelled reservation and cancelled showtime preservation
