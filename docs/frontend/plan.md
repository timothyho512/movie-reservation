# Movie Reservation Frontend Plan

## Context

The backend is a complete Spring Boot 4.0 REST API with JWT auth, Stripe hosted checkout, seat locking, and guest checkout support. The goal is to build a polished Next.js frontend covering browsing movies/showtimes, selecting seats, completing payment via Stripe, and viewing bookings.

Frontend-readiness APIs have been added to the backend — the Next.js app should use these clean DTOs instead of raw entity endpoints.

---

## Why Next.js

1. **SSR for SEO** — `/movies` and `/movies/[id]` contain indexable content. Server Components call the browse DTOs at render time so HTML is populated before JS hydration.
2. **App Router nested layouts** — the checkout funnel needs a stripped nav layout. Route groups (`(checkout)`) give a separate `layout.tsx` per segment.
3. **httpOnly cookie auth** — the JWT is stored in an `httpOnly` cookie, so it is intentionally invisible to JavaScript. The `authStore` holds only the non-sensitive `user` object for display. Next.js Route Handlers (`app/api/proxy/`) run server-side, read the cookie via `cookies()` from `next/headers`, and inject `Authorization: Bearer <token>` before forwarding to the Spring Boot backend — because the browser never exposes the cookie value to client JS.
4. **Static generation for theatres** — `fetch(..., { next: { revalidate: 300 } })` for infrequently-updated theatre listings.
5. **Stripe redirect handling** — Stripe does a full browser navigation to `/checkout/success?checkoutReference=...`. A Next.js page is the natural landing target for the polling loop.
6. **TanStack Query polling** — `refetchInterval: 2000` until checkout status is `FINALIZED` with zero boilerplate.
7. **TypeScript** — mirrors backend DTOs exactly for end-to-end type safety.

---

## Tech Stack

| Concern      | Choice                                                                                                                |
| ------------ | --------------------------------------------------------------------------------------------------------------------- |
| Framework    | Next.js 15 (App Router)                                                                                               |
| Language     | TypeScript                                                                                                            |
| Styling      | Tailwind CSS + shadcn/ui                                                                                              |
| State        | Zustand — `authStore` (user object only) + `checkoutStore` (selectedSeatIds, sessionId, expiresAt, checkoutReference) |
| Server state | TanStack Query v5 — checkout status polling, seat availability                                                        |
| Forms        | React Hook Form + Zod                                                                                                 |
| HTTP         | Native `fetch` + typed `apiFetch<T>` wrapper                                                                          |

---

## Backend API Reference (Updated DTOs)

**Public browse endpoints** return clean DTOs (not raw JPA entities):

| Endpoint                               | Response DTO                                                                                 |
| -------------------------------------- | -------------------------------------------------------------------------------------------- |
| `GET /api/movies`                      | `MovieCardResponse[]`                                                                        |
| `GET /api/movies/{id}`                 | `MovieDetailResponse` (includes showtimes)                                                   |
| `GET /api/showtimes`                   | `ShowtimeSummaryResponse[]`                                                                  |
| `GET /api/showtimes/{id}`              | `ShowtimeSummaryResponse`                                                                    |
| `GET /api/theatres`                    | `TheatreSummaryResponse[]`                                                                   |
| `GET /api/theatres/{id}`               | `TheatreDetailResponse` (includes screens)                                                   |
| **`GET /api/showtimes/{id}/seat-map`** | Combined payload: showtime + movie summary + screen summary + sorted seats with availability |

The seat-map endpoint is the primary endpoint for the seat selection page — it provides everything in one call: showtime info, movie summary, screen summary, and each seat's `rowLabel`, `seatNumber`, `seatType`, `price`, and `available` (accounting for active locks and reservations).

**Checkout flow endpoints** (unchanged):

```
POST /checkout/lock              → { sessionId, expiresAt, lockedSeatIds }
POST /checkout/session           → { checkoutReference, checkoutUrl, status, expiresAt }
GET  /checkout/session/{ref}     → { checkoutReference, status, reservationId, bookingReference }
```

**Auth endpoints:**

```
POST /api/auth/register   → { token, user }
POST /api/auth/login      → { token, user }
GET  /api/auth/me         → AuthUserResponse
```

**Reservations (read-only for customer):**

```
GET /api/reservations/{id}  → Reservation (for booking confirmation display)
```

---

## Project Structure

```
movie-reservation-frontend/
├── app/
│   ├── (marketing)/             # Public pages — NavBar + Footer layout
│   │   ├── layout.tsx
│   │   ├── page.tsx             # / — hero + featured movies + upcoming showtimes
│   │   ├── movies/
│   │   │   ├── page.tsx         # /movies — SSR movie grid
│   │   │   └── [id]/page.tsx    # /movies/[id] — SSR detail + ShowtimePicker
│   │   ├── theatres/
│   │   │   ├── page.tsx         # /theatres — SSG list (revalidate 300s)
│   │   │   └── [id]/page.tsx    # /theatres/[id] — theatre + screens
│   │   └── showtimes/
│   │       └── [id]/page.tsx    # /showtimes/[id] — showtime info
│   ├── (auth)/                  # Centred card layout, no nav
│   │   ├── layout.tsx
│   │   ├── login/page.tsx
│   │   └── register/page.tsx
│   ├── (checkout)/              # Stripped nav — logo only
│   │   ├── layout.tsx
│   │   ├── showtimes/[id]/seats/page.tsx   # Seat map (CSR)
│   │   ├── checkout/success/page.tsx        # Payment polling
│   │   └── checkout/cancel/page.tsx         # Abandoned payment recovery
│   ├── (account)/               # Protected — requires jwt cookie
│   │   ├── layout.tsx
│   │   └── account/
│   │       ├── page.tsx                     # /account — profile (GET /api/auth/me)
│   │       └── bookings/
│   │           ├── page.tsx                 # /account/bookings — reservation list
│   │           └── [id]/page.tsx            # /account/bookings/[id] — booking detail
│   ├── api/
│   │   ├── auth/login/route.ts              # Forward to backend, set httpOnly cookie
│   │   ├── auth/register/route.ts           # Forward to backend, set httpOnly cookie
│   │   ├── auth/logout/route.ts             # Clear cookie
│   │   └── proxy/[...path]/route.ts         # Catch-all: read cookie, inject Authorization header
│   ├── layout.tsx                           # Root: QueryClientProvider, Toaster
│   └── globals.css
├── components/
│   ├── ui/                      # shadcn/ui primitives
│   ├── layout/                  # NavBar, Footer
│   ├── movies/                  # MovieCard, MovieGrid, ShowtimePicker
│   ├── theatres/                # TheatreCard, ScreenBadge
│   ├── seats/                   # SeatMap, SeatButton, SeatLegend, SeatSummaryPanel, LockCountdown
│   ├── checkout/                # GuestEmailForm, CheckoutSummary, PaymentPolling, BookingConfirmation
│   └── reservations/            # ReservationCard, ReservationStatusBadge
├── lib/
│   ├── api-client.ts            # apiFetch<T> + ApiError
│   └── api/
│       ├── movies.ts            # getMovies(), getMovie(id)
│       ├── theatres.ts          # getTheatres(), getTheatre(id)
│       ├── showtimes.ts         # getShowtimes(), getShowtime(id), getSeatMap(id)
│       ├── checkout.ts          # lockSeats(), createCheckoutSession(), getCheckoutStatus()
│       └── reservations.ts      # getReservations(), getReservation(id)
├── stores/
│   ├── auth-store.ts            # { user: AuthUserResponse | null, setUser, clearUser }
│   └── checkout-store.ts        # { showtimeId, selectedSeatIds, sessionId, expiresAt, checkoutReference }
├── types/
│   └── api.ts                   # All backend DTOs as TypeScript interfaces
├── hooks/
│   ├── useAuth.ts               # Reads authStore
│   ├── useSeatSelection.ts      # Set<number> toggle logic
│   ├── useCheckoutPolling.ts    # TanStack Query poll until terminal status
│   └── useLockCountdown.ts      # MM:SS countdown from expiresAt
└── middleware.ts                 # Edge: redirect /account/* if no jwt cookie
```

---

## Pages & Routes

| URL                      | Render                | Auth          | Key API                                        |
| ------------------------ | --------------------- | ------------- | ---------------------------------------------- |
| `/`                      | SSR                   | No            | `GET /api/movies`, `GET /api/showtimes`        |
| `/movies`                | SSR                   | No            | `GET /api/movies` → `MovieCardResponse[]`      |
| `/movies/[id]`           | SSR                   | No            | `GET /api/movies/{id}` → `MovieDetailResponse` |
| `/theatres`              | SSG (revalidate 300s) | No            | `GET /api/theatres`                            |
| `/theatres/[id]`         | SSG (revalidate 300s) | No            | `GET /api/theatres/{id}`                       |
| `/showtimes/[id]`        | SSR                   | No            | `GET /api/showtimes/{id}`                      |
| `/showtimes/[id]/seats`  | CSR                   | No (guest ok) | `GET /api/showtimes/{id}/seat-map`             |
| `/login`                 | CSR                   | No            | `POST /api/auth/login` (via Route Handler)     |
| `/register`              | CSR                   | No            | `POST /api/auth/register` (via Route Handler)  |
| `/checkout/success`      | CSR                   | No            | `GET /checkout/session/{ref}` (polling)        |
| `/checkout/cancel`       | CSR                   | No            | —                                              |
| `/account`               | SSR                   | Yes           | `GET /api/auth/me`                             |
| `/account/bookings`      | CSR                   | Yes           | `GET /api/reservations`                        |
| `/account/bookings/[id]` | CSR                   | Yes           | `GET /api/reservations/{id}`                   |

---

## Checkout Flow (UI State Machine)

```
IDLE
  │ user selects seat on /showtimes/[id]/seats
  ▼
SELECTING
  │ clicks "Continue" (+ guestEmail if not logged in)
  │ POST /checkout/lock (via proxy) → { sessionId, expiresAt }
  ▼
LOCKED  (15-min LockCountdown visible)
  │ clicks "Pay Now"
  │ POST /checkout/session (via proxy) → { checkoutReference, checkoutUrl }
  │ window.location.href = checkoutUrl  ← full navigation to Stripe
  ▼
[Stripe hosted checkout]
  │ success  →  /checkout/success?checkoutReference=chk_...
  │ cancel   →  /checkout/cancel?checkoutReference=chk_...
  ▼
AWAITING_PAYMENT  (poll GET /checkout/session/{ref} every 2s)
  │ status === FINALIZED
  ▼
CONFIRMED  → BookingConfirmation with bookingReference + "View My Bookings" CTA

Countdown expires → clear checkoutStore, toast, redirect to /showtimes/[id]/seats
```

**Key notes:**

- `GET /api/showtimes/{id}/seat-map` is the single call for the seat selection page — no need to stitch separate seat/availability calls
- Checkout `status` terminal states: `FINALIZED`, `EXPIRED`, `FAILED`, `CANCELLED` — polling stops on any of these
- `checkoutStore` is in-memory only (no persistence) — closing the tab abandons the flow; locks expire after 15 mins via backend scheduler
- `authStore` holds only `user` (not the token) — the token lives exclusively in the httpOnly cookie; Route Handlers read it server-side for proxied requests

---

## Auth Strategy

- `POST /api/auth/login/register` flow: browser POSTs to Next.js Route Handler → Route Handler forwards to Spring Boot → receives `{ token, user }` → sets `httpOnly; SameSite=Lax` cookie with `token`, returns only `user` to client → client stores `user` in `authStore`
- **Token is never exposed to client JS** (httpOnly cookie). The `authStore` never holds the token.
- Authenticated API calls from client components go through `app/api/proxy/[...path]/route.ts` — the Route Handler reads the cookie with `cookies().get('jwt')` server-side and adds `Authorization: Bearer <token>` before forwarding to `http://localhost:8080`
- Server Components call `cookies()` from `next/headers` directly for SSR data fetching
- `middleware.ts` on the Edge protects `/account/*`: redirects to `/login?redirectTo=...` if cookie absent or JWT `exp` is past (checked via `jose`)
- No refresh token exists — on session expiry the middleware redirect handles UX cleanly

---

## Implementation Phases

| Phase | What                                                                                                                                      |
| ----- | ----------------------------------------------------------------------------------------------------------------------------------------- |
| 1     | Project setup: Next.js 15, TypeScript, Tailwind, shadcn/ui init, `types/api.ts`, `lib/api-client.ts`, `lib/api/*.ts`, stores, root layout |
| 2     | Public browse: `/movies`, `/movies/[id]`, `/theatres`, `/theatres/[id]`, `/showtimes/[id]`, homepage                                      |
| 3     | Auth: Route Handlers for login/register/logout, `/login`, `/register`, middleware, NavBar wiring                                          |
| 4     | Seat selection: `/showtimes/[id]/seats`, SeatMap, SeatButton, SeatSummaryPanel, GuestEmailForm, lock call, LockCountdown                  |
| 5     | Checkout: CheckoutSummary, Stripe redirect, `/checkout/success` polling, BookingConfirmation, `/checkout/cancel`                          |
| 6     | Account: `/account`, `/account/bookings`, `/account/bookings/[id]`                                                                        |
| 7     | Polish: skeletons, error boundaries, toasts, a11y (keyboard nav on SeatMap, WCAG contrast)                                                |

---

## Verification

End-to-end test after each phase:

1. `/movies` → click a movie → ShowtimePicker groups showtimes by date
2. Click a showtime → `/showtimes/[id]/seats` → single API call to seat-map endpoint, grid renders with available/locked states
3. Select seats → "Continue" → lock confirmed → 15-min countdown starts
4. "Pay Now" → Stripe sandbox → Stripe test card `4242 4242 4242 4242`
5. Redirect to `/checkout/success` → polling detects `FINALIZED` → bookingReference shown
6. Log in → `/account/bookings` → reservation appears with `CONFIRMED` status

Backend URL: `http://localhost:8080`
