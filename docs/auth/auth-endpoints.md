# Auth Endpoint Contract

## Auth Response Shape

Both register and login return the same response body.

### AuthResponse

| Field | Type   | Required | Notes                           |
| ----- | ------ | -------: | ------------------------------- |
| token | string |      yes | JWT access token                |
| user  | object |      yes | safe authenticated user payload |

### AuthUserResponse

| Field       | Type    | Required | Notes                    |
| ----------- | ------- | -------: | ------------------------ |
| id          | number  |      yes | internal user id         |
| firstName   | string  |      yes | safe field               |
| lastName    | string  |      yes | safe field               |
| email       | string  |      yes | safe field               |
| phoneNumber | string  |       no | safe field               |
| role        | string  |      yes | `CUSTOMER`, `ADMIN`, etc |
| active      | boolean |      yes | active flag              |

## 1. POST /api/auth/register

### Purpose

Create a new customer account and immediately issue an access token.

### Request DTO

| Field       | Type   | Required | Notes                            |
| ----------- | ------ | -------: | -------------------------------- |
| firstName   | string |      yes | user first name                  |
| lastName    | string |      yes | user last name                   |
| email       | string |      yes | unique login identifier          |
| password    | string |      yes | raw password, hashed server-side |
| phoneNumber | string |       no | optional phone                   |

### Success Response

Status: `201 Created`

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

### Error Responses

| Status | Meaning                 |
| ------ | ----------------------- |
| 400    | invalid request payload |
| 409    | email already exists    |

### Response DTO

| Field            | Type    | Notes              |
| ---------------- | ------- | ------------------ |
| token            | string  | JWT access token   |
| user.id          | number  | internal user id   |
| user.firstName   | string  | safe user field    |
| user.lastName    | string  | safe user field    |
| user.email       | string  | safe user field    |
| user.phoneNumber | string  | safe user field    |
| user.role        | string  | usually `CUSTOMER` |
| user.active      | boolean | user active flag   |

---

## 2. POST /api/auth/login

### Purpose

Authenticate an existing user and return an access token.

### Request DTO

Suggested class: `LoginRequest`

| Field    | Type   | Required | Notes        |
| -------- | ------ | -------: | ------------ |
| email    | string |      yes | login email  |
| password | string |      yes | raw password |

### Success Response

Status: `200 OK`

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

### Response DTO

| Field | Type   | Notes                            |
| ----- | ------ | -------------------------------- |
| token | string | JWT access token                 |
| user  | object | same safe user shape as register |

### Errors

| Status | Meaning             |
| ------ | ------------------- |
| 401    | invalid credentials |

---

## 3. GET /api/auth/me

### Purpose

Return the current authenticated user resolved from the JWT principal.

### Headers

| Header        | Required | Notes          |
| ------------- | -------: | -------------- |
| Authorization |      yes | `Bearer <jwt>` |

### Success Response

Status: `200 OK`

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

### Response DTO

| Field       | Type    | Notes                       |
| ----------- | ------- | --------------------------- |
| id          | number  | authenticated user id       |
| firstName   | string  | safe user field             |
| lastName    | string  | safe user field             |
| email       | string  | safe user field             |
| phoneNumber | string  | safe user field             |
| role        | string  | role claim-backed user role |
| active      | boolean | active flag                 |

### Errors

| Status | Meaning                                       |
| ------ | --------------------------------------------- |
| 401    | missing, invalid, malformed, or expired token |

---

## 4. Mixed Ownership Endpoints

### Routes

- `POST /checkout/lock`
- `POST /checkout/confirm`
- `POST /checkout/cancel`
- `POST /api/reservations/{id}/cancel`

### Authenticated request Rule

| Condition | Behavior |
| --- | --- |
| valid JWT present | use authenticated principal |
| guest fields also present | reject as ambiguous for authenticated flow |

### Guest request Rule

| Condition           | Behavior                              |
| ------------------- | ------------------------------------- |
| no JWT present      | process as guest flow                 |
| lock                | requires `guestEmail`                 |
| confirm/cancel lock | requires `guestEmail` and `sessionId` |
| reservation cancel  | requires `guestEmail`                 |

---

## 5. DTO Summary

### Safe User DTO

| Field       | Type    |
| ----------- | ------- |
| id          | Long    |
| firstName   | String  |
| lastName    | String  |
| email       | String  |
| phoneNumber | String  |
| role        | String  |
| active      | boolean |

### Register Request DTO

| Field       | Type   |
| ----------- | ------ |
| firstName   | String |
| lastName    | String |
| email       | String |
| password    | String |
| phoneNumber | String |

### Login Request DTO

| Field    | Type   |
| -------- | ------ |
| email    | String |
| password | String |

### Auth Response DTO

| Field | Type        |
| ----- | ----------- |
| token | String      |
| user  | SafeUserDto |

```

```

### Note

- User entity should not be returned directly from auth endpoints.
- Password must never be exposed in responses.
- Logged-in checkout and reservation cancellation requests no longer include `userId`; identity comes from the JWT principal.
