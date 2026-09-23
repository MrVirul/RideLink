# API Reference

All endpoints can be reached two ways:

- **Direct**: `http://localhost:<port>/<path>` (bypasses the gateway)
- **Via gateway**: `http://localhost:8080/<service-name>/<path>` (gateway strips the
  `<service-name>/` prefix via `stripPrefix(1)` before forwarding)

Only the account service exposes business endpoints today (`driver-service`,
`ride-service`, `fare-service` are scaffolded and expose nothing yet). Every service exposes
the Spring Boot Actuator endpoints below.

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

**Response — `200 OK`** (currently returns the persisted entity, including the hashed
password):

```json
{
  "id": 1,
  "name": "Virul",
  "email": "virul@gmail.com",
  "password": "$2a$10$ncozqXKp9N9j4cn3CMtQm.kiJbQTa1U7KqBPA31napOGfAz37LaHO",
  "accountNonExpired": true,
  "accountNonLocked": true,
  "authorities": [],
  "credentialsNonExpired": true,
  "enabled": true,
  "username": "virul@gmail.com"
}
```

**Errors**

| Status | When                                                        |
| ------ | ----------------------------------------------------------- |
| `400`  | Malformed JSON body                                         |
| `405`  | Wrong HTTP method (e.g. `GET` on signup)                    |
| `500`  | Missing/blank required field hitting a `NOT NULL` column, or duplicate `email` (unique constraint) |

## POST /api/v1/auth/login

Accepts `email` + `password`, authenticates with Spring Security, and currently returns the
placeholder string `JWT_TOEKN Generated Successfully` (real JWT generation is implemented in
`JwtService` but not yet connected). Invalid credentials produce an exception that surfaces
as `500`.

**Request**

```http
POST /account-service/api/v1/auth/login HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{"email": "virul@gmail.com", "password": "123"}
```

**Response — `200 OK`**

```
JWT_TOEKN Generated Successfully
```

## Bruno collection

`api-tests/` contains a [Bruno](https://www.usebruno.com/) collection
(`opencollection.yml`) with an `account-service` folder:

- `health check (public endpoint)` — `GET localhost:8080/actuator/health`
- `signup` — `POST localhost:8080/account-service/api/v1/auth/signup` with
  `{name, email, password}`
- `login` — currently also targets `/api/v1/auth/signup` (artifact to fix); the intended
  endpoint is `POST localhost:8081/api/v1/auth/login` with `{email, password}`