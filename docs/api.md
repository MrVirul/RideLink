# API Reference

All endpoints can be reached two ways:

- **Direct**: `http://localhost:<port>/<path>` (bypasses the gateway)
- **Via gateway**: `http://localhost:8080/<service-name>/<path>` (gateway strips the
  `<service-name>/` prefix via `stripPrefix(1)` before forwarding)

Only the account service exposes business endpoints today (`driver-service`,
`ride-service`, `fare-service` are scaffolded and expose nothing yet). Every service exposes
the Spring Boot Actuator endpoints below.

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

The two public operations under `/api/v1/auth` carry an empty `@SecurityRequirements` and
therefore opt out — no lock icon, no token needed. New endpoints are secured in the spec by
default; annotate a genuinely public one with an empty `@SecurityRequirements`.

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

## Bruno collection

`api-tests/` contains a [Bruno](https://www.usebruno.com/) collection
(`opencollection.yml`) with an `account-service` folder:

- `health check (public endpoint)` — `GET localhost:8080/actuator/health`
- `signup` — `POST localhost:8080/account-service/api/v1/auth/signup` with
  `{name, email, password}`
- `login` — currently also targets `/api/v1/auth/signup` (artifact to fix); the intended
  endpoint is `POST localhost:8081/api/v1/auth/login` with `{email, password}`