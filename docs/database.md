# Database

Each service has **its own dedicated PostgreSQL database**, hosted on Neon. Databases are
configured per service and loaded from a git-ignored local properties file.

## Connection model

Every business service (`account-`, `driver-`, `ride-`, `fare-service`) sets, in its
`application.properties`:

```properties
spring.config.import=optional:classpath:env.properties
spring.datasource.url=${<SERVICE>_DB_HOST}
spring.datasource.username=${<SERVICE>_DB_USER}
spring.datasource.password=${<SERVICE>_DB_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver
```

- `env.properties` lives at `src/main/resources/env.properties` and is **git-ignored**
  (secrets). A `.env.properties` example with placeholder values is kept at the repo root.
- The `optional:` import means startup does not fail if the file is missing (the app will
  fail at datasource initialisation instead).

| Service           | Env var prefix | Example defaults file                    |
| ----------------- | -------------- | ---------------------------------------- |
| `account-service` | `ACCOUNT_DB_*` | `account-service/src/main/resources/env.properties` |
| `driver-service`  | `DRIVER_DB_*`  | `driver-service/src/main/resources/env.properties`  |
| `ride-service`    | `RIDE_DB_*`    | `ride-service/src/main/resources/env.properties`    |
| `fare-service`    | `FARE_DB_*`    | `fare-service/src/main/resources/env.properties`    |

The `api-gateway` and `service-registry` have no database and no `env.properties`.

JPA/Hibernate settings (identical across services):

```properties
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

`ddl-auto=update` means Hibernate creates/alters tables from the entities on startup — no
SQL migrations are applied.

## CI variant

`.github/workflows/ci.yml` boots a `postgres:16-alpine` service container and points all
four `*_DB_*` variable groups at the single `ridelink_test` database, plus a test
`JWT_SECRET`, then runs `./mvnw -B clean verify`.

## Schema

### `account-service` — `users` table (from `User` entity)

| Column | Type / constraints          | Notes                          |
| ------ | --------------------------- | ------------------------------ |
| `id`   | IDENTITY (serial bigint)    | Assigned on insert             |
| `name` | `VARCHAR NOT NULL`          |                                |
| `email`| `VARCHAR NOT NULL UNIQUE`   | Principal for auth             |
| `password` | `VARCHAR NOT NULL`      | BCrypt hash                    |

The `driver-`, `ride-`, and `fare-service` modules have datasource configuration but no JPA
entities yet, so they create no tables yet.

## Pointing at a real Neon DB

Create each service's `env.properties`:

```properties
# account-service/src/main/resources/env.properties
ACCOUNT_DB_HOST=jdbc:postgresql://<host>:5432/<database>?sslmode=require
ACCOUNT_DB_USER=<user>
ACCOUNT_DB_PASSWORD=<password>
```

> **Never commit `env.properties`** — files matching `.env*` are git-ignored.