# API Reference

All endpoints can be reached two ways:

- **Direct**: `http://localhost:<port>/<path>` (bypasses the gateway)
- **Via gateway**: `http://localhost:8080/<service-name>/<path>` (gateway strips the
  `<service-name>/` prefix via `stripPrefix(1)` before forwarding)

## Services and ports

| Service            | Port | Application name   | Business endpoints                     |
| ------------------ | ---- | ------------------ | -------------------------------------- |
| `service-registry` | 8761 | `service-registry` | Eureka dashboard only (no REST API)    |
| `api-gateway`      | 8080 | `api-gateway`      | Routing only (no business endpoints)   |
| `account-service`  | 8081 | `account-service`  | Accounts, auth, JWT                    |
| `driver-service`   | 8082 | `driver-service`   | Driver profiles, location, availability|
| `ride-service`     | 8083 | `ride-service`     | Ride lifecycle                         |
| `fare-service`     | 8084 | `fare-service`     | Fare estimation, payments, receipts    |

Every business service also exposes the Spring Boot Actuator endpoints listed below.

## OpenAPI / Swagger UI

The machine-readable contract is generated at runtime by
[springdoc-openapi](https://springdoc.org/) from the annotations on each controller. Nothing
is checked in, so the spec cannot drift from the code.

**Use the aggregated UI on the gateway** — it lists every service in a dropdown:

```
http://localhost:8080/swagger-ui.html
```

| What                     | URL                                                                     |
| ------------------------ | ----------------------------------------------------------------------- |
| Aggregated UI (gateway)  | `http://localhost:8080/swagger-ui.html`                                  |
| Per-service UI           | `http://localhost:<8081-8084>/swagger-ui.html`                           |
| Per-service spec         | `http://localhost:<8081-8084>/v3/api-docs`                              |
| Per-service spec via gw  | `http://localhost:8080/<service>/v3/api-docs`                            |
| UI configuration         | `http://localhost:<port>/v3/api-docs/swagger-config`                    |

The per-service **UI** deliberately does not work through the gateway
(`http://localhost:8080/<service>/swagger-ui.html` renders blank): swagger-ui always fetches
its configuration from the *root* path `/v3/api-docs/swagger-config`, which the gateway does
not route. The raw spec through the gateway does work, because `/<service>/v3/api-docs` is
rewritten to `/v3/api-docs` by `stripPrefix(1)`.

### Authenticating in the UI

Each service spec declares a `bearerAuth` security scheme and applies it as the **default for
every operation** (see each service's `OpenApiConfig`). That is what makes the **Authorize**
button work: the token you paste there is only attached to operations that declare a security
requirement.

1. `POST /api/v1/auth/login` returns a raw JWT string. Copy it — do **not** add `Bearer `.
2. Paste it into the **Authorize** dialog.
3. Secured operations show a lock icon and send `Authorization: Bearer <token>`.

> **Only `account-service` actually enforces security.** `driver-service`, `ride-service` and
> `fare-service` have no Spring Security configuration: their specs *declare* `bearerAuth` (a
> copy of the account-service `OpenApiConfig`), but the services never inspect the token, so a
> lock icon there does not mean a token is required. `ride-service` identifies the caller with
> an `X-User-Id` header instead (see below).

In `account-service` the public operations are listed in `OpenApiConfig.PUBLIC_PATHS`
(`/api/v1/auth/signup`, `/api/v1/auth/login`), which clears the requirement for them so they
show no lock icon. `GET /api/v1/auth/me` is authenticated and is **not** in that list. Add a
path to that list when a new public account-service endpoint is introduced.

All OpenAPI metadata lives in each service's `OpenApiConfig` class, so the controllers carry no
Swagger annotations. springdoc still derives the paths, methods and payload schemas from the
code itself, which is why the request and response shapes in the UI stay in sync automatically.

### Pointing the docs at another host

Each spec advertises a single server, taken from `app.openapi.servers.base-url`, which
defaults to the local gateway and is overridable per environment:

```bash
export OPENAPI_SERVERS_BASE_URL=https://api.example.com/account-service
```

### Keeping the spec honest

`OpenApiDocsTests` (account-service) and `OpenApiAggregationTests` (api-gateway) assert that
the spec and the UI are actually served, that `bearerAuth` is declared, and that entity
internals such as the BCrypt hash do not leak into the published document. They run as part
of `./mvnw verify`.

## Authentication model

| Service           | Security                                                                    |
| ----------------- | --------------------------------------------------------------------------- |
| `account-service` | Spring Security, stateless JWT. Only `/api/v1/auth/signup` and `/api/v1/auth/login` are public; every other path needs `Authorization: Bearer <jwt>`. A missing or invalid token fails with `403` (no `AuthenticationEntryPoint` is registered). |
| `driver-service`  | None. Any client can call any endpoint.                                     |
| `ride-service`    | None, but every endpoint except `GET /api/v1/ride/{id}/status` requires an `X-User-Id` header naming the calling account. This is a **placeholder for real auth** — nothing verifies it, so any client can claim any account id. |
| `fare-service`    | None. Any client can call any endpoint.                                     |

## Actuator (all services)

| Method | Path                | Port         | Access                        |
| ------ | ------------------- | ------------ | ----------------------------- |
| GET    | `/actuator/health`  | each service | public (`permitAll`)          |
| GET    | `/actuator/info`    | each service | public (`permitAll`)          |
| GET    | `/actuator/metrics` | each service | open on driver/ride/fare; **behind JWT on `account-service`** (only `health`/`info` are permitted there) |

Health includes DB status:

```console
$ curl http://localhost:8081/actuator/health
{"status":"UP","components":{"db":{"status":"UP",...},"ping":{"status":"UP"}}}
```

## Endpoint index

| Method     | Endpoint                          | Service (port)      | Caller identity     |
| ---------- | --------------------------------- | ------------------- | ------------------- |
| POST       | `/api/v1/auth/signup`             | account (8081)      | public              |
| POST       | `/api/v1/auth/login`              | account (8081)      | public              |
| GET        | `/api/v1/auth/me`                 | account (8081)      | JWT                 |
| POST       | `/api/v1/auth/logout`             | account (8081)      | JWT                 |
| GET        | `/api/v1/accounts/profile`        | account (8081)      | JWT                 |
| PUT        | `/api/v1/accounts/profile`        | account (8081)      | JWT                 |
| PUT        | `/api/v1/accounts/profile/password` | account (8081)    | JWT                 |
| POST       | `/api/v1/driver`                  | driver (8082)       | none enforced       |
| PUT        | `/api/v1/driver/{id}/location`    | driver (8082)       | none enforced       |
| PUT/PATCH  | `/api/v1/driver/{id}/availability` | driver (8082)      | none enforced       |
| GET        | `/api/v1/driver/eligible`         | driver (8082)       | none enforced       |
| POST       | `/api/v1/ride/request`            | ride (8083)         | `X-User-Id`         |
| GET        | `/api/v1/ride/{id}`               | ride (8083)         | `X-User-Id`         |
| GET        | `/api/v1/ride/{id}/status`        | ride (8083)         | none                |
| GET        | `/api/v1/ride/passenger/me`       | ride (8083)         | `X-User-Id`         |
| GET        | `/api/v1/ride/driver/me`          | ride (8083)         | `X-User-Id`         |
| POST       | `/api/v1/ride/{id}/assign`        | ride (8083)         | none                |
| PATCH      | `/api/v1/ride/{id}/start`         | ride (8083)         | `X-User-Id`         |
| PATCH      | `/api/v1/ride/{id}/complete`      | ride (8083)         | `X-User-Id`         |
| POST       | `/api/v1/ride/{id}/cancel`        | ride (8083)         | `X-User-Id`         |
| GET        | `/api/v1/ride/{id}/cancellation`  | ride (8083)         | `X-User-Id`         |
| POST       | `/api/v1/fare/estimate`           | fare (8084)         | none enforced       |
| POST       | `/api/v1/fare/final`              | fare (8084)         | none enforced       |
| POST       | `/api/payments`                   | fare (8084)         | none enforced       |
| GET        | `/api/payments/{paymentId}`       | fare (8084)         | none enforced       |
| GET        | `/api/payments/ride/{rideId}`     | fare (8084)         | none enforced       |
| GET        | `/api/payments/{paymentId}/receipt` | fare (8084)       | none enforced       |

The exact direct and gateway URLs follow the pattern in the introduction, e.g.
`GET localhost:8080/ride-service/api/v1/ride/42` proxies to `localhost:8083/api/v1/ride/42`.

---

## account-service

### POST /api/v1/auth/signup

Creates a user. Password is BCrypt-hashed before persistence. `role` defaults to `PASSENGER`
when omitted.

**Request**

```http
POST /account-service/api/v1/auth/signup HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{"name": "Virul", "email": "virul@gmail.com", "password": "Virul@123", "role": "DRIVER"}
```

**Response — `201 Created`** (a `UserResponse` projection: the hash and the Spring Security
`UserDetails` fields are deliberately not serialised)

```json
{
  "id": 1,
  "name": "Virul",
  "email": "virul@gmail.com",
  "role": "DRIVER"
}
```

**Errors**

| Status | When                                                                       |
| ------ | -------------------------------------------------------------------------- |
| `400`  | Validation failure (`name` < 2 / > 50 chars, invalid `email`, `password` < 6 chars, unknown `role`) or malformed JSON |
| `409`  | The `email` is already registered (`An account with this email already exists`) |

### POST /api/v1/auth/login

Accepts `email` + `password`, authenticates with Spring Security, and returns a signed JWT
as a raw `text/plain` string (not a JSON object, so there is no `token` field to unwrap). The
token carries the account role as a claim and expires after 24 hours
(`app.jwt.expiration`). Send it on later requests as `Authorization: Bearer <token>`, or
paste the raw value into the **Authorize** dialog in Swagger UI.

**Request**

```http
POST /account-service/api/v1/auth/login HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{"email": "virul@gmail.com", "password": "Virul@123"}
```

**Response — `200 OK`**

```
eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiUEFTU0VOR0VSIiwic3ViIjoidmlydWxAZ21haWwuY29tIn0....
```

**Errors**

| Status | When                                                              |
| ------ | ----------------------------------------------------------------- |
| `400`  | Blank/invalid `email` or blank `password`                        |
| `401`  | Unknown email or wrong password (`Invalid email or password`)    |

### GET /api/v1/auth/me

Returns the account behind the presented token, read from the Spring Security principal and
projected to a `UserResponse` (the raw entity is never serialised).

**Request**

```http
GET /account-service/api/v1/auth/me HTTP/1.1
Host: localhost:8080
Authorization: Bearer <token>
```

**Response — `200 OK`** (a `UserResponse` projection)

```json
{
  "id": 1,
  "name": "Virul",
  "email": "virul@gmail.com",
  "role": "PASSENGER"
}
```

**Errors**

| Status | When                                                    |
| ------ | ------------------------------------------------------- |
| `403`  | Missing or invalid bearer token                         |

### POST /api/v1/auth/logout

Signs the caller out. The server is stateless and keeps no session to invalidate, so this
endpoint only validates the token, logs an audit line, and acknowledges: **the client must
discard the JWT**. The token stays cryptographically valid until it expires (24 h).

**Request**

```http
POST /account-service/api/v1/auth/logout HTTP/1.1
Host: localhost:8080
Authorization: Bearer <token>
```

**Response — `200 OK`** (`Signed out successfully`)

**Errors**

| Status | When                            |
| ------ | ------------------------------- |
| `403`  | Missing or invalid bearer token |

### GET /api/v1/accounts/profile

Fetches the authenticated user's profile.

**Request**

```http
GET /account-service/api/v1/accounts/profile HTTP/1.1
Host: localhost:8080
Authorization: Bearer <token>
```

**Response — `200 OK`** (a `UserResponse` projection)

```json
{
  "id": 1,
  "name": "Virul",
  "email": "virul@gmail.com",
  "role": "PASSENGER"
}
```

**Errors**

| Status | When                                                                   |
| ------ | ---------------------------------------------------------------------- |
| `403`  | Missing or invalid bearer token                                        |
| `404`  | The user the token names no longer exists (`User not found with email: ...`) |

### PUT /api/v1/accounts/profile

Updates the authenticated user's name. Role manipulation is ignored by the payload
(privilege escalation is impossible here).

**Request**

```http
PUT /account-service/api/v1/accounts/profile HTTP/1.1
Host: localhost:8080
Content-Type: application/json
Authorization: Bearer <token>

{"name": "New Name"}
```

**Response — `200 OK`** (the updated `UserResponse`)

**Errors**

| Status | When                                              |
| ------ | ------------------------------------------------- |
| `400`  | `name` blank or outside 2–50 characters           |
| `403`  | Missing or invalid bearer token                   |

### PUT /api/v1/accounts/profile/password

Securely changes the authenticated user's password.

**Request**

```http
PUT /account-service/api/v1/accounts/profile/password HTTP/1.1
Host: localhost:8080
Content-Type: application/json
Authorization: Bearer <token>

{"oldPassword": "Virul@123", "newPassword": "Virul@456"}
```

**Response — `200 OK`** (`Password updated successfully`)

**Errors**

| Status | When                                                        |
| ------ | ----------------------------------------------------------- |
| `400`  | `oldPassword` does not match, or `newPassword` < 6 chars    |
| `403`  | Missing or invalid bearer token                             |

---

## driver-service

No authentication is enforced. `Driver` doubles as both the registration request body and the
response body for every endpoint; `id` and `lastOnlineAt` are server-assigned.

| Field                | Notes                                                        |
| -------------------- | ------------------------------------------------------------ |
| `id`                 | Server-assigned, ignore it on register                       |
| `userId`             | Account id this driver signs in with                         |
| `vehicleNumberPlate` | Unique                                                        |
| `latitude`/`longitude` | Current position                                            |
| `serviceArea`        | Free-text area the driver operates in                        |
| `available`          | JSON name of the `isAvailable` flag; forced `true` on register |
| `accuracy`           | Optional position accuracy in metres                         |
| `lastOnlineAt`       | Server-assigned timestamp                                    |

The controller is mapped at **both** `/api/v1/driver` and `/api/v1/drivers`; the examples below
use the singular form.

### POST /api/v1/driver

Registers a driver. The server sets `available = true` and `lastOnlineAt = now`.

**Request**

```http
POST /driver-service/api/v1/driver HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
  "userId": 9,
  "vehicleNumberPlate": "WP-CAB-4821",
  "latitude": 6.9271,
  "longitude": 79.8612,
  "serviceArea": "Colombo",
  "accuracy": 12.5
}
```

**Response — `201 Created`**

```json
{
  "id": 5,
  "userId": 9,
  "vehicleNumberPlate": "WP-CAB-4821",
  "latitude": 6.9271,
  "longitude": 79.8612,
  "serviceArea": "Colombo",
  "available": true,
  "accuracy": 12.5,
  "lastOnlineAt": "2026-09-28T10:19:02"
}
```

**Errors**

| Status | When                                                                       |
| ------ | -------------------------------------------------------------------------- |
| `500`  | A required column is null, or `vehicleNumberPlate` violates its unique key. The request body is not annotated with validation constraints, so missing fields reach the database and fail there. |

### PUT /api/v1/driver/{id}/location

Updates a driver's current position and refreshes `lastOnlineAt`.

**Request**

```http
PUT /driver-service/api/v1/driver/5/location HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{"latitude": 6.9271, "longitude": 79.8612, "accuracy": 12.5}
```

**Response — `200 OK`**, the updated `Driver`.

**Errors**

| Status | When                                                        |
| ------ | ----------------------------------------------------------- |
| `400`  | `latitude` outside −90..90 or `longitude` outside −180..180 |
| `500`  | No driver carries that `id` (`Driver not found`)            |

### PUT or PATCH /api/v1/driver/{id}/availability

Sets or toggles a driver's availability and appends an availability-history row.

- Body `{"available": true|false}` sets the value explicitly.
- Omitting the body (or the `available` field) **flips** the current value.

**Request**

```http
PATCH /driver-service/api/v1/driver/5/availability HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{"available": true}
```

**Response — `200 OK`**, the updated `Driver`.

**Errors**

| Status | When                                             |
| ------ | ------------------------------------------------ |
| `500`  | No driver carries that `id` (`Driver not found`) |

> `GET /api/v1/driver/eligible` is the read side used by `ride-service`; it returns the
> drivers with `available = true`.

### GET /api/v1/driver/eligible

**Request**

```http
GET /driver-service/api/v1/driver/eligible HTTP/1.1
Host: localhost:8080
```

**Response — `200 OK`**, a JSON array of `Driver` objects. Empty array when none are
available.

---

## ride-service

Every endpoint except `GET /api/v1/ride/{id}/status` requires an `X-User-Id` header naming the
calling account. The value is a placeholder for real authentication: nothing verifies it, so
any client can claim any account id. A missing or non-numeric header yields `400`.

Ride states (`Status` enum): `SEARCHING`, `ASSIGNED`, `ONGOING`, `COMPLETED`, `CANCELLED`.

### POST /api/v1/ride/request

Registers a ride request and returns the persisted ride, so the caller gets back the `id` to
track it with. `requestedTime` is stamped by the server, so a client cannot backdate a
request.

**Request**

```http
POST /ride-service/api/v1/ride/request HTTP/1.1
Host: localhost:8080
Content-Type: application/json
X-User-Id: 1

{"pickupLocation": "Colombo Fort Railway Station", "dropOffLocation": "Galle Face Green", "tripDistance": 7.4}
```

**Response — `201 Created`**, with a `Location: /api/v1/ride/42` header

```json
{
  "id": 42,
  "passengerId": 1,
  "driverId": null,
  "status": "SEARCHING",
  "pickupLocation": "Colombo Fort Railway Station",
  "dropOffLocation": "Galle Face Green",
  "tripDistance": 7.4,
  "requestedTime": "2026-09-28T10:15:30",
  "assignedAt": null,
  "startTime": null,
  "completedTime": null,
  "cancelledAt": null,
  "cancelledBy": null
}
```

**Errors**

| Status | When                                                                        |
| ------ | --------------------------------------------------------------------------- |
| `400`  | Missing `X-User-Id`, or a blank location / non-positive `tripDistance`      |
| `409`  | The passenger already has a ride in `SEARCHING`, `ASSIGNED` or `ONGOING`    |

### GET /api/v1/ride/{id}

Reads a single ride back — the poll target for the `id` the create call returns. Scoped to the
owning passenger.

**Request**

```http
GET /ride-service/api/v1/ride/42 HTTP/1.1
Host: localhost:8080
X-User-Id: 1
```

**Response — `200 OK`**, same shape as the create call.

**Errors**

| Status | When                                  |
| ------ | ------------------------------------- |
| `404`  | No ride carries that id               |
| `403`  | The ride belongs to another passenger |

### GET /api/v1/ride/{id}/status

Returns a compact status view of any ride. Unlike the other ride endpoints this one takes **no
`X-User-Id` header** and is not scoped to a caller.

**Response — `200 OK`**

```json
{
  "rideId": 42,
  "status": "ASSIGNED",
  "updatedAt": "2026-09-28T10:17:04",
  "assignedAt": "2026-09-28T10:17:04",
  "driverId": 9,
  "cancelledAt": null
}
```

`updatedAt` is the most recent of the ride's timestamps (falling back to `requestedTime`).

**Errors**

| Status | When                    |
| ------ | ----------------------- |
| `404`  | No ride carries that id |

### GET /api/v1/ride/passenger/me

Lists the calling passenger's rides, newest request first.

**Query parameters** (all optional): `status` (one of the enum values, case-insensitive),
`page` and `size` (paging is only applied when **both** are supplied).

**Request**

```http
GET /ride-service/api/v1/ride/passenger/me?status=SEARCHING&page=0&size=20 HTTP/1.1
Host: localhost:8080
X-User-Id: 1
```

**Response — `200 OK`**, a JSON array of rides (same shape as the create call).

**Errors**

| Status | When                                        |
| ------ | ------------------------------------------- |
| `400`  | Missing `X-User-Id`, or an unknown `status` |

### GET /api/v1/ride/driver/me

Lists the calling driver's rides, newest assignment first.

**Query parameters**: same as `/passenger/me` (`status`, `page`, `size`).

**Request**

```http
GET /ride-service/api/v1/ride/driver/me HTTP/1.1
Host: localhost:8080
X-User-Id: 9
```

**Response — `200 OK`**, a JSON array of rides.

**Errors**

| Status | When                                        |
| ------ | ------------------------------------------- |
| `400`  | Missing `X-User-Id`, or an unknown `status` |

### POST /api/v1/ride/{id}/assign

Assigns the first available driver (read from `driver-service` `GET /api/v1/driver/eligible`) to
a `SEARCHING` ride and transitions it to `ASSIGNED`. Like `/status`, this dispatch endpoint
takes **no `X-User-Id`** and no request body.

**Request**

```http
POST /ride-service/api/v1/ride/42/assign HTTP/1.1
Host: localhost:8080
```

**Response — `200 OK`**, the ride with `status: "ASSIGNED"`, `driverId` and `assignedAt` set.

**Errors**

| Status | When                                                                |
| ------ | ------------------------------------------------------------------- |
| `404`  | No ride carries that id                                             |
| `409`  | The ride is not in `SEARCHING`, or no driver is currently available |

The ride is read under a `PESSIMISTIC_WRITE` lock, so two concurrent assigns cannot both pick a
driver for the same ride.

### PATCH /api/v1/ride/{id}/start

Transitions `ASSIGNED → ONGOING`. Only the assigned driver may start the ride.

**Request**

```http
PATCH /ride-service/api/v1/ride/42/start HTTP/1.1
Host: localhost:8080
X-User-Id: 9
```

**Response — `200 OK`**, the ride with `status: "ONGOING"` and `startTime` set.

**Errors**

| Status | When                                              |
| ------ | ------------------------------------------------- |
| `404`  | No ride carries that id                           |
| `403`  | The caller is not the assigned driver             |
| `409`  | The ride is not in `ASSIGNED`                     |

### PATCH /api/v1/ride/{id}/complete

Transitions `ONGOING → COMPLETED`. Either the passenger or the assigned driver may complete
the ride.

**Request**

```http
PATCH /ride-service/api/v1/ride/42/complete HTTP/1.1
Host: localhost:8080
X-User-Id: 9
```

**Response — `200 OK`**, the ride with `status: "COMPLETED"` and `completedTime` set.

**Errors**

| Status | When                                                  |
| ------ | ----------------------------------------------------- |
| `404`  | No ride carries that id                               |
| `403`  | The caller is neither the passenger nor the driver    |
| `409`  | The ride is not in `ONGOING`                          |

### POST /api/v1/ride/{id}/cancel

Cancels a ride and records the cancellation. The caller must be the passenger who requested
the ride or the driver assigned to it, and the state each of them is allowed to cancel from
differs:

| Caller          | Cancellable from              | Reasoning                              |
| --------------- | ----------------------------- | -------------------------------------- |
| Passenger       | `SEARCHING`, `ASSIGNED`, `ONGOING` | Allowed until the ride is `COMPLETED` |
| Assigned driver | `SEARCHING`, `ASSIGNED`       | Locked in once the trip is `ONGOING`   |
| Anyone else     | —                             | `403`, including an unassigned driver  |

The body is optional; when given, `reason` (max 500 characters) is stored on the cancellation
history row so the audit trail records *why* a ride was dropped, not just that it was.

**Request**

```http
POST /ride-service/api/v1/ride/42/cancel HTTP/1.1
Host: localhost:8080
Content-Type: application/json
X-User-Id: 1

{"reason": "Change of plans"}
```

**Response — `200 OK`**, the ride with `status: "CANCELLED"` and `cancelledAt` / `cancelledBy` set.

**Errors**

| Status | When                                                             |
| ------ | ---------------------------------------------------------------- |
| `400`  | Missing `X-User-Id`, or a `reason` over 500 characters           |
| `404`  | No ride carries that id                                          |
| `403`  | The caller is neither the passenger nor the assigned driver      |
| `409`  | The ride is `COMPLETED`, already `CANCELLED`, or in a state the caller may not cancel from |

The ride is read under a `PESSIMISTIC_WRITE` lock, so two concurrent cancels cannot both see a
cancellable state: the second one blocks, then reads the `CANCELLED` state the first committed
and is rejected with `409`. The `ride_cancellations` table also has a unique index on `ride_id`,
so a ride can carry at most one cancellation row.

### GET /api/v1/ride/{id}/cancellation

Reads the cancellation history row for a ride. Visible to the passenger who requested it and to
the driver assigned to it.

**Response — `200 OK`**

```json
{
  "rideId": 42,
  "previousStatus": "SEARCHING",
  "cancelledBy": 1,
  "cancelledAt": "2026-09-28T10:22:10",
  "reason": "Change of plans"
}
```

`previousStatus` is the state the ride was in before the cancel, so `SEARCHING` versus `ONGOING`
distinguishes a driver who bailed before pickup from one who dropped the ride mid-trip.

**Errors**

| Status | When                                                        |
| ------ | ----------------------------------------------------------- |
| `404`  | No ride carries that id, or the ride was never cancelled    |
| `403`  | The caller is neither the passenger nor the assigned driver |

---

## fare-service

No authentication is enforced. There is no linkage to `ride-service`: the fare endpoints price
whatever distance the caller supplies, and the payment endpoints persist whatever amount they
are given.

### POST /api/v1/fare/estimate

Prices a ride on its quoted distance before the trip starts.

Formula: `200.00 + 100.00 × distanceKm`, rounded half-up to 2 decimals (LKR).

**Request**

```http
POST /fare-service/api/v1/fare/estimate HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{"rideId": 42, "distanceKm": 7.4}
```

**Response — `200 OK`**

```json
{
  "rideId": 42,
  "distanceKm": 7.4,
  "estimatedFare": 940.00
}
```

**Errors**

| Status | When                                              |
| ------ | ------------------------------------------------- |
| `400`  | Missing `rideId`, or `distanceKm` ≤ 0             |

### POST /api/v1/fare/final

Prices a completed ride on the distance actually driven.

**Request**

```http
POST /fare-service/api/v1/fare/final HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{"rideId": 42, "actualDistanceKm": 8.1}
```

**Response — `200 OK`**

```json
{
  "rideId": 42,
  "actualDistanceKm": 8.1,
  "finalFare": 1010.00
}
```

**Errors**

| Status | When                                                    |
| ------ | ------------------------------------------------------- |
| `400`  | Missing `rideId`, or `actualDistanceKm` ≤ 0             |

### POST /api/payments

Records a simulated payment for a ride. No card details are taken; the payment is created
already `PAID` with a generated `TXN-<uuid>` reference. `paymentMethod` is upper-cased.

**Request**

```http
POST /fare-service/api/payments HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{"rideId": 42, "amount": 1010.00, "paymentMethod": "CARD"}
```

**Response — `201 Created`**, with a `Location: /api/payments/7` header

```json
{
  "paymentId": 7,
  "rideId": 42,
  "amount": 1010.00,
  "paymentMethod": "CARD",
  "status": "PAID",
  "transactionReference": "TXN-3f2a9c10-...",
  "paidAt": "2026-09-28T10:44:07"
}
```

**Errors**

| Status | When                                                                    |
| ------ | ----------------------------------------------------------------------- |
| `400`  | Missing `rideId`, `amount` ≤ 0, or `paymentMethod` not CASH/CARD/WALLET |

### GET /api/payments/{paymentId}

**Response — `200 OK`**, a single `PaymentResponse`.

**Errors**

| Status | When                            |
| ------ | ------------------------------- |
| `404`  | No payment carries that id      |

### GET /api/payments/ride/{rideId}

Lists all payments recorded for a ride, newest first.

**Response — `200 OK`**, a JSON array of `PaymentResponse` (empty when the ride has none).

### GET /api/payments/{paymentId}/receipt

**Response — `200 OK`**

```json
{
  "receiptNumber": "RCP-7",
  "paymentId": 7,
  "rideId": 42,
  "amount": 1010.00,
  "paymentMethod": "CARD",
  "paymentStatus": "PAID",
  "transactionReference": "TXN-3f2a9c10-...",
  "paidAt": "2026-09-28T10:44:07",
  "message": "Payment completed successfully"
}
```

**Errors**

| Status | When                        |
| ------ | --------------------------- |
| `404`  | No payment carries that id  |

---

## Bruno collection

`api-tests/` contains a [Bruno](https://www.usebruno.com/) collection
(`opencollection.yml`) with one folder per service:

- `account-service/`
  - `health check (public endpoint)` — `GET localhost:8080/actuator/health`
  - `signup-driver` / `signup-passenger` — `POST localhost:8080/account-service/api/v1/auth/signup`
  - `login` — `POST localhost:8080/account-service/api/v1/auth/login` with `{email, password}`
  - `logedin user` — `GET localhost:8080/account-service/api/v1/auth/me` with a bearer token
- `driver-service/`
  - `register` — `POST localhost:8080/driver-service/api/v1/driver`
  - `update location` — `PUT localhost:8080/driver-service/api/v1/driver/{id}/location`
  - `update availability` — `PATCH localhost:8080/driver-service/api/v1/driver/{id}/availability`
  - `Eligible available drivers` — `GET localhost:8080/driver-service/api/v1/driver/eligible`

`View Profile.yml` at the collection root still targets `GET
localhost:8080/account-service/api/v1/users/me`, a path that no longer exists — the profile
endpoint is `GET /api/v1/accounts/profile` (or `GET /api/v1/auth/me`). There are currently no
Bruno requests for `ride-service` or `fare-service`.
