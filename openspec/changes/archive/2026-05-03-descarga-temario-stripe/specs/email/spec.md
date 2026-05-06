# Transactional Email Specification

## Purpose

Covers the transactional email sent to the buyer after a successful payment. Email delivery is triggered exclusively by the webhook handler (or the reconciliation scheduler) when a `Purchase` transitions to `PAID`.

---

## Requirements

### Requirement: Email Sent on Purchase PAID Transition

The system MUST send exactly one transactional email per `Purchase` when `status` transitions from `PENDING` to `PAID`. Email MUST NOT be sent for `FAILED` transitions or for webhook events that produce no state change (idempotent no-ops).

The email MUST be delivered via the Resend API (`POST https://api.resend.com/emails`) using the API key from `RESEND_API_KEY` environment variable.

The `from` address MUST be taken from `RESEND_FROM_EMAIL` environment variable (`noreply@akademia.diegobarrioh.dev` in production; `onboarding@resend.dev` in development/test).

**Email delivery tracking**: the `Purchase` aggregate MUST track whether the email was successfully delivered via the `emailSentAt` field (Instant, nullable). This field is set to `now()` only when `TransactionalEmailPort.send(...)` returns `true` (indicating Resend returned 2xx). It remains `NULL` if delivery fails or has not been attempted. The `PurchaseReconciliationScheduler` uses this field to retry failed deliveries (see reconciliation spec).

#### Scenario: Email sent after successful webhook

- GIVEN a `Purchase` transitions from `PENDING` to `PAID` via the webhook handler
- WHEN `rowsAffected == 1` from the atomic UPDATE
- THEN one email is dispatched to `Purchase.email` via Resend API
- AND if `TransactionalEmailPort.send(...)` returns `true`, `emailSentAt` is set to `now()` and persisted in the same call chain
- AND the email MUST NOT be sent again if the webhook arrives a second time (idempotent guard via `rowsAffected == 0`)

#### Scenario: No email on payment failure

- GIVEN a `Purchase` transitions to `FAILED`
- WHEN the webhook processes `payment_intent.payment_failed`
- THEN no email is sent

---

### Requirement: Email Content Requirements

The email MUST contain:
- **Subject**: a human-readable subject including the product name (e.g., "Tu descarga: Temario Subalterno GVA").
- **HTML body** (inline, no external template engine): a header with the product name, a prominent call-to-action button linking to `${APP_BASE_URL}/descarga/{downloadToken}`, and a plain-text fallback URL for email clients that do not render HTML.
- **Plain-text fallback**: the download URL MUST appear as readable text in the email body so that clients without HTML support can still access the link.

The `downloadToken` embedded in the URL MUST match `Purchase.downloadToken`.

#### Scenario: Email content is correct for PAID purchase

- GIVEN a `Purchase` with `downloadToken=abc-123` for product `TEMARIO_SUBALTERNO_GVA`
- AND `APP_BASE_URL=https://akademia.diegobarrioh.dev`
- WHEN the email is composed
- THEN the HTML body contains a link to `https://akademia.diegobarrioh.dev/descarga/abc-123`
- AND the plain-text part contains the same URL as readable text
- AND the subject contains "Temario Subalterno GVA"

---

### Requirement: Email Delivery Failure Handling

If the Resend API call fails (network error, non-2xx response), `TransactionalEmailPort.send(...)` MUST return `false` (it MUST NOT propagate an unchecked exception that bubbles up to the webhook handler). The system MUST log the error at ERROR level with SLF4J including the `Purchase.id` and HTTP status/message. The webhook handler MUST still return `200 OK` to Stripe.

When `send(...)` returns `false`, `emailSentAt` MUST remain `NULL` on the `Purchase`. The `PurchaseReconciliationScheduler` will retry sending on its next tick (see reconciliation spec — Function B).

Email re-delivery for PAID purchases with `email_sent_at IS NULL` is handled automatically by the scheduler. Manual operator intervention is NOT required for transient Resend outages.

#### Scenario: Resend API returns error during webhook

- GIVEN a `Purchase` transitions from `PENDING` to `PAID` via the webhook handler
- AND the Resend API returns a `429 Too Many Requests` response when the email is attempted
- WHEN the email adapter handles the error
- THEN `TransactionalEmailPort.send(...)` returns `false`
- AND the error is logged at ERROR level with `Purchase.id`
- AND the webhook handler returns `200 OK` to Stripe
- AND the `Purchase.status` remains `PAID` with `emailSentAt=NULL`
- AND the scheduler will retry sending the email on its next tick (Function B)
