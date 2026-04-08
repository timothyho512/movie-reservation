# JWT Authentication Design

## Summary

This document defines the first JWT-based authentication layer for the movie reservation API.

The goal is to add:

- user registration
- user login
- JWT generation and validation
- authenticated user lookup via `/api/auth/me`

This change must preserve the existing guest checkout flow.

The first pass does not include:

- refresh tokens
- password reset
- email verification
- role-based admin authorization
- full redesign of existing `/api/users` CRUD

## Goals

- Authenticate registered users with JWT bearer tokens
- Hash passwords with BCrypt instead of storing raw passwords
- Expose a clean API-first auth contract for frontend use
- Keep guest checkout and guest reservation cancellation working
- Use authenticated principal as the source of truth for logged-in ownership

## Non-Goals

- Cookie/session authentication
- OAuth/social login
- refresh-token flow
- admin portal security model
- account recovery flows

## Current State

The app already has:

- `User` entity with `id`, `email`, `password`, `role`, `active`
- checkout flow with guest and registered ownership logic
- reservation cancellation with guest and registered ownership rules
- JWT authentication with register, login, `/api/auth/me`, and `401` handling for protected routes
- logged-in checkout and reservation cancellation already resolved from the authenticated principal

## Auth Contract

### `POST /api/auth/register`

Registers a new customer account and immediately returns an access token.

#### Request

```json
{
  "firstName": "Jay",
  "lastName": "Doe",
  "email": "jay@example.com",
  "password": "plain-text-password",
  "phoneNumber": "07123456789"
}
```

###### Behavior

- validate required fields
- validate email format
- reject duplicate email
- hash password with BCrypt
- create user with role CUSTOMER
- create JWT for the new user

#### Response

Status: 201 Created

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

### `POST /api/auth/login`

Authenticates an existing user and returns an access token.

#### Request

```json
{
  "email": "jay@example.com",
  "password": "plain-text-password"
}
```

###### Behavior

- find active user by email
- compare password using BCrypt
- reject invalid credentials with 401 Unauthorized
- return signed JWT and safe user payload

#### Response

Status: 200 OK

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

### `GET /api/auth/me`

Returns the authenticated user resolved from the bearer token.

#### Headers

```http
Authorization: Bearer <jwt-token>
```

#### Response

Status: 200 OK

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

## JWT Design

### Token Type

Access token only.

### Transport

JWT is sent by the client in the Authorization header:

```http
Authorization: Bearer <token>
```

### Claims

Recommended claims:

- sub: user email
- userId: internal user id
- role: user role
- iat: issued-at time
- exp: expiry time

### Expiry

Use a fixed access-token expiry from configuration.
Example:

```properties
app.jwt.expiration-ms=3600000
```

### Secret

Use a configured secret from environment or application properties.
Example:

```properties
app.jwt.secret=...
```

### Security Boundaries

#### Public Routes

These remain accessible without authentication:

- POST /api/auth/register
- POST /api/auth/login
- GET /health
- read-only catalog endpoints intended for public client use
- GET /api/showtimes/{id}/available-seats
- guest checkout and guest cancellation flows

#### Protected Routes

These require a valid JWT:

- GET /api/auth/me
- existing /api/users/\*\* in the first pass, to stop public exposure

#### Mixed Routes

These routes allow either:

- guest identity via request body, or
- authenticated user via JWT principal

Routes:

- POST /checkout/lock
- POST /checkout/confirm
- POST /checkout/cancel
- POST /api/reservations/{id}/cancel

### Ownership Rules

#### Guest Flows

Guest ownership remains unchanged:

- checkout lock uses guestEmail
- checkout confirm uses guestEmail + sessionId
- checkout cancel uses guestEmail + sessionId
- reservation cancel uses guestEmail

#### Authenticated Flows

Authenticated ownership must come from the JWT principal.

For logged-in requests:

- derive user identity from authenticated principal
- logged-in request DTOs do not carry `userId`
- service-layer ownership checks compare persisted reservation or lock ownership against principal `userId`

### Error Handling

#### Register

- 400 Bad Request: validation failure
- 409 Conflict: email already exists

#### Login

- 401 Unauthorized: invalid email/password
- 403 Forbidden: inactive account if enforced

#### Authenticated Access

- 401 Unauthorized: missing, invalid, malformed, or expired JWT

#### Ownership Failures

- 403 Forbidden is the cleaner long-term choice for ownership mismatch
- if current codebase standard is 409 Conflict via existing exception mapping, keep behavior consistent in the first pass and normalize later

### Implementation Notes

- add Spring Security dependency
- add JWT library dependency
- add PasswordEncoder bean using BCrypt
- add UserRepository.findByEmail(...)
- add auth DTOs and response DTOs
- add JWT utility/service
- add authentication filter that populates SecurityContext
- configure stateless security
- return safe DTOs, never User entity directly from auth endpoints

### Test Plan

#### Registration

- register succeeds with valid payload
- password stored in DB is hashed
- duplicate email fails with 409

#### Login

- login succeeds with correct credentials
- login fails with unknown email
- login fails with wrong password

#### Token Validation

- /api/auth/me works with valid token
- /api/auth/me fails without token
- /api/auth/me fails with malformed token
- /api/auth/me fails with expired token

#### Mixed Ownership

- authenticated checkout lock succeeds using JWT principal
- authenticated checkout confirm succeeds using JWT principal
- authenticated checkout cancel succeeds using JWT principal
- authenticated reservation cancel succeeds using JWT principal
- guest checkout flow still works unchanged
- guest reservation cancel still works unchanged

#### Deferred Follow-Up

After this pass:

- tighten /api/users/\*\* into admin/self-only rules
- add refresh-token strategy if needed
- consider role-based authorization for admin write endpoints
