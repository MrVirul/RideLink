# Architecture

RideLink is a backend-only, service-oriented monorepo built with Spring Boot and Spring
Cloud. It is organised as a set of independently deployable microservices that announce
themselves to a Netflix Eureka service registry and are reached by clients only through a
single API Gateway.

## Service topology

```
                        ┌──────────────────────┐
   Clients (HTTP) ────► │   api-gateway :8080  │
                        └──────────┬───────────┘
                                   │  `lb://SERVICE-NAME` via Eureka
            ┌──────────────────────┼───────────────────────┐
            │                      │                       │
            ▼                      ▼                       ▼
 ┌──────────────────┐   ┌──────────────────┐   ┌──────────────────┐
 │ account :8081    │   │ driver :8082      │   │ ride :8083        │
 │ (implemented)    │   │ (scaffolded)      │   │ (scaffolded)      │
 └────────┬─────────┘   └────────┬─────────┘   └────────┬─────────┘
          │                     │                       │
          ▼                     ▼                       ▼
 ┌────────────────────────────────────────────────────────────────┐
 │                        fare :8084                               │
 │                        (scaffolded)                             │
 └───────────────────────────────┬────────────────────────────────┘
                                  │
                                  ▼
        ┌─────────────────────────────────────────┐
        │  PostgreSQL (Neon) — one DB per service  │
        └─────────────────────────────────────────┘
```

| Service            | Port | Application name   | Purpose                                | Status          |
| ------------------ | ---- | ------------------ | -------------------------------------- | --------------- |
| `service-registry` | 8761 | `service-registry` | Eureka server (discovery)              | Implemented     |
| `api-gateway`      | 8080 | `api-gateway`      | Spring Cloud Gateway (single entry point) | Implemented  |
| `account-service`  | 8081 | `account-service`  | User accounts, auth, JWT               | Implemented     |
| `driver-service`   | 8082 | `driver-service`   | Driver profiles & fleet                | Scaffolded only |
| `ride-service`     | 8083 | `ride-service`     | Ride lifecycle                         | Scaffolded only |
| `fare-service`     | 8084 | `fare-service`     | Fare calculation                       | Scaffolded only |

> *Scaffolded* means the service directory, Maven build, Eureka registration and database
> wiring exist and start cleanly, but no business controllers/models are implemented yet.

## Core components

### Service registry (Eureka)

- The `service-registry` module is a standalone (`standalone` profile) Eureka server.
  It does **not** register with itself (`eureka.client.register-with-eureka=false`) and runs
  a single instance on `localhost:8761`.
- Every other service — including the gateway — fetches the registry from
  `http://localhost:8761/eureka/` (`eureka.client.service-url.defaultZone`).
- Dashboard: `http://localhost:8761/` (shows all registered application names).

### API Gateway

- Uses **Spring Cloud Gateway Server Web MVC**
  (`spring-cloud-starter-gateway-server-webmvc`). This is the *servlet* flavour of Spring
  Cloud Gateway, not the classic reactive (WebFlux) gateway.
- Routes are declared **explicitly** in `api-gateway/src/main/resources/application.properties`
  under the `spring.cloud.gateway.server.webmvc.routes[]` namespace. The legacy reactive
  property `spring.cloud.gateway.discovery.locator.*` is read by the WebFlux gateway only and
  is **ignored** here.
- Routes forward to `lb://<APPLICATION-NAME>` URIs. The `lb` scheme is resolved by the
  `LoadBalancerClient` against the Eureka registry, giving client-side load balancing across
  instances. See [Gateway](gateway.md).

### Account service (the reference implementation)

The account service is the only business service with real logic and acts as the template
for the remaining services:

- `AuthController` at `/api/v1/auth` exposes `POST /signup` and `POST /login`.
- JWT authentication (JJWT) + Spring Security, passwords hashed with BCrypt.
- Uses Spring Data JPA against a dedicated Postgres database.
- See [Authentication & Security](authentication.md) and [API Reference](api.md).

## Request flow (end to end)

```
Client
  │ POST /account-service/api/v1/auth/signup   (or directly :8081/api/v1/auth/signup)
  ▼
api-gateway:8080
  │ 1. Matches predicate path=/account-service/**  (gateway credentials NOT verified here)
  │ 2. stripPrefix(1) → forwards to /api/v1/auth/signup
  │ 3. Resolves lb://ACCOUNT-SERVICE via Eureka registry, picks an instance
  ▼
account-service:8081
  │ 4. Spring Security: /api/v1/auth/** is permitAll
  │ 5. @RequestBody User deserialized (Jackson)
  │ 6. AuthService.registerUser() → BCrypt-hash password → save via JPA
  ▼
Neon PostgreSQL (ACCOUNT_DB_* env vars)
```

## Technology stack

| Concern        | Choice                                                        |
| -------------- | ------------------------------------------------------------- |
| Language       | Java 21                                                       |
| Framework      | Spring Boot 4.1.1 (parent POM `org.springframework.boot`)     |
| Spring Cloud   | 2025.1.3 BOM (`spring-cloud-dependencies`)                    |
| Discovery      | `spring-cloud-starter-netflix-eureka-client` / `-server`      |
| Gateway        | `spring-cloud-starter-gateway-server-webmvc` (servlet flavour) |
| HTTP client    | `spring-cloud-starter-openfeign` (declared, not yet used)     |
| Persistence    | Spring Data JPA + Hibernate, `PostgreSQLDialect`              |
| Security (acc) | Spring Security, JJWT, BCrypt                                 |
| Validation     | `spring-boot-starter-validation` (declared; `@Valid` not yet wired in controllers) |
| Build          | Maven Wrapper (`./mvnw`), multi-module parent POM, Lombok      |
| Database       | PostgreSQL hosted on Neon — one dedicated database per service |

## Request/coverage caveats

- There is **no authentication/authorization at the gateway layer**; the gateway only
  routes. Protected endpoints authenticate inside the target service (currently only the
  account service enforces security, and only `/api/v1/auth/**` is public there).
- Because `stripPrefix` is applied on the gateway while Spring Security rules run on the
  *target* service, the security matchers use the stripped path (`/api/v1/auth/**`), not the
  gateway-prefixed path (`/account-service/api/v1/auth/**`).
- See [Operations](operations.md) for failure behaviour and troubleshooting.