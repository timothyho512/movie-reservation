# Checkout Payment v2: Stripe Checkout

## Goal

Replace fake confirm-time payment with Stripe-hosted checkout.

The production payment flow is:

1. lock seats
2. create a Stripe Checkout Session
3. redirect customer to Stripe
4. receive Stripe webhook
5. create reservation only after verified payment success
6. let frontend poll checkout status

`POST /checkout/confirm` is now a legacy fake-payment path kept temporarily for development/tests. The canonical real-payment path is `POST /checkout/session` plus Stripe webhook finalization.

## Core Rules

- `SeatLock` is the temporary hold before payment success.
- `CheckoutSession` is the payment orchestration record.
- `Reservation` is created only after `checkout.session.completed` is verified.
- Stripe webhook confirmation is authoritative; the frontend success redirect is not.
- Guest ownership uses `guestEmail + sessionId`.
- Authenticated ownership uses the JWT principal only.
- Duplicate Stripe webhooks must be idempotent.

## API Contract

### POST `/checkout/lock`

Creates active seat locks.

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

Response includes:

- `sessionId` for guest checkout
- `expiresAt`
- `lockedSeatIds`
- message

### POST `/checkout/session`

Creates the internal `CheckoutSession`, creates the Stripe Checkout Session, stores Stripe identifiers, and returns the Stripe redirect URL.

Guest request:

```json
{
  "showtimeId": 1,
  "seatIds": [10, 11],
  "guestEmail": "guest@example.com",
  "sessionId": "guest-lock-session-id"
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
  "checkoutReference": "chk_...",
  "stripeCheckoutSessionId": "cs_test_...",
  "checkoutUrl": "https://checkout.stripe.com/...",
  "status": "PENDING_PAYMENT",
  "expiresAt": "2026-04-14T12:30:00",
  "message": "Checkout session created successfully"
}
```

Important behavior:

- all requested seats must still be actively locked by the caller
- no reservation is created here
- locks stay `LOCKED` while the customer is on Stripe

### GET `/checkout/session/{checkoutReference}`

Returns checkout status for frontend polling after Stripe redirect.

Pending response:

```json
{
  "checkoutReference": "chk_...",
  "status": "PENDING_PAYMENT",
  "reservationId": null,
  "bookingReference": null,
  "message": "Checkout session is awaiting payment"
}
```

Finalized response:

```json
{
  "checkoutReference": "chk_...",
  "status": "FINALIZED",
  "reservationId": 42,
  "bookingReference": "BK123456789",
  "message": "Checkout session finalized successfully"
}
```

Guest status lookup currently relies on the unguessable `checkoutReference`. Keep this response low-risk. If sensitive data is added later, add a guest status token or guest identity verification.

### POST `/checkout/webhook/stripe`

Receives Stripe webhooks.

Handled events:

- `checkout.session.completed`
- `checkout.session.expired`

Behavior:

- verifies Stripe signature
- ignores unhandled events safely
- ignores unknown Stripe session IDs safely
- treats already-finalized sessions idempotently

`checkout.session.completed`:

- loads internal checkout session by Stripe Checkout Session ID
- validates the checkout is not cancelled, expired, failed, or finalized
- validates locks are still active and owned by the checkout owner
- creates reservation with `ReservationStatus.CONFIRMED`
- sets reservation `PaymentStatus.PAID`
- converts locks to `CONVERTED_TO_RESERVATION`
- links reservation to checkout session
- marks checkout session `FINALIZED`

`checkout.session.expired`:

- marks pending checkout session `EXPIRED`
- does not create reservation
- does not mutate locks; normal lock cleanup/cancel flow releases locks

## Data Model

### CheckoutSession

Stores the payment attempt and Stripe linkage.

Key fields:

- `checkoutReference`
- showtime
- authenticated user or guest identity
- `itemsSnapshotJson`
- `totalAmount`
- `currency`
- status
- `stripeCheckoutSessionId`
- `stripePaymentIntentId`
- `stripeCustomerEmail`
- `checkoutUrl`
- optional linked reservation
- `expiresAt`
- timestamps

`itemsSnapshotJson` is a durable purchase snapshot. It stores seat/item details such as seat ID, row, number, type, and unit price. This keeps payment history stable even if live seat metadata changes later.

### Statuses

`CheckoutSessionStatus`:

- `PENDING_PAYMENT`
- `PAID`
- `FAILED`
- `CANCELLED`
- `EXPIRED`
- `FINALIZED`

Current flow primarily uses:

- `PENDING_PAYMENT`
- `CANCELLED`
- `EXPIRED`
- `FINALIZED`

`PAID` is available as an intermediate state but current webhook finalization goes straight to `FINALIZED` after reservation creation.

## Lifecycle

### Happy Path

1. customer locks seats
2. backend creates `CheckoutSession`
3. backend creates Stripe Checkout Session
4. frontend redirects to Stripe
5. Stripe sends `checkout.session.completed`
6. backend verifies event
7. backend creates reservation
8. backend converts locks
9. backend marks checkout `FINALIZED`
10. frontend polls status and shows booking confirmation

### Payment Abandoned

1. customer creates Stripe Checkout Session
2. customer leaves Stripe or does not pay
3. no completed webhook arrives
4. Stripe may send `checkout.session.expired`
5. backend marks checkout `EXPIRED`
6. seat locks expire through existing cleanup if not cancelled first
7. no reservation is created

### Customer Cancels Before Payment

1. customer has active locks and maybe a pending checkout session
2. frontend calls `POST /checkout/cancel`
3. locks become `EXPIRED`
4. pending checkout sessions for that owner/showtime become `CANCELLED`
5. finalized checkout sessions are not changed

### Duplicate Webhook

1. Stripe retries `checkout.session.completed`
2. backend finds checkout already `FINALIZED`
3. backend returns success without creating another reservation

## Cleanup

Existing scheduled cleanup now handles:

- timed-out `SeatLock` rows
- stale `CheckoutSession` rows where status is `PENDING_PAYMENT` and `expiresAt` is in the past

Cleanup transitions:

- `SeatLock: LOCKED -> EXPIRED`
- `CheckoutSession: PENDING_PAYMENT -> EXPIRED`

Cleanup does not modify finalized checkout sessions or converted locks.

## Test Coverage

Integration tests cover:

- guest and authenticated checkout session creation
- wrong owner/session rejection
- checkout status lookup
- completed webhook reservation finalization
- duplicate completed webhook idempotency
- expired webhook handling
- cancel flow marking pending checkout sessions `CANCELLED`
- stale pending checkout cleanup
- timed-out lock cleanup

Unit tests cover:

- Stripe completed webhook signature parsing
- Stripe expired webhook signature parsing
- invalid Stripe signature rejection
- ignored/unhandled Stripe event types

## Non-Goals

Not included yet:

- refunds
- custom Payment Intents UI
- multiple payment providers
- invoice flows
- full removal of legacy fake `/checkout/confirm`
- redesign of direct `POST /api/reservations`
