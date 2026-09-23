# Operations

How to run, monitor, and troubleshoot the RideLink platform.

## Prerequisites

- JDK 21
- Maven (optional — the included wrapper `./mvnw` downloads Maven on first run)
- `curl` and `nc` (used by the helper scripts)

## Starting everything (recommended)

```bash
./startup.sh
```

- Starts services in dependency order (Eureka → gateway → business services), waits for
  health checks, prints a summary with PIDs, and reports database connection status (from
  the `HikariPool ... Start completed` log line).
- Press **Ctrl+C** to stop all services cleanly.
- Logs go to `logs/<service>.log` (e.g. `logs/account service.log`, `logs/eureka server.log`).

## Monitoring

```bash
./monitor.sh
```

Real-time dashboard showing health, PID, CPU, memory, uptime and recent logs for each
service. Keys: `R` refresh · `D` health details · `L` logs · `Q` quit.

## Manual start (correct order)

```bash
cd service-registry && ./mvnw spring-boot:run   # 8761 first!
cd api-gateway      && ./mvnw spring-boot:run   # 8080
cd account-service  && ./mvnw spring-boot:run   # 8081 (then driver/ride/fare)
```

## Stopping

```bash
pkill -f spring-boot:run           # stop all
lsof -ti :8081 | xargs kill -9     # stop one service by port
```

## Health and discovery

| Check                                                            | Purpose                        |
| ---------------------------------------------------------------- | ------------------------------ |
| `http://localhost:8761/`                                         | Eureka dashboard / registry    |
| `http://localhost:8761/eureka/apps`                              | Registry JSON (all instances)  |
| `http://localhost:<port>/actuator/health`                        | Health incl. DB                |
| `http://localhost:<port>/actuator/info`                          | Service info                   |

```bash
curl http://localhost:8761/eureka/apps   # confirm ACCOUNT-SERVICE etc. are present
```

## Troubleshooting

### 403 Forbidden on an API call

If you see `403` on what should be a public endpoint:

1. Check it is under `/api/v1/auth/**`, `/actuator/health/**`, `/actuator/info/**`, or
   `/error` — everything else is `authenticated` and needs a JWT.
2. Historically, a locked-down `/error` masked real HTTP errors (400/405/500) as bare 403.
   `/error` is now `permitAll`, so genuine status codes surface directly. A 403 today means
   an endpoint that is just not public.

### Gateway returns 404 for `/account-service/...`

Routes must be declared under `spring.cloud.gateway.server.webmvc.routes[]` (the WebMVC
gateway ignores reactive `discovery.locator.*`/`routes` properties). See [Gateway](gateway.md).

### Gateway returns 500 with "No servers available for service: ACCOUNT-SERVICE"

Eureka is down or the target service has not (re)registered. Verify Eureka is up and the
service shows in `http://localhost:8761/eureka/apps`. If services started while Eureka was
down, their registry cache is stale — wait ~30 s for the next discovery refresh or restart
the gateway (starting order: Eureka first).

### 500 on signup with a "duplicate key / unique constraint" error

The `users.email` column is unique. Use a fresh email (or clean the DB). Similarly, missing
`name`/`email`/`password` in the JSON body violates `NOT NULL` and also ends as a 500 —
use `@Valid` on the controller to get a clean 400.

### A route matched but the request still fails

When the error body's `path` field contains the `/account-service/...` prefix it comes from
the *gateway*; when it's the plain `/api/v1/auth/signup` path it originates from the target
service. This distinguishes proxy failures from downstream failures.

## Logs

- Location: `logs/` (git-ignored).
- Per-service files mirror the names in `startup.sh`:
  `eureka server.log`, `api gateway.log`, `account service.log`, `driver service.log`,
  `ride service.log`, `fare service.log`.
- Useful markers:
  - `HikariPool-1 - Start completed` → DB connected.
  - `No servers available for service: ...` → load balancer could not resolve an instance.
  - `registration failed Cannot execute request on any known server` → Eureka unreachable.