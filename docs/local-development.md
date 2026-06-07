# Local Development

This project runs the backend against local Postgres and Redis containers and uses Stripe CLI for local webhook delivery.

## First-time setup

Copy the example environment file and replace the Stripe placeholders:

```sh
cp .env.example .env
```

Required values:

```sh
SPRING_DATASOURCE_USERNAME=movie_reservation_user
SPRING_DATASOURCE_PASSWORD=movie_reservation_pass
SPRING_DATASOURCE_DB=movie_reservation_db

STRIPE_SECRET_KEY=sk_test_replace_me
STRIPE_WEBHOOK_SECRET=whsec_replace_me
```

`STRIPE_SECRET_KEY` comes from the Stripe Dashboard. `STRIPE_WEBHOOK_SECRET` comes from the Stripe CLI command in the webhook section below.

## Start local services

Docker Compose automatically reads `.env` for variable substitution.

```sh
docker compose up -d db test-db redis rabbitmq
```

The main development database is available at `localhost:5433`. The test database is available at `localhost:5434`. Redis is available at `localhost:6379`. RabbitMQ is available at `localhost:5672`, and the management UI is available at `http://localhost:15672`.

Redis stores active temporary seat holds and short-lived seat-map cache entries. Postgres remains the source of truth for confirmed reservations, checkout sessions, and audit/history rows.

The backend also runs a scheduled transactional outbox worker locally. It publishes due outbox events to RabbitMQ, where the first async worker logs booking email work without calling a real email provider.

The backend also runs two one-minute maintenance processes:

- checkout cleanup expires stale pending payments, closes their Stripe Checkout Sessions, releases locks, and retries pending refunds
- showtime lifecycle processing changes `UPCOMING` showtimes to `ONGOING`, changes ended showtimes to `COMPLETED`, and completes their paid confirmed reservations

## Booking Timing Configuration

Default local values:

```sh
SEAT_LOCK_TTL_SECONDS=1860
BOOKING_CUTOFF_MINUTES=10
```

`SEAT_LOCK_TTL_SECONDS` controls the active Redis hold and payment deadline.
The local checkout copies the earliest selected lock expiration, and Stripe
Checkout receives the same timestamp.

Stripe requires hosted Checkout expiration to be at least 30 minutes after the
Stripe Session is created. The current 31-minute default therefore only leaves
about one minute between locking seats and creating Stripe Checkout. For
realistic manual testing, set a longer value, for example:

```sh
SEAT_LOCK_TTL_SECONDS=3600
```

This keeps the implemented coordination behavior while providing enough time
to navigate from seat selection to Stripe.

`BOOKING_CUTOFF_MINUTES` closes new customer booking activity before the
showtime begins. With the default value, a 19:00 showtime stops accepting new
locks and checkouts at 18:50.

These variables are optional because the application has the defaults above.
Add them to `.env` when testing a different timing policy.

## Observability

Prometheus and Grafana are available through Docker Compose for local monitoring:

```sh
docker compose up -d prometheus grafana
```

Prometheus is available at `http://localhost:9090`. Grafana is available at `http://localhost:3001` with the default local credentials `admin` / `admin`, unless overridden with `GRAFANA_ADMIN_USER` and `GRAFANA_ADMIN_PASSWORD`.

Prometheus scrapes the backend at `host.docker.internal:8080/actuator/prometheus`, so keep the backend running on the host with the normal `./gradlew bootRun --args='--spring.profiles.active=dev'` command.

The backend exposes Actuator health and metrics at:

```text
http://localhost:8080/actuator/health
http://localhost:8080/actuator/prometheus
```

## Start the backend

Spring Boot does not automatically load `.env`, so export the file into the shell before running the backend:

```sh
set -a
source .env
set +a
./gradlew bootRun --args='--spring.profiles.active=dev'
```

The backend runs on `http://localhost:8080`.

## Demo data

When the backend starts with the `dev` profile, it seeds useful booking data if the demo customer does not already exist:

```text
Customer email: demo.customer@example.com
Admin email: demo.admin@example.com
Password: Password123!
```

The seed includes 3 movies, 2 theatres, 3 screens, 90 seats, and several upcoming showtimes. Seeded showtimes are generated relative to the current date so checkout can lock and book seats immediately.

If your local database already has older demo data and you want a clean reseed:

```sh
docker compose down -v
docker compose up -d db test-db redis rabbitmq
```

## Start Stripe webhook forwarding

In a separate terminal:

```sh
stripe login
stripe listen --forward-to localhost:8080/checkout/webhook/stripe
```

Copy the printed `whsec_...` value into `.env` as `STRIPE_WEBHOOK_SECRET`, then restart the backend shell command so Spring uses the updated value.

The `whsec_...` value is tied to the active Stripe CLI listener. If the CLI prints a new secret later, update `.env` and restart the backend.

## Run backend tests

Keep `test-db` and `redis` running, then run:

```sh
./gradlew test
```

## Frontend

Run the frontend from the frontend project directory:

```sh
cd frontend
npm run dev
```
