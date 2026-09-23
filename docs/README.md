# RideLink Technical Documentation

Deep-dive technical documentation for the RideLink backend microservices monorepo.

| Document | Contents |
| -------- | -------- |
| [Architecture](architecture.md) | Service topology, request flow through the gateway, discovery & load balancing |
| [API Gateway](gateway.md) | WebMVC gateway routing model, route table, adding a new service |
| [Authentication & Security](authentication.md) | JWT flow, Spring Security configuration, signup/login behaviour |
| [API Reference](api.md) | Contracts for the HTTP endpoints (request/response payloads) |
| [Database](database.md) | Per-service PostgreSQL setup, environment variables, schema |
| [Operations](operations.md) | Running, monitoring, logs, and troubleshooting |