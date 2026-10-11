# Ride Service API

Endpoint reference for `ride-service` (port `8083`). Through the API gateway
(`http://localhost:8080`) every path is prefixed with `/ride-service`, which is stripped before
forwarding. The examples below use the gateway URLs.

- **Base path:** `/api/v1/ride`
- **Controller:** `ride-service/src/main/java/com/ridelink/ride_service/controller/RideController.java`
- **DTOs:** `ride-service/src/main/java/com/ridelink/ride_service/dto/RideDto.java`

## Authentication

There is no Spring Security in this service. Every endpoint except `GET /{id}/status` requires a
placeholder `X-User-Id` header naming the calling account. The value is **not verified** — any
client can claim any account id. Missing or non-positive values yield `400`.

Send `Content-Type: application/json` on requests with a body.

## Ride states

`Status` enum: `SEARCHING`, `ASSIGNED`, `ONGOING`, `COMPLETED`, `CANCELLED`.

```
SEARCHING ──assign──▶ ASSIGNED ──start──▶ ONGOING ──complete──▶ COMPLETED
    │                    │                  │
    └────────────────────┴──────────────────┴──cancel──▶ CANCELLED
```

## Endpoint index

| Method | Endpoint                          | `X-User-Id` | Success |
| ------ | --------------------------------- | ----------- | ------- |
| POST   | `/api/v1/ride/request`            | required    | `201`   |
| GET    | `/api/v1/ride/{id}`               | required    | `200`   |
| GET    | `/api/v1/ride/{id}/status`        | none        | `200`   |
| GET    | `/api/v1/ride/passenger/me`       | required    | `200`   |
| GET    | `/api/v1/ride/driver/me`          | required    | `200`   |
| POST   | `/api/v1/ride/{id}/assign`        | none        | `200`   |
| PATCH  | `/api/v1/ride/{id}/start`         | required    | `200`   |
| PATCH  | `/api/v1/ride/{id}/complete`      | required    | `200`   |
| POST   | `/api/v1/ride/{id}/cancel`        | required    | `200`   |
| GET    | `/api/v1/ride/{id}/cancellation`  | required    | `200`   |

---

## POST /api/v1/ride/request

Registers a ride request. `requestedTime` is stamped by the server and the ride starts in
`SEARCHING`.

**Request**

```http
POST /ride-service/api/v1/ride/request HTTP/1.1
Host: localhost:8080
Content-Type: application/json
X-User-Id: 1
```

```json
{
  "pickupLocation": "Colombo Fort Railway Station",
  "dropOffLocation": "Galle Face Green",
  "tripDistance": 7.4
}
```

| Field            | Type   | Required | Notes                                   |
| ---------------- | ------ | -------- | --------------------------------------- |
| `pickupLocation` | string | yes      | Must not be blank                       |
| `dropOffLocation`| string | yes      | Must not be blank                       |
| `tripDistance`   | double | yes      | Must be positive (km)                   |

**Response — `201 Created`** (with `Location: /api/v1/ride/42` header)

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
  "assignedAt": null,
  "startTime": null,
  "completedTime": null,
  "cancelledAt": null,
  "cancelledBy": null
}
```

**Errors**

| Status | When                                                                     |
| ------ | ------------------------------------------------------------------------ |
| `400`  | Missing/invalid `X-User-Id`, blank location, or non-positive `tripDistance` |
| `409`  | Passenger already has a ride in `SEARCHING`, `ASSIGNED` or `ONGOING`     |

---

## GET /api/v1/ride/{id}

Reads a single ride back. Scoped to the owning passenger.

**Request**

```http
GET /ride-service/api/v1/ride/42 HTTP/1.1
Host: localhost:8080
X-User-Id: 1
```

**Response — `200 OK`**

```json
{
  "id": 42,
  "passengerId": 1,
  "driverId": 9,
  "status": "ASSIGNED",
  "pickupLocation": "Colombo Fort Railway Station",
  "dropOffLocation": "Galle Face Green",
  "tripDistance": 7.4,
  "requestedTime": "2026-09-28T10:15:30",
  "assignedAt": "2026-09-28T10:17:04",
  "startTime": null,
  "completedTime": null,
  "cancelledAt": null,
  "cancelledBy": null
}
```

**Errors**

| Status | When                                  |
| ------ | ------------------------------------- |
| `404`  | No ride carries that id               |
| `403`  | The ride belongs to another passenger |

---

## GET /api/v1/ride/{id}/status

Returns a compact status view of any ride. This is the only endpoint that takes **no
`X-User-Id`** and is not scoped to a caller.

**Request**

```http
GET /ride-service/api/v1/ride/42/status HTTP/1.1
Host: localhost:8080
```

**Response — `200 OK`** (`updatedAt` is the most recent of the ride's timestamps)

```json
{
  "rideId": 42,
  "status": "ASSIGNED",
  "updatedAt": "2026-09-28T10:17:04",
  "assignedAt": "2026-09-28T10:17:04",
  "driverId": 9,
  "cancelledAt": null
}
```

**Errors**

| Status | When                    |
| ------ | ----------------------- |
| `404`  | No ride carries that id |

---

## GET /api/v1/ride/passenger/me

Lists the calling passenger's rides, newest request first.

**Query parameters** (all optional): `status` (enum name, case-insensitive), `page`, `size`.
Paging is applied only when **both** `page` and `size` are supplied.

**Request**

```http
GET /ride-service/api/v1/ride/passenger/me?status=SEARCHING&page=0&size=20 HTTP/1.1
Host: localhost:8080
X-User-Id: 1
```

**Response — `200 OK`**, a JSON array of rides

```json
[
  {
    "id": 42,
    "passengerId": 1,
    "driverId": null,
    "status": "SEARCHING",
    "pickupLocation": "Colombo Fort Railway Station",
    "dropOffLocation": "Galle Face Green",
    "tripDistance": 7.4,
    "requestedTime": "2026-09-28T10:15:30",
    "assignedAt": null,
    "startTime": null,
    "completedTime": null,
    "cancelledAt": null,
    "cancelledBy": null
  }
]
```

**Errors**

| Status | When                                        |
| ------ | ------------------------------------------- |
| `400`  | Missing `X-User-Id`, or an unknown `status` |

---

## GET /api/v1/ride/driver/me

Lists the calling driver's rides, newest assignment first.

**Query parameters**: same as `/passenger/me` (`status`, `page`, `size`).

**Request**

```http
GET /ride-service/api/v1/ride/driver/me?status=ASSIGNED HTTP/1.1
Host: localhost:8080
X-User-Id: 9
```

**Response — `200 OK`**, a JSON array of rides (same shape as above).

**Errors**

| Status | When                                        |
| ------ | ------------------------------------------- |
| `400`  | Missing `X-User-Id`, or an unknown `status` |

---

## POST /api/v1/ride/{id}/assign

Assigns the first available driver (from `driver-service` `GET /api/v1/driver/eligible`) to a
`SEARCHING` ride and transitions it to `ASSIGNED`. This is a dispatch operation and takes no
`X-User-Id` and no request body.

**Request**

```http
POST /ride-service/api/v1/ride/42/assign HTTP/1.1
Host: localhost:8080
```

**Response — `200 OK`**

```json
{
  "id": 42,
  "passengerId": 1,
  "driverId": 9,
  "status": "ASSIGNED",
  "pickupLocation": "Colombo Fort Railway Station",
  "dropOffLocation": "Galle Face Green",
  "tripDistance": 7.4,
  "requestedTime": "2026-09-28T10:15:30",
  "assignedAt": "2026-09-28T10:17:04",
  "startTime": null,
  "completedTime": null,
  "cancelledAt": null,
  "cancelledBy": null
}
```

**Errors**

| Status | When                                                                 |
| ------ | -------------------------------------------------------------------- |
| `404`  | No ride carries that id                                              |
| `409`  | The ride is not in `SEARCHING`, or no driver is currently available  |

The ride is read under a `PESSIMISTIC_WRITE` lock, so two concurrent assigns cannot both pick a
driver for the same ride.

---

## PATCH /api/v1/ride/{id}/start

Transitions `ASSIGNED → ONGOING`. Only the assigned driver may start the ride. No request body.

**Request**

```http
PATCH /ride-service/api/v1/ride/42/start HTTP/1.1
Host: localhost:8080
X-User-Id: 9
```

**Response — `200 OK`**

```json
{
  "id": 42,
  "passengerId": 1,
  "driverId": 9,
  "status": "ONGOING",
  "pickupLocation": "Colombo Fort Railway Station",
  "dropOffLocation": "Galle Face Green",
  "tripDistance": 7.4,
  "requestedTime": "2026-09-28T10:15:30",
  "assignedAt": "2026-09-28T10:17:04",
  "startTime": "2026-09-28T10:19:02",
  "completedTime": null,
  "cancelledAt": null,
  "cancelledBy": null
}
```

**Errors**

| Status | When                                  |
| ------ | ------------------------------------- |
| `404`  | No ride carries that id               |
| `403`  | The caller is not the assigned driver |
| `409`  | The ride is not in `ASSIGNED`         |

---

## PATCH /api/v1/ride/{id}/complete

Transitions `ONGOING → COMPLETED`. Either the passenger or the assigned driver may complete the
ride. No request body.

**Request**

```http
PATCH /ride-service/api/v1/ride/42/complete HTTP/1.1
Host: localhost:8080
X-User-Id: 9
```

**Response — `200 OK`**

```json
{
  "id": 42,
  "passengerId": 1,
  "driverId": 9,
  "status": "COMPLETED",
  "pickupLocation": "Colombo Fort Railway Station",
  "dropOffLocation": "Galle Face Green",
  "tripDistance": 7.4,
  "requestedTime": "2026-09-28T10:15:30",
  "assignedAt": "2026-09-28T10:17:04",
  "startTime": "2026-09-28T10:19:02",
  "completedTime": "2026-09-28T10:41:55",
  "cancelledAt": null,
  "cancelledBy": null
}
```

**Errors**

| Status | When                                               |
| ------ | -------------------------------------------------- |
| `404`  | No ride carries that id                            |
| `403`  | The caller is neither the passenger nor the driver |
| `409`  | The ride is not in `ONGOING`                       |

---

## POST /api/v1/ride/{id}/cancel

Cancels a ride and records a cancellation row. The body is optional; when given, `reason`
(max 500 characters) is stored for the audit trail.

| Caller          | Cancellable from                   |
| --------------- | ---------------------------------- |
| Passenger       | `SEARCHING`, `ASSIGNED`, `ONGOING` |
| Assigned driver | `SEARCHING`, `ASSIGNED`            |
| Anyone else     | `403`                              |

**Request**

```http
POST /ride-service/api/v1/ride/42/cancel HTTP/1.1
Host: localhost:8080
Content-Type: application/json
X-User-Id: 1
```

```json
{
  "reason": "Change of plans"
}
```

| Field    | Type   | Required | Notes             |
| -------- | ------ | -------- | ----------------- |
| `reason` | string | no       | Max 500 characters |

**Response — `200 OK`**

```json
{
  "id": 42,
  "passengerId": 1,
  "driverId": 9,
  "status": "CANCELLED",
  "pickupLocation": "Colombo Fort Railway Station",
  "dropOffLocation": "Galle Face Green",
  "tripDistance": 7.4,
  "requestedTime": "2026-09-28T10:15:30",
  "assignedAt": "2026-09-28T10:17:04",
  "startTime": null,
  "completedTime": null,
  "cancelledAt": "2026-09-28T10:22:10",
  "cancelledBy": 1
}
```

**Errors**

| Status | When                                                                                       |
| ------ | ------------------------------------------------------------------------------------------ |
| `400`  | Missing `X-User-Id`, or a `reason` over 500 characters                                     |
| `404`  | No ride carries that id                                                                    |
| `403`  | The caller is neither the passenger nor the assigned driver                                |
| `409`  | The ride is `COMPLETED`, already `CANCELLED`, or in a state the caller may not cancel from |

The ride is read under a `PESSIMISTIC_WRITE` lock, so concurrent cancels cannot both succeed;
the `ride_cancellations` table has a unique index on `ride_id`.

---

## GET /api/v1/ride/{id}/cancellation

Reads the cancellation history row. Visible to the passenger who requested the ride and to the
driver assigned to it.

**Request**

```http
GET /ride-service/api/v1/ride/42/cancellation HTTP/1.1
Host: localhost:8080
X-User-Id: 1
```

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

**Errors**

| Status | When                                                            |
| ------ | --------------------------------------------------------------- |
| `404`  | No ride carries that id, or the ride was never cancelled        |
| `403`  | The caller is neither the passenger nor the assigned driver     |

---

## Enums & types

### `Status`

`SEARCHING`, `ASSIGNED`, `ONGOING`, `COMPLETED`, `CANCELLED`

### `RideRequestResponse`

| Field            | Type               |
| ---------------- | ------------------ |
| `id`             | integer            |
| `passengerId`    | long               |
| `driverId`       | long / null        |
| `status`         | `Status`           |
| `pickupLocation` | string             |
| `dropOffLocation`| string             |
| `tripDistance`   | double             |
| `requestedTime`  | LocalDateTime      |
| `assignedAt`     | LocalDateTime/null |
| `startTime`      | LocalDateTime/null |
| `completedTime`  | LocalDateTime/null |
| `cancelledAt`    | LocalDateTime/null |
| `cancelledBy`    | long / null        |

### `RideStatusResponse`

| Field        | Type               |
| ------------ | ------------------ |
| `rideId`     | integer            |
| `status`     | `Status`           |
| `updatedAt`  | LocalDateTime      |
| `assignedAt` | LocalDateTime/null |
| `driverId`   | long / null        |
| `cancelledAt`| LocalDateTime/null |

### `CancellationResponse`

| Field            | Type               |
| ---------------- | ------------------ |
| `rideId`         | integer            |
| `previousStatus` | `Status`           |
| `cancelledBy`    | long               |
| `cancelledAt`    | LocalDateTime      |
| `reason`         | string / null      |
