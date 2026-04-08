# JWT Authentication Design

## Overview

This document defines the first JWT-based authentication layer for the movie reservation API.

The goal is to:

- add user registration
- add user login
- validate JWT bearer tokens
- expose authenticated user info through `/api/auth/me`
- use JWT principal as the source of truth for logged-in ownership

This change must preserve guest checkout and guest reservation cancellation.

## Scope

Included in this pass:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/me`
- Spring Security stateless JWT authentication
- BCrypt password hashing
- authenticated principal support for logged-in checkout and reservation cancellation

Not included in this pass:

- refresh tokens
- password reset
- email verification
- social login
- full role-based authorization model
- full redesign of `/api/users/**`

## Design Decisions

- Authentication uses Spring Security with stateless bearer-token auth.
- Passwords are hashed with BCrypt before persistence.
- Both `register` and `login` return `token + safe user payload`.
- JWT is access-token only in this pass.
- Guest flow remains unchanged.
- Logged-in identity comes from the JWT principal, not the request body.

## Security Boundary

### Public routes

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /health`
- public read endpoints such as showtime availability
- guest checkout and guest cancellation flows when no JWT is provided

### Protected routes

- `GET /api/auth/me`
- `/api/users/**` should no longer remain public in this pass

### Mixed routes

These routes support either guest identity or authenticated identity:

- `POST /checkout/lock`
- `POST /checkout/confirm`
- `POST /checkout/cancel`
- `POST /api/reservations/{id}/cancel`

Rule:

- if a valid JWT is present, use authenticated principal
- if no JWT is present, use guest request fields
- authenticated flows must not include guest identity fields

## JWT Contract

### Transport

The client sends the token in the `Authorization` header:

```http
Authorization: Bearer <token>
```

### Claims

- `sub`: user email
- `userId`: internal user id
- `role`: user role
- `iat`: issued-at timestamp
- `exp`: expiry timestamp

### Configuration

- `app.jwt.secret`
- `app.jwt.expiration-ms`

### Ownership Rules

#### Guest requests

Guest ownership remains as implemented today:

- checkout lock: `guestEmail`
- checkout confirm: `guestEmail` + `sessionId`
- checkout cancel: `guestEmail` + `sessionId`
- reservation cancel: `guestEmail`

#### Authenticated requests

Authenticated ownership is resolved from the principal:

- use `principal.userId`
- logged-in request DTOs do not include `userId`
- compare persisted reservation or lock ownership against principal identity

## Error Policy

- `400 Bad Request`: invalid input
- `401 Unauthorized`: invalid login or invalid/missing/expired JWT
- `409 Conflict`: duplicate email
- ownership mismatch can remain aligned with current exception handling in the first pass if needed, then normalized later

## Implementation Shape

### `AuthController`

- `register`
- `login`
- `me`

### `AuthService`

- registration
- credential validation
- current-user lookup

### `JwtService`

- token generation
- token parsing
- token validation

### `JwtAuthenticationFilter`

- parse bearer token
- validate token
- populate `SecurityContext`

### `UserRepository`

- add `findByEmail(...)`
- auth endpoints return DTOs only, never raw `User` entity

## Acceptance Criteria

- register stores hashed password and returns token
- login returns token for valid credentials
- `/api/auth/me` returns authenticated user
- invalid or missing JWT returns `401`
- guest flows continue unchanged
- authenticated checkout and reservation cancel work through JWT principal only
