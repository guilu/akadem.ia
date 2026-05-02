# Payments Specification

## Purpose

Covers the purchase initiation (`POST /api/v1/payments/create-intent`) and the Stripe webhook handler (`POST /api/v1/payments/webhook`). These two endpoints form the core of the guest-checkout purchase flow.

---

## Requirements

### Requirement: Create Payment Intent — Public Endpoint

`POST /api/v1/payments/create-intent` MUST be publicly accessible (no authentication required). The endpoint MUST accept a JSON body `{ email: string, productId: string }`. Both fields MUST be present and non-empty; missing or blank values MUST return `400 Bad Request`. The email MUST pass basic RFC 5322 format validation server-side. The `productId` MUST exist in the product catalog; an unknown `productId` MUST return `404 Not Found`.

The endpoint MUST:
1. Look up the `DigitalProduct` from the in-memory catalog via `productId`.
2. Create a Stripe `PaymentIntent` with `amount`, `currency`, and `metadata: { email, productId, userId? }` (userId populated if a valid JWT is present, otherwise omitted).
3. Persist a `Purchase` record in status `PENDING` with a new UUID `downloadToken` and the Stripe `paymentIntentId`.
4. Return `{ clientSecret: string, downloadToken: string }` so the frontend can proceed with payment confirmation and knows the post-payment redirect URL before the webhook fires.

The `stripePaymentIntentId` MUST have a UNIQUE constraint in the database (enforced by `V011__purchases.sql`). The `downloadToken` MUST have a UNIQUE constraint.

#### Scenario: Happy path — valid email and product

- GIVEN a guest user with email `buyer@example.com` and productId `TEMARIO_SUBALTERNO_GVA`
- WHEN `POST /api/v1/payments/create-intent` is called with `{ email: "buyer@example.com", productId: "TEMARIO_SUBALTERNO_GVA" }`
- THEN the system returns `200 OK` with `{ clientSecret, downloadToken }`
- AND a `Purchase` record exists in the database with `status=PENDING`, `email=buyer@example.com`, and the returned `downloadToken`

#### Scenario: Missing email field

- GIVEN a request body `{ productId: "TEMARIO_SUBALTERNO_GVA" }` with no email
- WHEN `POST /api/v1/payments/create-intent` is called
- THEN the system returns `400 Bad Request`
- AND no `Purchase` record is created and no Stripe call is made

#### Scenario: Invalid product ID

- GIVEN a request body `{ email: "buyer@example.com", productId: "UNKNOWN_SKU" }`
- WHEN `POST /api/v1/payments/create-intent` is called
- THEN the system returns `404 Not Found`
- AND no `Purchase` record is created and no Stripe call is made

#### Scenario: Authenticated user triggers create-intent

- GIVEN a valid JWT is present in the Authorization header for user `user-uuid-123`
- WHEN `POST /api/v1/payments/create-intent` is called with valid body
- THEN the Stripe `PaymentIntent` metadata contains `userId=user-uuid-123`
- AND the `Purchase` record stores `userId=user-uuid-123`

---

### Requirement: Webhook — Signature Verification

`POST /api/v1/payments/webhook` MUST be publicly accessible. It MUST read the raw request body and the `Stripe-Signature` header. The system MUST call `Webhook.constructEvent(rawBody, signatureHeader, webhookSecret)` to verify integrity. If verification fails (invalid or missing signature), the system MUST return `400 Bad Request` and MUST NOT process the event.

#### Scenario: Valid signature

- GIVEN a Stripe webhook payload with a valid `Stripe-Signature` header
- WHEN `POST /api/v1/payments/webhook` is called
- THEN `constructEvent` succeeds and the event is dispatched for processing
- AND the system returns `200 OK`

#### Scenario: Invalid signature

- GIVEN a Stripe webhook payload with a tampered or missing `Stripe-Signature` header
- WHEN `POST /api/v1/payments/webhook` is called
- THEN the system returns `400 Bad Request`
- AND no Purchase state change occurs and no email is sent

---

### Requirement: Webhook — Event Handling and Idempotency

The webhook handler MUST process two event types: `payment_intent.succeeded` and `payment_intent.payment_failed`. All other event types MUST be acknowledged with `200 OK` and ignored.

**For `payment_intent.succeeded`:** The system MUST execute `UPDATE purchases SET status=PAID, paidAt=now() WHERE stripePaymentIntentId=? AND status=PENDING`. If `rowsAffected == 1`, the system MUST trigger transactional email delivery. If `rowsAffected == 0` (already PAID or FAILED), the system MUST return `200 OK` without sending a duplicate email.

**For `payment_intent.payment_failed`:** The system MUST execute `UPDATE purchases SET status=FAILED WHERE stripePaymentIntentId=? AND status=PENDING`. No email is sent for failed payments.

The system MUST log `event.id`, `event.type`, `paymentIntentId`, and processing result using SLF4J at INFO level.

#### Scenario: Successful payment — first delivery

- GIVEN a `Purchase` with `status=PENDING` and `stripePaymentIntentId=pi_abc`
- WHEN webhook receives `payment_intent.succeeded` for `pi_abc`
- THEN `Purchase.status` transitions to `PAID` and `paidAt` is set
- AND one transactional email is sent to `Purchase.email`
- AND the webhook returns `200 OK`

#### Scenario: Webhook delivered twice (idempotency)

- GIVEN a `Purchase` already in `status=PAID` for `stripePaymentIntentId=pi_abc`
- WHEN webhook receives a second `payment_intent.succeeded` for `pi_abc`
- THEN `UPDATE … WHERE status=PENDING` affects 0 rows
- AND no email is sent
- AND the webhook returns `200 OK`

#### Scenario: Payment failed event

- GIVEN a `Purchase` with `status=PENDING` and `stripePaymentIntentId=pi_xyz`
- WHEN webhook receives `payment_intent.payment_failed` for `pi_xyz`
- THEN `Purchase.status` transitions to `FAILED`
- AND no email is sent
- AND the webhook returns `200 OK`

#### Scenario: Unknown event type

- GIVEN a valid Stripe webhook payload with `event.type=customer.created`
- WHEN webhook receives the event
- THEN the system returns `200 OK` and takes no action
