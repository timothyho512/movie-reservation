# Load Testing And Concurrency Testing

This project uses k6 for focused HTTP load tests against a running local backend.

The first committed scenario validates the most important booking invariant:

> When many clients try to lock the same seat for the same showtime at the same time, exactly one lock succeeds and every other attempt fails cleanly.

## Tooling

k6 is a load testing tool that runs JavaScript test scripts and sends real HTTP requests to the application.

Install k6 locally before running these scripts:

```sh
brew install k6
```

Or follow the official install guide if you are not using Homebrew:

```text
https://grafana.com/docs/k6/latest/set-up/install-k6/
```

## Local Setup

Start the required local services:

```sh
docker compose up -d db redis rabbitmq
```

Start the backend with the dev profile:

```sh
set -a
source .env
set +a
./gradlew bootRun --args='--spring.profiles.active=dev'
```

The dev profile seeds demo movies, showtimes, screens, and seats if the demo data is not already present. The k6 script discovers an upcoming showtime and available seat through the public API, so IDs do not need to be hardcoded.

## Same-Seat Lock Contention

Run:

```sh
k6 run load-tests/k6/same-seat-lock.js
```

Default behavior:

- `100` virtual users
- `100` total lock attempts
- every user targets the same `showtimeId` and `seatId`
- every user uses a unique guest email and unique `Idempotency-Key`
- expected result is `1` successful lock and `99` conflict responses

The script calls:

```text
GET /api/showtimes
GET /api/showtimes/{id}/seat-map
POST /checkout/lock
```

Expected summary:

```text
Same-seat lock contention test
================================
Expected: 1 success and 99 conflicts
Actual: 1 success, 99 conflicts, 0 unexpected statuses
Result: PASS
```

Observed local result:

```text
Selected showtimeId=1, seatId=1, movie="Inception", seat=A1
Expected: 1 success and 99 conflicts
Actual: 1 success, 99 conflicts, 0 unexpected statuses
Result: PASS
```

Useful overrides:

```sh
BASE_URL=http://localhost:8080 VUS=100 ITERATIONS=100 k6 run load-tests/k6/same-seat-lock.js
```

## Seat-Map Browsing Load

Run:

```sh
k6 run load-tests/k6/seat-map-browsing.js
```

Default behavior:

- `1000` virtual users
- `1000` total seat-map requests
- requests are spread across upcoming seeded showtimes
- every request calls `GET /api/showtimes/{id}/seat-map`
- expected result is `1000` successful responses and `0` unexpected statuses

The script validates that every seat-map response:

- returns `200 OK`
- includes the requested `showtimeId`
- includes at least one seat

Default latency thresholds:

- p95 response time below `500ms`
- p99 response time below `1000ms`

Expected summary:

```text
Seat-map browsing load test
===========================
Expected: 1000 successful seat-map responses and 0 unexpected statuses
Actual: 1000 successes, 0 unexpected statuses
Latency: avg=..., p95=..., p99=...
Result: PASS
```

Observed local result:

```text
Selected 6 upcoming showtimes for seat-map browsing load
Expected: 1000 successful seat-map responses and 0 unexpected statuses
Actual: 1000 successes, 0 unexpected statuses
Latency: avg=122.24ms, p95=283.97ms, p99=380.88ms
Result: PASS
```

Useful overrides:

```sh
BASE_URL=http://localhost:8080 VUS=1000 ITERATIONS=1000 P95_THRESHOLD_MS=500 P99_THRESHOLD_MS=1000 k6 run load-tests/k6/seat-map-browsing.js
```

## Verification

The k6 thresholds fail the run unless:

- exactly one request returns `200 OK`
- all remaining lock attempts return `409 Conflict`
- no unexpected response statuses occur
- every response-specific check passes

Optional database verification:

```sh
docker exec -it movie-reservation-db psql \
  -U movie_reservation_user \
  -d movie_reservation_db
```

Then inspect the selected seat/showtime pair from the k6 output:

```sql
SELECT seat_id, showtime_id, status, count(*)
FROM seat_locks
GROUP BY seat_id, showtime_id, status
ORDER BY showtime_id, seat_id, status;
```

The lock-only test should not create reservations. Reservation creation belongs to the payment webhook flow and will be tested in a later checkout/session/webhook scenario.

## CV Result

A concise project bullet from this milestone:

```text
Validated concurrency-safe seat holds with k6, proving only one user can lock the same seat/showtime under 100 concurrent booking attempts while competing requests fail cleanly with 409 Conflict.
```

```text
Load-tested the seat-map browsing path with k6, validating 1000 concurrent read requests with zero unexpected responses and documented p95/p99 latency.
```
