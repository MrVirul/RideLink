# API Gateway

The gateway is Spring Cloud Gateway **Server Web MVC**
(`spring-cloud-starter-gateway-server-webmvc:5.0.3` within Spring Cloud 2025.1.3) — the
servlet-based implementation of Spring Cloud Gateway. All route configuration lives in
`api-gateway/src/main/resources/application.properties`.

## Why the WebMVC gateway

- The project deliberately uses the servlet flavour, so the gateway runs on a standard
  port 8080 servlet container and integrates with the same starter patterns as the other
  services.
- Properties use the **`spring.cloud.gateway.server.webmvc.*`** prefix.
- The classic reactive gateway properties
  (`spring.cloud.gateway.discovery.locator.enabled`, `spring.cloud.gateway.routes.*`, etc.)
  are **not** read by this gateway. In particular, **automatic discovery-based routing does
  not apply** — every route must be declared explicitly.

## Route model

A route is defined by:

| Property group | Meaning                                                        |
| -------------- | -------------------------------------------------------------- |
| `id`           | Unique route name                                               |
| `uri`          | Target; `lb://<APPLICATION-NAME>` resolves via the load balancer from Eureka |
| `predicates[]` | Conditions for matching a request; `name` = predicate bean, `args` keyed by parameter |
| `filters[]`    | Modifiers applied to matched requests; `name` = filter bean, `args` keyed by parameter |

Predicate/filter configuration binds to Java method names. The route table uses:

- **`path`** predicate (`GatewayRequestPredicates.path(String... patterns)`), with arg key
  `patterns`.
- **`stripPrefix`** filter (`BeforeFilterFunctions`), with arg key `parts` — removes `N`
  leading path segments before forwarding. `parts=1` strips the service name prefix.

## Route table

All business services are registered under their application name (uppercase as shown in
Eureka).

| Route ID        | Predicate pattern          | Target URI           | Strip |
| --------------- | -------------------------- | -------------------- | ----- |
| `account-service` | `/account-service/**`     | `lb://ACCOUNT-SERVICE` | 1     |
| `driver-service`  | `/driver-service/**`      | `lb://DRIVER-SERVICE`  | 1     |
| `ride-service`    | `/ride-service/**`        | `lb://RIDE-SERVICE`    | 1     |
| `fare-service`    | `/fare-service/**`        | `lb://FARE-SERVICE`    | 1     |

Example routing: `POST /account-service/api/v1/auth/signup` → `stripPrefix(1)` removes
`/account-service` → forwarded to `lb://ACCOUNT-SERVICE` as `POST /api/v1/auth/signup`.

## Adding a new service

To expose a new `foo-service` through the gateway, append:

```properties
spring.cloud.gateway.server.webmvc.routes[next].id=foo-service
spring.cloud.gateway.server.webmvc.routes[next].uri=lb://FOO-SERVICE
spring.cloud.gateway.server.webmvc.routes[next].predicates[0].name=path
spring.cloud.gateway.server.webmvc.routes[next].predicates[0].args[patterns]=/foo-service/**
spring.cloud.gateway.server.webmvc.routes[next].filters[0].name=stripPrefix
spring.cloud.gateway.server.webmvc.routes[next].filters[0].args[parts]=1
```

where `[next]` is the next array index (currently 0–3 are used). Then:

1. The service must register with Eureka under the application name `FOO-SERVICE`
   (`spring.application.name=foo-service`, registration already enabled by default config).
2. The target port must accept the **stripped** path (no `/foo-service` prefix).

## Health

- Gateway health: `http://localhost:8080/actuator/health` (permitted without auth).
- A forwarding failure surfaces as the **gateway's own** error status, **not** the
  downstream service's status. In practice this happens when the `lb` URI cannot resolve:
  with Eureka down (or before the downstream service registers) the gateway responds
  `500 Internal Server Error` while Eureka/load-balancer logs
  `No servers available for service: <NAME>` (backed by `503 Unable to find instance`).

## Troubleshooting routing issues

| Symptom                           | Likely cause                                            |
| --------------------------------- | ------------------------------------------------------- |
| `404` for `/account-service/...`  | Route not registered (wrong/legacy property namespace, discovery locator assumed) |
| `500` + `No servers available for service: ACCOUNT-SERVICE` in gateway log | Eureka down or service not yet registered |
| `405/400` with `/account-service/...` in the error body | The request *did* reach the target service (prefix was stripped before forwarding) |

Restart order matters. If services started while Eureka was down, their local registry cache
is empty until the next discovery refresh; a gateway restart (or waiting ~30 s) resolves it.
See [Operations](operations.md) for the full startup procedure.