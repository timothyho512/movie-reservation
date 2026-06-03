# Reservation History and Details API

## Goal

Expose finalized booking information to customers after Stripe Checkout creates a reservation.

Use this locally with the backend running at:

```text
http://localhost:8080
```

For a full local booking path, start the services in `docs/local-development.md`, complete checkout through Stripe, then use these reservation endpoints.

This feature completes the post-payment user journey:

1. customer locks seats
2. backend creates Stripe Checkout Session
3. Stripe webhook finalizes reservation
4. customer can view reservation history/details

The read API is intentionally ownership-based:

- authenticated users read reservations through their JWT principal
- guests read a reservation by booking reference plus guest email
- request-body `userId` is not used for reservation ownership

## Frontend-Facing Endpoints

### GET `/api/reservations`

Lists reservations for the authenticated user.

Authentication:

- required
- `Authorization: Bearer <jwt>`

Ownership:

- user id comes from the JWT principal
- returns only reservations where `reservation.user.id` matches the principal
- guest reservations are not returned from this endpoint

Response:

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
      "title": "Test Movie",
      "director": "Test Director"
    },
    "screen": {
      "id": 2,
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

Ordering:

- newest first by `bookingTime`

Failure:

- unauthenticated request returns `401 Unauthorized`

### GET `/api/reservations/{id}`

Returns one reservation detail for the authenticated user.

Authentication:

- required
- `Authorization: Bearer <jwt>`

Ownership:

- user id comes from the JWT principal
- reservation must belong to the authenticated user

Response:

```json
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
    "title": "Test Movie",
    "director": "Test Director"
  },
  "screen": {
    "id": 2,
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
```

Failure:

- unauthenticated request returns `401 Unauthorized`
- wrong authenticated owner returns conflict-style ownership error
- unknown reservation id returns not found

### GET `/api/reservations/reference/{reservationReference}?guestEmail=...`

Returns one guest reservation detail.

Authentication:

- not required

Ownership:

- booking reference identifies the reservation
- `guestEmail` must match the reservation guest email
- email comparison is case-insensitive

Example:

```http
GET /api/reservations/reference/BK1776170000000?guestEmail=guest@example.com
```

Response:

- same `ReservationResponse` shape as authenticated detail lookup

Failure:

- missing `guestEmail` returns validation error
- wrong guest email returns conflict-style ownership error
- unknown reservation reference returns not found

## Response DTO

All frontend-facing reservation read endpoints return `ReservationResponse`.

Top-level fields:

- `reservationId`
- `reservationReference`
- `reservationStatus`
- `paymentStatus`
- `showtime`
- `movie`
- `screen`
- `seats`
- `totalAmount`
- `currency`
- `createdAt`

Nested `showtime` fields:

- `id`
- `startTime`
- `endTime`

Nested `movie` fields:

- `id`
- `title`
- `director`

Nested `screen` fields:

- `id`
- `name`
- `screenType`

Nested `seats` fields:

- `id`
- `rowLabel`
- `seatNumber`
- `seatType`

Seat ordering:

- sorted by row label
- then seat number
- then seat id

## Security Contract

Security rules:

- `GET /api/reservations` is authenticated
- `GET /api/reservations/{id}` is authenticated
- `GET /api/reservations/reference/**` is public but requires `guestEmail`

The authenticated endpoints must not accept request-body or query-string `userId`.

The guest lookup endpoint is intentionally scoped to one reservation by booking reference. It does not provide a guest reservation list.

## Relationship To Checkout

The reservation read API does not create reservations.

Reservations are created by:

- Stripe `checkout.session.completed` webhook in the real payment flow
- legacy fake-payment `/checkout/confirm` in development/test scenarios only

The expected production path is:

```text
POST /checkout/lock
POST /checkout/session
Stripe hosted payment
POST /checkout/webhook/stripe
GET /checkout/session/{checkoutReference}
GET /api/reservations/{id}
```

After webhook finalization:

- reservation status is `CONFIRMED`
- payment status is `PAID`
- checkout session status is `FINALIZED`
- seat locks are `CONVERTED_TO_RESERVATION`

## Legacy / Non-Frontend Endpoints

The controller still contains older CRUD-style reservation endpoints:

- `POST /api/reservations`
- `PUT /api/reservations/{id}`
- `DELETE /api/reservations/{id}`
- `POST /api/reservations/{id}/cancel`

These are admin/internal endpoints, not the main frontend-facing
history/details API. Normal customer booking should not create or cancel
reservations through these routes.

For the current production-style checkout model, frontend reservation creation should happen through Stripe Checkout finalization, not direct reservation creation.

## Test Coverage

Current integration coverage includes:

- authenticated user can list own reservations
- authenticated user can read own reservation detail
- authenticated user cannot read another user's reservation
- guest can look up reservation by reference and email
- guest cannot look up reservation with the wrong email
- authenticated history/detail endpoints require authentication
