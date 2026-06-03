# Backend API Contract

This document is the canonical frontend-facing API contract for the current monolith.
It describes the routes that should remain stable while the backend is being
stabilized. Admin/internal CRUD routes are listed separately because they are not
the primary frontend booking flows yet.

Base URL in local development:

```text
http://localhost:8080
```

All request and response bodies are JSON unless noted otherwise. Authenticated
requests use:

```http
Authorization: Bearer <jwt>
```

Date-time values are serialized as local ISO-8601 strings, for example:

```text
2026-04-20T18:30:00
```

Clients should use structured fields such as `status`, `reservationId`, and
`bookingReference` for control flow. The `message` field is human-readable and
should not be used for branching logic.

## Error Shape

Most application errors return this shape:

```json
{
  "timestamp": "2026-05-26T21:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Reservation not found with id: 42",
  "path": "/api/reservations/42"
}
```

Common status codes:

| Status | Meaning |
| --- | --- |
| `400` | Invalid or incomplete request |
| `401` | Missing, invalid, or expired authentication |
| `404` | Requested resource does not exist |
| `409` | Business conflict, such as unavailable seats or ownership mismatch |
| `410` | Checkout/session expired |
| `500` | Unexpected server error |

## Auth

### POST `/api/auth/register`

Creates a customer account and returns a JWT.

Auth: public

Request:

```json
{
  "firstName": "Jay",
  "lastName": "Doe",
  "email": "jay@example.com",
  "password": "password123",
  "phoneNumber": "07123456789"
}
```

Success: `201 Created`

```json
{
  "token": "jwt-token",
  "user": {
    "id": 1,
    "firstName": "Jay",
    "lastName": "Doe",
    "email": "jay@example.com",
    "phoneNumber": "07123456789",
    "role": "CUSTOMER",
    "active": true
  }
}
```

### POST `/api/auth/login`

Authenticates an existing user and returns a JWT.

Auth: public

Request:

```json
{
  "email": "jay@example.com",
  "password": "password123"
}
```

Success: `200 OK`

Response shape is the same as register.

### GET `/api/auth/me`

Returns the current authenticated user.

Auth: required

Success: `200 OK`

```json
{
  "id": 1,
  "firstName": "Jay",
  "lastName": "Doe",
  "email": "jay@example.com",
  "phoneNumber": "07123456789",
  "role": "CUSTOMER",
  "active": true
}
```

## Movies

### GET `/api/movies`

Returns movie cards for browsing.

Auth: public

Success: `200 OK`

```json
[
  {
    "id": 1,
    "title": "Inception",
    "director": "Christopher Nolan"
  }
]
```

### GET `/api/movies/{id}`

Returns movie detail with showtime summaries.

Auth: public

Success: `200 OK`

```json
{
  "id": 1,
  "title": "Inception",
  "director": "Christopher Nolan",
  "showtimes": [
    {
      "id": 10,
      "movie": {
        "id": 1,
        "title": "Inception",
        "director": "Christopher Nolan"
      },
      "theatre": {
        "id": 2,
        "name": "Imperial Cinema",
        "city": "London",
        "country": "UK"
      },
      "screen": {
        "id": 3,
        "name": "Screen 1",
        "screenType": "STANDARD"
      },
      "startTime": "2026-04-20T18:30:00",
      "endTime": "2026-04-20T20:45:00",
      "basePrice": 12.50,
      "availableSeats": 48,
      "totalSeats": 50,
      "status": "UPCOMING"
    }
  ]
}
```

## Theatres

### GET `/api/theatres`

Returns theatre summaries for browsing.

Auth: public

Success: `200 OK`

```json
[
  {
    "id": 2,
    "name": "Imperial Cinema",
    "address": "1 Exhibition Road",
    "city": "London",
    "state": "England",
    "country": "UK",
    "postalCode": "SW7 2AZ",
    "phoneNumber": "02000000000",
    "totalScreens": 3,
    "totalSeats": 150,
    "active": true
  }
]
```

### GET `/api/theatres/{id}`

Returns theatre detail with screen summaries.

Auth: public

Success: `200 OK`

```json
{
  "id": 2,
  "name": "Imperial Cinema",
  "address": "1 Exhibition Road",
  "city": "London",
  "state": "England",
  "country": "UK",
  "postalCode": "SW7 2AZ",
  "phoneNumber": "02000000000",
  "totalScreens": 3,
  "totalSeats": 150,
  "active": true,
  "screens": [
    {
      "id": 3,
      "name": "Screen 1",
      "totalSeats": 50,
      "screenType": "STANDARD",
      "active": true
    }
  ]
}
```

## Showtimes

### GET `/api/showtimes`

Returns showtimes ordered by start time ascending.

Auth: public

Success: `200 OK`

Response item shape is `ShowtimeSummaryResponse`:

```json
[
  {
    "id": 10,
    "movie": {
      "id": 1,
      "title": "Inception",
      "director": "Christopher Nolan"
    },
    "theatre": {
      "id": 2,
      "name": "Imperial Cinema",
      "city": "London",
      "country": "UK"
    },
    "screen": {
      "id": 3,
      "name": "Screen 1",
      "screenType": "STANDARD"
    },
    "startTime": "2026-04-20T18:30:00",
    "endTime": "2026-04-20T20:45:00",
    "basePrice": 12.50,
    "availableSeats": 48,
    "totalSeats": 50,
    "status": "UPCOMING"
  }
]
```

### GET `/api/showtimes/{id}`

Returns one showtime summary.

Auth: public

Success: `200 OK`

Response shape is one `ShowtimeSummaryResponse`.

### GET `/api/showtimes/{id}/seat-map`

Returns all seats for the showtime screen with current availability.

Auth: public

Availability is `false` when a seat is already reserved or actively locked.

Success: `200 OK`

```json
{
  "showtimeId": 10,
  "showtimeStatus": "UPCOMING",
  "startTime": "2026-04-20T18:30:00",
  "endTime": "2026-04-20T20:45:00",
  "movie": {
    "id": 1,
    "title": "Inception",
    "director": "Christopher Nolan"
  },
  "screen": {
    "id": 3,
    "name": "Screen 1",
    "screenType": "STANDARD"
  },
  "seats": [
    {
      "id": 100,
      "rowLabel": "A",
      "seatNumber": 1,
      "seatType": "REGULAR",
      "price": 12.50,
      "available": true
    }
  ]
}
```

### GET `/api/showtimes/{id}/available-seats`

Legacy availability endpoint.

Auth: public

Prefer `/api/showtimes/{id}/seat-map` for frontend seat selection because it
includes seat labels, type, price, and availability in one response.

Success: `200 OK`

```json
{
  "showtimeId": 10,
  "seats": [
    {
      "seatId": 100,
      "available": true
    }
  ]
}
```

## Checkout

Checkout supports both authenticated and guest ownership.

Authenticated requests:

- include `Authorization: Bearer <jwt>`
- ownership comes from the JWT principal
- guest identity fields should not be used for ownership

Guest requests:

- omit `Authorization`
- include `guestEmail`
- include `sessionId` where required after lock creation

### POST `/checkout/lock`

Temporarily locks seats in Redis before payment. Postgres `seat_locks` rows are kept as audit/history records only; Redis is the active hold authority.

Auth: optional

Optional header:

```http
Idempotency-Key: lock-attempt-abc-123
```

When provided, retries for the same owner and same lock request return the
original lock response, including the guest `sessionId`. Reusing the same key
with different showtime, seats, or owner identity returns `409 Conflict`. If the
stored lock attempt has expired, retrying the key returns `410 Gone`.

Request:

```json
{
  "showtimeId": 10,
  "seatIds": [100, 101],
  "guestEmail": "guest@example.com"
}
```

For authenticated checkout, omit `guestEmail`.

Success: `200 OK`

```json
{
  "sessionId": "guest-session-id",
  "expiresAt": "2026-04-20T18:15:00",
  "lockedSeatIds": [100, 101],
  "message": "Seats locked successfully"
}
```

### POST `/checkout/session`

Creates a Stripe Checkout Session for the selected locked seats. This is the
canonical real-payment entry point.

All requested seats must still have active Redis holds owned by the caller.

Auth: optional

Optional header:

```http
Idempotency-Key: checkout-attempt-abc-123
```

When provided, retries for the same owner and same request body return the
original checkout session response instead of creating another Stripe Checkout
Session. Reusing the same key with different showtime, seats, or owner identity
returns `409 Conflict`.

Request:

```json
{
  "showtimeId": 10,
  "seatIds": [100, 101],
  "guestEmail": "guest@example.com",
  "sessionId": "guest-session-id"
}
```

For authenticated checkout, omit `guestEmail` and `sessionId`.

Success: `200 OK`

```json
{
  "checkoutReference": "chk_4b5f8c2e8f944d85a03d1d2f8a9e1b11",
  "stripeCheckoutSessionId": "cs_test_123",
  "checkoutUrl": "https://checkout.stripe.com/c/pay/cs_test_123",
  "status": "PENDING_PAYMENT",
  "expiresAt": "2026-04-20T18:15:00",
  "message": "Checkout session created successfully"
}
```

### GET `/checkout/session/{checkoutReference}`

Returns checkout status after redirecting back from Stripe.

Auth: optional

The backend uses the authenticated principal when provided. Guest status lookup
requires the checkout reference plus the guest identity that created the session.

Guest query parameters:

| Query param | Required | Notes |
| --- | ---: | --- |
| `guestEmail` | yes | Must match the checkout session guest email |
| `sessionId` | yes | Must match the guest seat-lock session id |

Example:

```http
GET /checkout/session/chk_abc123?guestEmail=guest@example.com&sessionId=guest-session-id
```

Success: `200 OK`

```json
{
  "checkoutReference": "chk_4b5f8c2e8f944d85a03d1d2f8a9e1b11",
  "status": "FINALIZED",
  "reservationId": 42,
  "bookingReference": "BK1776170000000",
  "message": "Checkout session finalized successfully"
}
```

### POST `/checkout/webhook/stripe`

Receives Stripe webhook events.

Auth: public, verified by `Stripe-Signature`

Headers:

```http
Stripe-Signature: t=...,v1=...
```

Body: raw Stripe webhook payload.

Success: `200 OK` with empty body.

Expected production behavior:

- `checkout.session.completed` finalizes the checkout, creates the reservation, and releases Redis holds
- `checkout.session.expired` expires the checkout and releases Redis holds
- duplicate webhooks should not create duplicate reservations

### POST `/checkout/cancel`

Cancels active Redis seat holds before reservation creation.

Auth: optional

Request:

```json
{
  "showtimeId": 10,
  "seatIds": [100, 101],
  "guestEmail": "guest@example.com",
  "sessionId": "guest-session-id"
}
```

Success: `200 OK`

```json
{
  "message": "Locks cancelled successfully"
}
```

### POST `/checkout/confirm`

Legacy fake-payment endpoint retained for development/tests.

Auth: optional

Production checkout should use `/checkout/session` and Stripe webhook
finalization instead.

Request:

```json
{
  "showtimeId": 10,
  "seatIds": [100, 101],
  "guestEmail": "guest@example.com",
  "sessionId": "guest-session-id",
  "paymentMethodToken": "pm_success"
}
```

Success: `200 OK`

```json
{
  "reservationId": 42,
  "bookingReference": "BK1776170000000",
  "status": "CONFIRMED",
  "paymentStatus": "PAID",
  "totalPrice": 25.00,
  "seatIds": [100, 101],
  "message": "Reservation confirmed successfully"
}
```

## Reservations

### GET `/api/reservations`

Lists reservations for the authenticated user.

Auth: required

Ownership:

- user id comes from the JWT principal
- guest reservations are not returned here
- ordered newest first by booking time

Success: `200 OK`

```json
[
  {
    "reservationId": 42,
    "reservationReference": "BK1776170000000",
    "reservationStatus": "CONFIRMED",
    "paymentStatus": "PAID",
    "showtime": {
      "id": 10,
      "startTime": "2026-04-20T18:30:00",
      "endTime": "2026-04-20T20:45:00"
    },
    "movie": {
      "id": 1,
      "title": "Inception",
      "director": "Christopher Nolan"
    },
    "screen": {
      "id": 3,
      "name": "Screen 1",
      "screenType": "STANDARD"
    },
    "seats": [
      {
        "id": 100,
        "rowLabel": "A",
        "seatNumber": 1,
        "seatType": "REGULAR"
      }
    ],
    "totalAmount": 12.50,
    "currency": "GBP",
    "createdAt": "2026-04-15T10:00:00"
  }
]
```

### GET `/api/reservations/{id}`

Returns one reservation for the authenticated user.

Auth: required

Ownership:

- reservation must belong to the authenticated user
- wrong owner returns a conflict-style ownership error

Success: `200 OK`

Response shape is one `ReservationResponse`.

### GET `/api/reservations/reference/{reservationReference}?guestEmail={email}`

Returns one guest reservation by booking reference and email.

Auth: public

Ownership:

- `guestEmail` is required
- email comparison is case-insensitive
- this endpoint does not expose a guest reservation list

Success: `200 OK`

Response shape is one `ReservationResponse`.

### POST `/api/reservations/{id}/cancel`

Cancels an existing reservation through an admin/internal workflow.

Auth: admin or manager JWT required

Customer and unauthenticated guest cancellation is intentionally disabled at
the HTTP security layer for now. Admin/manager users can cancel any reservation
without sending guest ownership fields.

Request:

```http
POST /api/reservations/42/cancel
Authorization: Bearer <admin-or-manager-jwt>
```

Send an empty body. `guestEmail` is rejected for authenticated requests.

Success: `200 OK`

```json
{
  "reservationId": 42,
  "status": "CANCELLED",
  "message": "Reservation cancelled successfully"
}
```

## Internal CRUD Routes

These routes exist in the monolith but are admin/internal rather than primary
frontend booking flows. They are protected by explicit `ADMIN`/`MANAGER`
authorization for mutating operations, and their response bodies are DTO-based
so they do not expose raw JPA entity graphs.

| Method | Path | Current note |
| --- | --- | --- |
| `POST` | `/api/movies` | Admin/manager only. Creates movie, returns `MovieDetailResponse` |
| `PUT` | `/api/movies/{id}` | Admin/manager only. Updates movie, returns `MovieDetailResponse` |
| `DELETE` | `/api/movies/{id}` | Admin/manager only. Deletes movie |
| `POST` | `/api/theatres` | Admin/manager only. Creates theatre, returns `TheatreDetailResponse` |
| `PUT` | `/api/theatres/{id}` | Admin/manager only. Updates theatre, returns `TheatreDetailResponse` |
| `DELETE` | `/api/theatres/{id}` | Admin/manager only. Deletes theatre |
| `GET` | `/api/screens` | Public. Returns `ScreenResponse` list |
| `GET` | `/api/screens/{id}` | Public. Returns `ScreenResponse` |
| `POST` | `/api/screens` | Admin/manager only. Creates screen, returns `ScreenResponse` |
| `PUT` | `/api/screens/{id}` | Admin/manager only. Updates screen, returns `ScreenResponse` |
| `DELETE` | `/api/screens/{id}` | Admin/manager only. Deletes screen |
| `GET` | `/api/seats` | Public. Returns `SeatResponse` list |
| `GET` | `/api/seats/{id}` | Public. Returns `SeatResponse` |
| `POST` | `/api/seats` | Admin/manager only. Creates seat, returns `SeatResponse` |
| `PUT` | `/api/seats/{id}` | Admin/manager only. Updates seat, returns `SeatResponse` |
| `DELETE` | `/api/seats/{id}` | Admin/manager only. Deletes seat |
| `POST` | `/api/showtimes` | Admin/manager only. Creates showtime, returns `ShowtimeSummaryResponse` |
| `PUT` | `/api/showtimes/{id}` | Admin/manager only. Updates showtime, returns `ShowtimeSummaryResponse` |
| `DELETE` | `/api/showtimes/{id}` | Admin/manager only. Deletes showtime |
| `POST` | `/api/reservations` | Admin/manager only. Legacy direct reservation creation, returns `ReservationResponse` |
| `PUT` | `/api/reservations/{id}` | Admin/manager only. Legacy direct reservation update, returns `ReservationResponse` |
| `DELETE` | `/api/reservations/{id}` | Admin/manager only. Deletes reservation |
| `GET` | `/api/users` | Admin/manager only. Returns safe user DTO list |
| `GET` | `/api/users/{id}` | Admin/manager only. Returns safe user DTO |
| `POST` | `/api/users` | Admin/manager only. Creates user and returns safe user DTO |
| `PUT` | `/api/users/{id}` | Admin/manager only. Updates user and returns safe user DTO |
| `DELETE` | `/api/users/{id}` | Admin/manager only. Deletes user |

## Enums Used In Responses

Known enum values from the current model:

| Enum | Values |
| --- | --- |
| `CheckoutSessionStatus` | `PENDING_PAYMENT`, `PAID`, `FAILED`, `CANCELLED`, `EXPIRED`, `FINALIZED` |
| `CurrencyCode` | `GBP` |
| `LockStatus` | `LOCKED`, `PROCESSING`, `EXPIRED`, `CONVERTED_TO_RESERVATION` |
| `PaymentStatus` | `PENDING`, `PAID`, `FAILED`, `REFUNDED` |
| `ReservationStatus` | `PENDING`, `CONFIRMED`, `CANCELLED`, `COMPLETED` |
| `ScreenType` | `STANDARD`, `IMAX`, `DOLBY_ATMOS`, `THREE_D`, `FOUR_DX`, `VIP` |
| `SeatType` | `REGULAR`, `VIP`, `WHEELCHAIR` |
| `ShowtimeStatus` | `UPCOMING`, `ONGOING`, `COMPLETED`, `CANCELLED` |
| `UserRole` | `CUSTOMER`, `ADMIN`, `MANAGER` |

## Stabilization Notes

The frontend-facing contract should stay DTO-based:

- no raw JPA entities on public browsing endpoints
- no password fields in user/auth responses
- reservation read ownership must come from JWT or guest reference/email
- `/api/showtimes/{id}/seat-map` is the preferred seat selection endpoint
- real payments should use `/checkout/session` plus `/checkout/webhook/stripe`

Current follow-up candidates:

- move internal CRUD routes behind explicit admin authorization
- add contract integration tests for every route in this document
