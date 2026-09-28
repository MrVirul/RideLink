# API Reference

All endpoints can be reached two ways:

- **Direct**: `http://localhost:<port>/<path>` (bypasses the gateway)
- **Via gateway**: `http://localhost:8080/<service-name>/<path>` (gateway strips the
  `<service-name>/` prefix via `stripPrefix(1)` before forwarding)

`account-service` (auth), `driver-service` (driver records) and `ride-service` (ride
lifecycle) expose business endpoints today; `fare-service` is scaffolded and exposes nothing yet.
Every service exposes the Spring Boot Actuator endpoints below.

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

Each spec declares a `bearerAuth` security scheme and applies it as the **default for every
operation** (see `OpenApiConfig`). That is what makes the **Authorize** button work: the
token you paste there is only attached to operations that declare a security requirement.

1. `POST /api/v1/auth/login` returns a raw JWT string. Copy it — do **not** add `Bearer `.
2. Paste it into the **Authorize** dialog.
3. Secured operations show a lock icon and send `Authorization: Bearer <token>`.

The two public operations under `/api/v1/auth` are listed in `OpenApiConfig.PUBLIC_PATHS`,
which clears the requirement for them, so they show no lock icon and need no token. Add a path
to that list when a new public endpoint is introduced.

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

## Actuator (all services)

| Method | Path                | Port        | Access            |
| ------ | ------------------- | ----------- | ----------------- |
| GET    | `/actuator/health`  | each service | public (`permitAll`) |
| GET    | `/actuator/info`    | each service | public (`permitAll`) |
| GET    | `/actuator/metrics` | each service | public (needs the right path param set) |

Health includes DB status:

```console
$ curl http://localhost:8081/actuator/health
{"status":"UP","components":{"db":{"status":"UP",...},"ping":{"status":"UP"}}}
```

## Endpoint table

| Method | Endpoint                          | Direct URL                              | Via gateway URL                                        |
| ------ | --------------------------------- | --------------------------------------- | ------------------------------------------------------ |
| POST   | `/api/v1/auth/signup`             | `localhost:8081/api/v1/auth/signup`     | `localhost:8080/account-service/api/v1/auth/signup`    |
| POST   | `/api/v1/auth/login`              | `localhost:8081/api/v1/auth/login`      | `localhost:8080/account-service/api/v1/auth/login`     |
| POST   | `/api/v1/ride/request`            | `localhost:8083/api/v1/ride/request`    | `localhost:8080/ride-service/api/v1/ride/request`      |
| GET    | `/api/v1/ride/{id}`               | `localhost:8083/api/v1/ride/42`         | `localhost:8080/ride-service/api/v1/ride/42`           |
| POST   | `/api/v1/ride/{id}/cancel`        | `localhost:8083/api/v1/ride/42/cancel`  | `localhost:8080/ride-service/api/v1/ride/42/cancel`    |
| GET    | `/api/v1/ride/{id}/cancellation`  | `localhost:8083/api/v1/ride/42/cancellation` | `localhost:8080/ride-service/api/v1/ride/42/cancellation` |

## POST /api/v1/auth/signup

Creates a user. Password is BCrypt-hashed before persistence.

**Request**

```http
POST /account-service/api/v1/auth/signup HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{"name": "Virul", "email": "virul@gmail.com", "password": "123"}
```

**Response — `200 OK`** (a `UserResponse` projection: the hash and the Spring Security
`UserDetails` fields are deliberately not serialised)

```json
{
  "id": 1,
  "name": "Virul",
  "email": "virul@gmail.com",
  "role": "PASSENGER"
}
```

**Errors**

| Status | When                                                        |
| ------ | ----------------------------------------------------------- |
| `400`  | Malformed JSON body                                         |
| `405`  | Wrong HTTP method (e.g. `GET` on signup)                    |
| `500`  | Missing/blank required field hitting a `NOT NULL` column, or duplicate `email` (unique constraint) |

## POST /api/v1/auth/login

Accepts `email` + `password`, authenticates with Spring Security, and returns a signed JWT
as a raw `text/plain` string (not a JSON object, so there is no `token` field to unwrap). The
token carries the account role as a claim and expires after 24 hours
(`app.jwt.expiration`). Send it on later requests as `Authorization: Bearer <token>`, or
paste the raw value into the **Authorize** dialog in Swagger UI.

Invalid credentials are rejected by the `AuthenticationManager` and surface as `403` rather
than `401`, because no `AuthenticationEntryPoint` is registered on the security chain.

**Request**

```http
POST /account-service/api/v1/auth/login HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{"email": "virul@gmail.com", "password": "123"}
```

**Response — `200 OK`**

```
eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiUFFTU0VOR0VSIiwic3ViIjoidmlydWxAZ21haWwuY29tIn0....
```

## Ride endpoints

All four ride endpoints read the caller's account id from an `X-User-Id` header, which is a
**placeholder for real auth**: nothing verifies the header yet, so any client can claim any
account. It is a header rather than a body field so a client cannot request or read a ride on
somebody else's behalf once the gateway/JWT filter starts populating it.

## POST /api/v1/ride/request

Registers a ride request and returns the persisted ride, so the caller gets back the `id` to
track the request with. `requestedTime` is stamped by the server, so a client cannot backdate a
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
  "startTime": null,
  "completedTime": null,
  "cancelledAt": null,
  "cancelledBy": null
}
```

**Errors**

| Status | When                                                       |
| ------ | ---------------------------------------------------------- |
| `400`  | Missing or non-positive `X-User-Id`, or a blank location / non-positive `tripDistance` |
| `409`  | The passenger already has a ride that is `SEARCHING` or `ONGOING` |

## GET /api/v1/ride/{id}

Reads a single ride back — the poll target for the `id` the create call returns. Scoped to the
owning passenger.

**Response — `200 OK`**, same shape as the create call.

**Errors**

| Status | When                                       |
| ------ | ------------------------------------------ |
| `404`  | No ride carries that id                    |
| `403`  | The ride belongs to another passenger      |

## POST /api/v1/ride/{id}/cancel

Cancels a ride and records the cancellation. The caller must be the passenger who requested the
ride or the driver assigned to it, and the state each of them is allowed to cancel from differs:

| Caller                | Cancellable from    | Reasoning                                   |
| --------------------- | ------------------- | ------------------------------------------- |
| Passenger             | `SEARCHING`, `ONGOING` | Allowed until the ride is `COMPLETED`    |
| Assigned driver       | `SEARCHING`         | Locked in once the trip is `ONGOING`         |
| Anyone else           | —                   | `403`, including an unassigned driver        |

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
| `400`  | Missing or non-positive `X-User-Id`, or a `reason` over 500 characters |
| `404`  | No ride carries that id                                          |
| `403`  | The caller is neither the passenger nor the assigned driver      |
| `409`  | The ride is `COMPLETED`, already `CANCELLED`, or `ONGOING` when a driver cancels |

The ride is read under a `PESSIMISTIC_WRITE` lock, so two concurrent cancels cannot both see a
cancellable state: the second one blocks, then reads the `CANCELLED` state the first committed
and is rejected with `409`. The `ride_cancellations` table also has a unique index on `ride_id`,
so a ride can carry at most one cancellation row.

## GET /api/v1/ride/{id}/cancellation

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

## Bruno collection

`api-tests/` contains a [Bruno](https://www.usebruno.com/) collection
(`opencollection.yml`) with an `account-service` folder:

- `health check (public endpoint)` — `GET localhost:8080/actuator/health`
- `signup` — `POST localhost:8080/account-service/api/v1/auth/signup` with
  `{name, email, password}`
- `login` — currently also targets `/api/v1/auth/signup` (artifact to fix); the intended
  endpoint is `POST localhost:8081/api/v1/auth/login` with `{email, password}`