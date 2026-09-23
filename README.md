# RideLink

Backend-only microservices monorepo for the **Application Development** university assignment.

RideLink is a ride-hailing platform backend built as a set of independently deployable
Spring Boot microservices that register with a Netflix Eureka service registry and are
routed through a single API Gateway.

This repository is a backend-only microservices monorepo. `account-service` is fully
implemented (auth + JWT); `driver-service`, `ride-service`, and `fare-service` are
scaffolded and start cleanly but have no business endpoints yet.

**Deep-dive technical documentation:** [`docs/`](docs/README.md) — architecture, gateway
routing, authentication, API reference, database, and operations/troubleshooting.

## Architecture

```
                    ┌─────────────────────┐
   Clients ───────► │   API Gateway :8080 │
                    └──────────┬──────────┘
                               │ (routes via Eureka)
          ┌────────────────────┼────────────────────┐
          │                    │                    │
          ▼                    ▼                    ▼
┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐
│ Account :8081    │   │ Driver  :8082    │   │ Ride    :8083    │
│ (users, JWT)     │   │ (drivers)        │   │ (rides)          │
└────────┬────────┘   └────────┬────────┘   └────────┬────────┘
         │                    │                    │
         ▼                    ▼                    ▼
┌─────────────────────────────────────────────────────────────┐
│              Fare Service :8084  (fare calculation)          │
└─────────────────────────────────────────────────────────────┘
                         ▲
                         │
              ┌─────────────────────┐
              │  PostgreSQL (Neon)  │  one dedicated DB per service
              └─────────────────────┘

```

| Service            | Port | Database (env vars) | Purpose                                   |
| ------------------ | ---- | ------------------- | ----------------------------------------- |
| `service-registry` | 8761 | —                   | Netflix Eureka server (service discovery) |
| `api-gateway`      | 8080 | —                   | Spring Cloud Gateway (single entry point) |
| `account-service`  | 8081 | `ACCOUNT_DB_*`      | User accounts & JWT authentication        |
| `driver-service`   | 8082 | `DRIVER_DB_*`       | Driver profiles & fleet                   |
| `ride-service`     | 8083 | `RIDE_DB_*`         | Ride lifecycle management                 |
| `fare-service`     | 8084 | `FARE_DB_*`         | Fare calculation logic                    |

## Tech Stack

- **Java 21**
- **Spring Boot 4.1.1**
- **Spring Cloud 2025.1.3** (Eureka, OpenFeign, Gateway)
- **Spring Cloud Gateway Server Web MVC** — servlet (WebMVC) flavour of the gateway;
  routes are declared under `spring.cloud.gateway.server.webmvc.routes[]` (see `docs/gateway.md`)
- **Spring Data JPA** + **Hibernate**
- **Spring Security** (account-service only, for JWT)
- **PostgreSQL** (hosted on Neon) — one dedicated database per service
- **Maven** (via the included Maven Wrapper `./mvnw`) with Lombok

## Prerequisites

- **JDK 21** (`java -version` → 21)
- **Maven** — optional; the wrapper (`./mvnw`) downloads Maven on first run
- **`curl` and `nc`** — required by the helper scripts (`startup.sh`, `monitor.sh`)

## Project Setup

### 1. Clone the repository

```bash
git clone git@github.com:MrVirul/RideLink.git
cd RideLink
```

### 2. Configure environment variables

The services resolve database (and JWT) settings from **git-ignored `env.properties`**
files, one per service, located at `src/main/resources/env.properties`. They are excluded
from version control on purpose — **never commit them** or expose your credentials.

Each service loads its file with:

```properties
spring.config.import=optional:classpath:env.properties
```

Create the following files and fill in your own values.

#### `account-service/src/main/resources/env.properties`

```properties
ACCOUNT_DB_HOST=jdbc:postgresql://<host>:5432/<database>?sslmode=require
ACCOUNT_DB_USER=<user>
ACCOUNT_DB_PASSWORD=<password>
JWT_SECRET=<strong-random-secret-at-least-256-bits>
```

#### `driver-service/src/main/resources/env.properties`

```properties
DRIVER_DB_HOST=jdbc:postgresql://<host>:5432/<database>?sslmode=require
DRIVER_DB_USER=<user>
DRIVER_DB_PASSWORD=<password>
```

#### `ride-service/src/main/resources/env.properties`

```properties
RIDE_DB_HOST=jdbc:postgresql://<host>:5432/<database>?sslmode=require
RIDE_DB_USER=<user>
RIDE_DB_PASSWORD=<password>
```

#### `fare-service/src/main/resources/env.properties`

```properties
FARE_DB_HOST=jdbc:postgresql://<host>:5432/<database>?sslmode=require
FARE_DB_USER=<user>
FARE_DB_PASSWORD=<password>
```

> `api-gateway` and `service-registry` have no database and do not need an `env.properties`.
>
> Generate the JWT secret locally (macOS/Linux):
>
> ```bash
> openssl rand -base64 64
> ```
>
> A commented-out reference file (`.env.properties`) with example values is kept at the
> repository root for convenience. It is git-ignored.

### 3. Build

Build the whole project from the repository root:

```bash
./mvnw clean install
```

Or build a single service only:

```bash
./mvnw -pl account-service clean install
```

## Running the Project

### Option A — Automated startup script (recommended)

The `startup.sh` script starts every service in the correct order, waits for
health checks, prints a summary, and stores logs in `logs/`.

```bash
./startup.sh
```

- Press **Ctrl+C** to stop all services cleanly.
- Service logs are written to `logs/<service-name>.log`.

### Option B — Monitor dashboard

While services are running, open the real-time status dashboard (health, PID, CPU,
memory, uptime, logs):

```bash
./monitor.sh
```

- `R` refresh · `D` health details · `L` logs · `Q` quit

### Option C — Start services manually

**Startup order matters.** Eureka must be up before the other services register.

1. **Service Registry**

   ```bash
   cd service-registry && ./mvnw spring-boot:run
   ```

2. **API Gateway**

   ```bash
   cd api-gateway && ./mvnw spring-boot:run
   ```

3. **Business services** (in any order; each registers with Eureka)
   ```bash
   cd account-service && ./mvnw spring-boot:run
   cd driver-service  && ./mvnw spring-boot:run
   cd ride-service    && ./mvnw spring-boot:run
   cd fare-service    && ./mvnw spring-boot:run
   ```

### Stopping the services

```bash
pkill -f spring-boot:run
```

The startup script already handles this on Ctrl+C. You can also kill by port:

```bash
lsof -ti :8081 | xargs kill -9   # e.g. stop account-service
```

## Verification & Health

Every service exposes Spring Boot Actuator health endpoints:

| URL                                        | Description                            |
| ------------------------------------------ | -------------------------------------- |
| `http://localhost:8761/`                   | Eureka dashboard (registered services) |
| `http://localhost:<port>/actuator/health`  | Health check (includes DB status)      |
| `http://localhost:<port>/actuator/info`    | Service information                    |
| `http://localhost:<port>/actuator/metrics` | Runtime metrics                        |

```bash
curl http://localhost:8081/actuator/health
# {"status":"UP","components":{...,"db":{"status":"UP",...}}}
```

## Testing

Run tests for the whole project:

```bash
./mvnw test
```

For a single service:

```bash
./mvnw -pl account-service test
```

## Project Structure

```
RideLink/
├── pom.xml                     # Parent POM (aggregates all modules)
├── mvnw / mvnw.cmd             # Maven wrapper
├── startup.sh                  # Start all microservices in order (Ctrl+C to stop)
├── monitor.sh                  # Real-time service dashboard
├── service-registry/           # Eureka server
├── api-gateway/                # Spring Cloud Gateway (WebMVC)
├── account-service/            # Auth, users, JWT (implemented)
├── driver-service/             # Drivers (scaffolded)
├── ride-service/               # Rides (scaffolded)
├── fare-service/               # Fares (scaffolded)
├── api-tests/                  # Bruno API collection
└── docs/                       # Technical documentation (see docs/README.md)
```

Each module is a standard Maven project:

```
<service>/
├── pom.xml
└── src/main/
    ├── java/com/ridelink/<service>/   # application code
    └── resources/
        ├── application.properties     # committed service config
        └── env.properties             # git-ignored secrets (DB, JWT)
```

## Configuration & Secrets

- **Never commit** `env.properties`, `.env.properties`, or any file containing credentials
  or keys. All such files are covered by `.gitignore`.
- Customize non-secret settings (ports, log levels, JWT expiration) in each service's
  `application.properties`.
- The JWT secret is configured only for `account-service` via `app.jwt.secret=${JWT_SECRET}`;
  default token validity is 24 hours (`app.jwt.expiration=86400000`).

## Contributing

See [`CONTRIBUTING.md`](CONTRIBUTING.md) for the GitHub Flow workflow, branch naming
(`feature/<username>/<task-name>`), CommitLint-style (Conventional Commits) messages, and
the Definition of Done checklist.
