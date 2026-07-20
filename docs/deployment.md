# Free-Tier Deployment

This deployment uses:

- Render Free for the Spring Boot API
- Neon Free for PostgreSQL
- Upstash Free for Redis
- CloudAMQP Little Lemur for RabbitMQ
- Vercel Hobby for the Next.js frontend
- Stripe test mode for payments

The backend sleeps after a period without incoming traffic on Render Free. The
first request after sleeping can take about a minute, and scheduled backend work
does not run while the service is asleep. This is acceptable for a portfolio
demo, but not for a production reservation business.

## What the Deployment Files Do

- `Dockerfile` builds the Spring Boot JAR with Java 21 and runs it in a smaller
  Java runtime image.
- `.dockerignore` prevents local files, secrets, and the frontend from being
  copied into the backend Docker build.
- `render.yaml` asks Render to create one free Docker web service and lists the
  required environment variables.
- `application-prod.properties` activates hosted connection URLs, uses a small
  database connection pool, and configures Render's port and health check.
- `frontend/.env.example` documents the backend URL that Vercel needs.

## 1. Create the Managed Services

Create accounts and free resources in this order:

1. Create a Neon PostgreSQL project in a European region.
2. Create an Upstash Redis database in a nearby European region.
3. Create a CloudAMQP Little Lemur instance in a nearby European region.

Keep each provider's connection details private. They belong in Render's
environment settings, not in Git.

## 2. Collect the Backend Values

Render will ask for the following values when creating the Blueprint:

| Render variable | Value |
| --- | --- |
| `SPRING_DATASOURCE_URL` | Neon JDBC URL, beginning with `jdbc:postgresql://` and using `sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | Neon database username |
| `SPRING_DATASOURCE_PASSWORD` | Neon database password |
| `REDIS_URL` | Upstash TLS URL beginning with `rediss://` |
| `RABBITMQ_URL` | CloudAMQP URL beginning with `amqps://` |
| `CORS_ALLOWED_ORIGINS` | The Vercel frontend URL; use a temporary placeholder until Vercel is deployed |
| `STRIPE_SECRET_KEY` | Stripe test secret key beginning with `sk_test_` |
| `STRIPE_WEBHOOK_SECRET` | Stripe endpoint secret beginning with `whsec_`; a temporary placeholder is fine initially |
| `STRIPE_SUCCESS_URL` | `https://YOUR-VERCEL-DOMAIN/checkout/success?checkoutReference={CHECKOUT_REFERENCE}` |
| `STRIPE_CANCEL_URL` | `https://YOUR-VERCEL-DOMAIN/checkout/cancel?checkoutReference={CHECKOUT_REFERENCE}` |
| `TMDB_ACCESS_TOKEN` | TMDB API Read Access Token used only by the backend |

`JWT_SECRET` is generated automatically by Render and does not need to be
copied from the local `.env`.

`REDIS_KEY_NAMESPACE` is supplied by `render.yaml`. It keeps production
seat-map caches and seat locks separate from local or older deployments, even
when database IDs are reused.

`DEMO_DATA_ENABLED=true` is supplied by `render.yaml`. On first startup it
creates the portfolio catalogue, seats, future showtimes, and a demo customer.
Production does not create the demo administrator unless
`DEMO_ADMIN_ENABLED=true` is added manually. Keep that setting disabled for the
public deployment.

When `TMDB_ACCESS_TOKEN` is configured, every backend startup synchronizes four
currently playing UK films from TMDB and stores their metadata in Postgres. A
daily maintenance task refreshes that catalogue and maintains a rolling 14-day
showtime window. If the free backend sleeps through the daily task, the next
request wakes it and startup maintenance catches up automatically. A failed
TMDB request preserves the last successful catalogue.

## 3. Create the Render Backend

1. Push these deployment files to GitHub.
2. In Render, select **New > Blueprint**.
3. Connect the GitHub repository.
4. Render reads `render.yaml` and shows the `movie-reservation-api` service.
5. Enter every environment variable marked as requiring a value.
6. Apply the Blueprint and wait for the Docker build and Flyway migrations.
7. Open `https://YOUR-RENDER-DOMAIN/actuator/health`.

The expected response is:

```json
{"status":"UP"}
```

The demo seeder and rolling showtime generator are idempotent: redeploying does
not duplicate the catalogue, showtimes, or demo accounts.

## 4. Deploy the Vercel Frontend

1. Import the same GitHub repository into Vercel.
2. Set the root directory to `frontend`.
3. Add `NEXT_PUBLIC_API_URL=https://YOUR-RENDER-DOMAIN`.
4. Deploy the project.

After Vercel provides its final URL, update these Render variables:

- `CORS_ALLOWED_ORIGINS`
- `STRIPE_SUCCESS_URL`
- `STRIPE_CANCEL_URL`

Then redeploy the Render service.

## 5. Add the Stripe Webhook

In the Stripe test-mode dashboard:

1. Create a webhook endpoint pointing to
   `https://YOUR-RENDER-DOMAIN/checkout/webhook/stripe`.
2. Select `checkout.session.completed` and `checkout.session.expired`.
3. Copy the endpoint's `whsec_...` secret into Render's
   `STRIPE_WEBHOOK_SECRET`.
4. Redeploy the backend.

## Free Render Behaviour

Do not configure an artificial keep-alive ping. Let the free service sleep and
make the portfolio UI explain that the backend might need up to a minute to
wake. Before a live interview demo, open the health URL once to wake it.
