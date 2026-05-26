# Checkout Payment v1

> Note:
> This document describes the fake-payment v1 flow used before real provider integration.
> This is now a legacy/dev-test flow. The canonical production payment contract is
> the Stripe-backed flow in `docs/checkout/checkout-payment-V2.md`.

## Goal

Add payment-step realism to checkout without introducing a real payment provider yet.

This version keeps the current frontend-style flow:

1. lock seats
2. confirm checkout

In v1, `confirm checkout` means:

- validate lock ownership
- simulate payment attempt
- if payment succeeds, create reservation
- if payment fails, do not create reservation

## Scope

In scope:

- simulated payment inside `/checkout/confirm`
- clearer status semantics
- checkout tests for success/failure paths
- minimal backend-first changes

Out of scope:

- real payment provider integration
- separate `/checkout/pay` endpoint
- payment history table / `PaymentTransaction` entity
- broad reservation API redesign

## Endpoint Model

### POST `/checkout/lock`

Purpose:

- temporarily hold seats for a guest or authenticated user

Behavior:

- unchanged from current flow
- creates active `SeatLock` rows
- returns guest `sessionId` when relevant

### POST `/checkout/confirm`

Purpose:

- final checkout action
- acts as payment attempt + booking finalization

Behavior:

1. validate request identity
2. validate active lock ownership
3. validate lock not expired
4. simulate payment result
5. if payment succeeds:
   - create reservation
   - mark reservation as paid and confirmed
   - convert locks to reservation
6. if payment fails:
   - do not create reservation
   - keep lock active until expiry so user can retry

## State Semantics

### LockStatus

- `LOCKED`
  - seats are held
  - checkout can still be confirmed
- `PROCESSING`
  - internal transitional state during confirm
  - prevents concurrent finalize attempts
- `CONVERTED_TO_RESERVATION`
  - lock has been finalized into a reservation
- `EXPIRED`
  - lock released due to cancel/timeout

### ReservationStatus

Recommended v1 meaning:

- `CONFIRMED`
  - successful paid booking
- `CANCELLED`
  - booking cancelled
- `COMPLETED`
  - showtime has passed / booking fulfilled

`PENDING` should not be used for checkout-created reservations in v1 if reservation is only created after successful payment.

### PaymentStatus

- `PAID`
  - simulated payment succeeded
- `FAILED`
  - simulated payment failed
- `REFUNDED`
  - booking later refunded
- `PENDING`
  - avoid using this in finalized checkout flow unless there is a real async payment stage

## State Transitions

### Happy path

1. user locks seats
2. lock rows created with `LOCKED`
3. user confirms checkout
4. backend simulates payment success
5. reservation created with:
   - `ReservationStatus.CONFIRMED`
   - `PaymentStatus.PAID`
6. seat locks become `CONVERTED_TO_RESERVATION`

### Payment failure path

1. user locks seats
2. user confirms checkout
3. backend simulates payment failure
4. no reservation is created
5. lock remains `LOCKED`
6. user may retry confirm while lock is still valid

### Expiry path

1. user locks seats
2. lock expires before successful confirm
3. scheduler marks lock `EXPIRED`
4. confirm fails because no valid active lock exists

### Cancel lock path

1. user locks seats
2. user cancels lock before successful confirm
3. lock becomes `EXPIRED`
4. seats become available again

### Reservation cancel path

1. reservation already exists
2. user cancels reservation through reservation endpoint
3. reservation becomes `CANCELLED`
4. seats are returned to showtime inventory
5. payment refund behavior can be added later

## Confirm Endpoint Rules

### Success

Return:

- `reservationId`
- `bookingReference`
- `status = CONFIRMED`
- `paymentStatus = PAID`
- `totalPrice`
- `seatIds`
- success message

### Failure: payment failed

Return:

- no reservation created
- clear failure message such as `Payment failed`
- lock remains usable until expiry

### Failure: invalid lock

Return:

- conflict-style error
- clear message such as:
  - `No valid active lock found for seat X for this confirmation request`

## Fake Payment Design

For v1, payment is simulated inside `confirm`.

Possible request input options:

- `simulatePaymentSuccess: true|false`
- or `paymentMethodToken` with test values like:
  - `pm_success`
  - `pm_fail`

Recommendation:

- use a fake token field rather than a boolean
- it leaves the contract closer to future real payment integration

Example:

- `pm_success` => payment succeeds
- `pm_fail` => payment fails

## Persistence Strategy

For v1, do not add a new payment entity.

Use the existing checkout flow and reservation model:

- only create a `Reservation` after successful payment
- store final payment result on `Reservation`

No unpaid reservation rows should be created in this version.

## Important Consistency Rules

- `confirm` should not return “reservation confirmed” unless payment succeeded
- checkout-created reservations should not be saved as `PENDING/PENDING`
- reserved-seat queries should continue to rely on actual reservations plus active locks
- active seat locks remain the only temporary hold mechanism before booking exists

## Test Cases

Add integration coverage for:

- guest confirm succeeds when payment succeeds
- authenticated confirm succeeds when payment succeeds
- guest confirm fails when payment fails
- authenticated confirm fails when payment fails
- confirm fails with wrong guest email/session
- confirm fails after lock expiry
- no reservation created on failed payment
- lock remains retryable after failed payment
- seat cannot be relocked after successful confirm
- reservation cancel still works after successful paid booking

## Non-Goals For V1

Not doing yet:

- separate payment endpoint
- async payment workflow
- payment authorization/capture split
- webhook handling
- payment audit/history model
- refunds beyond status placeholders

## Implementation Direction

Minimal implementation path:

1. clarify enum semantics
2. extend confirm request/response for fake payment input/result
3. insert payment simulation into `CheckoutService.confirmCheckout`
4. on success, create reservation as `CONFIRMED + PAID`
5. on failure, return error without creating reservation
6. update tests
7. update docs/messages to match actual behavior
