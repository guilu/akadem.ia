# Downloads Specification

## Purpose

Covers the public PDF download endpoint `GET /api/v1/downloads/{token}` and the `Purchase` aggregate state model. This endpoint is the delivery mechanism for the digital product after a successful payment.

---

## Requirements

### Requirement: Download Endpoint — Public Access and Token Validation

`GET /api/v1/downloads/{token}` MUST be publicly accessible (no authentication required). The `{token}` path parameter MUST be treated as a UUID string. The system MUST:

1. Look up the `Purchase` by `downloadToken`.
2. If no `Purchase` is found, return `404 Not Found`.
3. If the `Purchase` is found but `status != PAID`, return `404 Not Found`. The system MUST NOT differentiate between PENDING and FAILED statuses in the response (non-enumerable).
4. If the `Purchase` has `status=PAID`, load the associated `DigitalProduct` from the catalog via `Purchase.productId`, open the file via `ProductFileStoragePort.open(storageKey)`, and stream the response.

The response MUST have:
- `Content-Type: application/pdf`
- `Content-Disposition: attachment; filename="<DigitalProduct.displayFilename>"`
- HTTP status `200 OK`

#### Scenario: Happy path — valid token, Purchase PAID

- GIVEN a `Purchase` with `status=PAID` and `downloadToken=550e8400-e29b-41d4-a716-446655440000`
- WHEN `GET /api/v1/downloads/550e8400-e29b-41d4-a716-446655440000` is called
- THEN the system returns `200 OK` with `Content-Type: application/pdf`
- AND `Content-Disposition: attachment; filename="Temario Subalterno GVA.pdf"`
- AND the PDF file bytes are streamed in the response body

#### Scenario: Token does not exist

- GIVEN no Purchase exists for token `00000000-0000-0000-0000-000000000000`
- WHEN `GET /api/v1/downloads/00000000-0000-0000-0000-000000000000` is called
- THEN the system returns `404 Not Found`

#### Scenario: Token exists but Purchase is PENDING (race condition)

- GIVEN a `Purchase` with `status=PENDING` and a valid `downloadToken`
- WHEN `GET /api/v1/downloads/{token}` is called
- THEN the system returns `404 Not Found`
- AND the response does not reveal whether a Purchase exists or not

#### Scenario: Token exists but Purchase is FAILED

- GIVEN a `Purchase` with `status=FAILED` and a valid `downloadToken`
- WHEN `GET /api/v1/downloads/{token}` is called
- THEN the system returns `404 Not Found`

#### Scenario: Simultaneous downloads with the same token

- GIVEN a `Purchase` with `status=PAID` and a valid `downloadToken`
- WHEN two concurrent requests call `GET /api/v1/downloads/{token}` simultaneously
- THEN both requests receive `200 OK` with the PDF stream
- AND no rate-limiting or single-use restriction is applied in the MVP

---

---

### Requirement: Purchase Info Endpoint — Public Read-Only Metadata

`GET /api/v1/downloads/{token}/info` MUST be publicly accessible (no authentication required). Unlike the file-download endpoint, this endpoint returns metadata about the purchase regardless of payment status — the frontend uses this to render the correct UI state.

The `{token}` path parameter MUST be validated as a UUID. If the value is not a valid UUID format, the system MUST return `400 Bad Request`.

The system MUST:

1. Look up the `Purchase` by `downloadToken`.
2. If no `Purchase` is found, return `404 Not Found`.
3. If the `Purchase` is found (regardless of `status`), return `200 OK` with the following JSON body:

```json
{
  "email":       "buyer@example.com",
  "productName": "Temario Subalterno GVA",
  "status":      "PENDING" | "PAID" | "FAILED",
  "amountCents": 1500,
  "currency":    "eur"
}
```

The response MUST NOT expose: `userId`, `stripePaymentIntentId`, `downloadToken`, `paidAt`, or `createdAt`.

`productName` MUST be resolved from `ProductCatalog` via `Purchase.productId` and mapped to `DigitalProduct.displayName`.

SecurityConfig change: add `.requestMatchers(HttpMethod.GET, "/api/v1/downloads/**").permitAll()` covers this endpoint too (same path prefix).

#### Scenario: Token valid, Purchase PAID

- GIVEN a `Purchase` with `status=PAID` and `downloadToken=550e8400-e29b-41d4-a716-446655440000`
- WHEN `GET /api/v1/downloads/550e8400-e29b-41d4-a716-446655440000/info` is called
- THEN the system returns `200 OK` with `{ email, productName, status: "PAID", amountCents, currency }`

#### Scenario: Token valid, Purchase PENDING

- GIVEN a `Purchase` with `status=PENDING` and a valid `downloadToken`
- WHEN `GET /api/v1/downloads/{token}/info` is called
- THEN the system returns `200 OK` with `{ ..., status: "PENDING" }`
- AND the frontend uses this to render "Procesando pago..." state

#### Scenario: Token valid, Purchase FAILED

- GIVEN a `Purchase` with `status=FAILED` and a valid `downloadToken`
- WHEN `GET /api/v1/downloads/{token}/info` is called
- THEN the system returns `200 OK` with `{ ..., status: "FAILED" }`
- AND the frontend uses this to render "Pago fallido" state

#### Scenario: Token does not exist

- GIVEN no `Purchase` exists for the provided token
- WHEN `GET /api/v1/downloads/{token}/info` is called
- THEN the system returns `404 Not Found`

#### Scenario: Token with malformed UUID

- GIVEN the request path contains a non-UUID value (e.g., `/info/not-a-uuid/info`)
- WHEN `GET /api/v1/downloads/{token}/info` is called
- THEN the system returns `400 Bad Request`

---

### Requirement: Purchase Aggregate State Model

The `Purchase` aggregate MUST have the following lifecycle:

| Status | Description |
|--------|-------------|
| `PENDING` | Created by `create-intent`; awaiting Stripe confirmation |
| `PAID` | Webhook or reconciliation confirmed payment succeeded |
| `FAILED` | Webhook or reconciliation confirmed payment failed |

State transitions MUST be:
- `PENDING → PAID`: triggered by `payment_intent.succeeded` webhook OR by reconciliation scheduler
- `PENDING → FAILED`: triggered by `payment_intent.payment_failed` webhook OR by reconciliation scheduler
- No other transitions are allowed (PAID and FAILED are terminal states)

The `Purchase` MUST store: `id` (UUID PK), `email`, `productId`, `stripePaymentIntentId` (UNIQUE), `downloadToken` (UUID, UNIQUE), `status`, `createdAt`, `paidAt` (nullable), `emailSentAt` (nullable), `userId` (nullable).

`emailSentAt` is set to `now()` only when the transactional email is delivered successfully (Resend returns 2xx). It remains `NULL` if email delivery fails or has not yet been attempted. It is never exposed in API responses.
