# Authentication & Security (account-service)

Security is implemented only in `account-service`. It uses Spring Security with a stateless
JWT flow (JJWT) and optionally also operates via the DAO `AuthenticationManager` for the
login path.

## Data model

`User` (`com.ridelink.account_service.model.User`) is a JPA entity mapped to the `users`
table and implements Spring Security's `UserDetails`:

| Field      | Column       | Constraints                        | Notes                          |
| ---------- | ------------ | ---------------------------------- | ------------------------------ |
| `id`       | `id`         | `@GeneratedValue(IDENTITY)`        | `Integer` (nullable/unknownable at request time) |
| `name`     | `name`       | `NOT NULL`                         | Body field                      |
| `email`    | `email`      | `NOT NULL`, `UNIQUE`, `@Email`     | Used as the security principal  |
| `password` | `password`   | `NOT NULL`                         | Stored BCrypt-hashed            |

`UserDetails` mapping:

- `getUsername()` → `email`
- `getPassword()` → stored (hashed) password
- `getAuthorities()` → empty list
- Account state flags → all `true`

## Security configuration (`security/SecurityConfig.java`)

- CSRF disabled; session policy **STATELESS**.
- `JwtAuthenticationFilter` runs before `UsernamePasswordAuthenticationFilter`.
- `DaoAuthenticationProvider` backed by `UserService` (loads `UserDetails` by email) and a
  `BCryptPasswordEncoder`.
- `AuthenticationManager` exposed from `AuthenticationConfiguration`.

### Public vs protected paths

| Path | Access |
| ---- | ------ |
| `/api/v1/auth/**` | `permitAll` (signup/login) |
| `/actuator/health/**`, `/actuator/info/**` | `permitAll` |
| `/error` | `permitAll` |
| everything else | `authenticated` (requires valid `Authorization: Bearer <jwt>`) |

> `/error` is explicitly `permitAll`. Because Spring Boot re-dispatches to `/error` when an
> exception occurs, a locked-down `/error` used to mask genuine HTTP errors (e.g. 400, 405)
> as bare `403 Forbidden` responses on the public endpoints. Keeping `/error` open lets the
> real status codes surface.

## JWT filter chain (`security/JwtAuthenticationFilter.java`)

1. Reads `Authorization: Bearer <jwt>` header; no header → pass through unchanged.
2. Extracts the subject (email) via `JwtService.extractUsername`.
3. If subject present and no auth already in context, loads `UserDetails`, validates token
   (`JwtService.isTokenValid`) and sets a `UsernamePasswordAuthenticationToken` in the
   `SecurityContextHolder`.

## `JwtService`

- Secret: `app.jwt.secret` (Base64-encoded; must decode to ≥ 256 bits for HS256/384/512).
  Provided via the `JWT_SECRET` environment variable (see `env.properties`).
- Validity: `app.jwt.expiration=86400000` ms (24 hours).
- `generateToken(...)`, `extractUsername(...)`, `isTokenValid(...)` implemented with JJWT's
  `Jwts.builder()` / `Jwts.parser()` API.

## Auth endpoints (`cotroller/AuthController.java`)

> Note: the package is literally named `...cotroller` (typo in the current tree).

### `POST /api/v1/auth/signup`

Request body is a `User` JSON object:

```json
{
  "name": "Virul",
  "email": "virul@gmail.com",
  "password": "123"
}
```

Flow: `AuthService.registerUser()` BCrypt-encodes the password, then saves via the
repository. Response `200 OK` returns the saved entity (including the hashed `password` and
`id`). A duplicate `email` violates the unique constraint and results in `500`.

### `POST /api/v1/auth/login`

Request body:

```json
{
  "email": "virul@gmail.com",
  "password": "123"
}
```

Flow: `AuthService.authenticateAndGetToken()` attempts `authenticationManager.authenticate()`
(BCrypt verified via the DAO provider). If successful it currently returns the placeholder
string `"JWT_TOEKN Generated Successfully"` — the real token generation in `JwtService`
(`generateToken`) is implemented but **not yet wired into the login response**. Invalid
credentials throw `RuntimeException` (surfaces as `500`).

## Current gaps / known issues

- Login does **not** return a real JWT yet (see above).
- No `@Valid`/validation annotations on the controller methods; empty or partially-missing
  bodies either fail Jackson binding (`400`) or error at the database layer (`500`).
- The response includes the hashed password (no `@JsonIgnore`/DTO).