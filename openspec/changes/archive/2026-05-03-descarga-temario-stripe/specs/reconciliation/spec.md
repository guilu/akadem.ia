# Reconciliation Specification

## Purpose

Covers the `PurchaseReconciliationScheduler`, a background process that resolves `Purchase` records stuck in `PENDING` status when the Stripe webhook was never delivered (misconfiguration, downtime, or transient network failure).

---

## Requirements

### Requirement: Scheduler Frequency and Selection Criteria

The `PurchaseReconciliationScheduler` MUST run every 15 minutes via Spring `@Scheduled`. On each tick, it MUST query the database for all `Purchase` records where `status = PENDING` AND `createdAt < now() - 1 hour`. Only purchases older than 1 hour are eligible; this avoids interfering with in-flight purchases.

#### Scenario: No eligible PENDING purchases

- GIVEN all PENDING purchases have `createdAt` within the last 1 hour
- WHEN the scheduler ticks
- THEN no Stripe API calls are made and no state changes occur

#### Scenario: Eligible PENDING purchase found

- GIVEN a `Purchase` with `status=PENDING` and `createdAt = 2h ago`
- WHEN the scheduler ticks
- THEN `PaymentIntent.retrieve(stripePaymentIntentId)` is called for that purchase

---

### Requirement: Reconciliation State Transitions

For each eligible `Purchase`, the scheduler MUST call `Stripe.PaymentIntent.retrieve(stripePaymentIntentId)`. Based on the returned `PaymentIntent.status`:

| Stripe status | Action |
|---------------|--------|
| `succeeded` | Transition `Purchase` to `PAID`, set `paidAt=now()`, send transactional email (same as webhook) |
| `canceled` or `payment_failed` | Transition `Purchase` to `FAILED` |
| Any other status (e.g., `requires_payment_method`) | Leave as `PENDING`, log at WARN level |

The email MUST be sent using the same idempotent update (`UPDATE … WHERE status=PENDING`) as the webhook handler — if the webhook also fires, only one of the two will produce `rowsAffected=1` and send the email.

#### Scenario: PENDING purchase reconciled to PAID

- GIVEN a `Purchase` with `status=PENDING`, `createdAt=2h ago`, and Stripe returns `status=succeeded`
- WHEN the scheduler processes this purchase
- THEN `Purchase.status` transitions to `PAID`, `paidAt` is set
- AND one transactional email is sent to `Purchase.email`

#### Scenario: PENDING purchase reconciled to FAILED

- GIVEN a `Purchase` with `status=PENDING`, `createdAt=2h ago`, and Stripe returns `status=canceled`
- WHEN the scheduler processes this purchase
- THEN `Purchase.status` transitions to `FAILED`
- AND no email is sent

#### Scenario: Race condition — webhook fires before scheduler completes reconciliation

- GIVEN a `Purchase` with `status=PENDING` eligible for reconciliation
- AND the webhook fires and sets `status=PAID` just before the scheduler UPDATE executes
- WHEN the scheduler executes `UPDATE … WHERE status=PENDING`
- THEN `rowsAffected=0` and the scheduler skips email delivery
- AND the final state is `PAID` with exactly one email sent (by the webhook)

#### Scenario: Scheduler runs while a webhook successfully processed the purchase earlier

- GIVEN a `Purchase` already in `status=PAID` (processed by webhook)
- WHEN the scheduler queries for `status=PENDING AND createdAt < now()-1h`
- THEN this purchase is NOT included in the result set
- AND no Stripe API call is made for it

---

---

### Requirement: Scheduler — Email Retry for PAID Purchases with Missing Email

In addition to reconciling PENDING purchases, the scheduler MUST also retry sending the transactional email for `Purchase` records where `status = PAID AND email_sent_at IS NULL AND paid_at < now() - interval '5 minutes'`.

The 5-minute grace period is required to avoid sending two emails in the rare case where the webhook is still within its own request lifecycle when the scheduler first runs.

On each tick, the scheduler processes email retries AFTER completing PENDING reconciliation (Function A runs first, then Function B).

For each eligible PAID purchase without `emailSentAt`:
- Attempt to send the transactional email via `TransactionalEmailPort.send(...)`.
- If the send returns `true` (success): set `emailSentAt = now()` on the `Purchase` and persist.
- If the send returns `false` (failure): log the failure at WARN level with `Purchase.id` and leave `emailSentAt = NULL` to be retried in the next tick.

**Idempotency note**: In the extremely unlikely race where both the webhook and the scheduler send the email (e.g., webhook succeeds but the `email_sent_at` update fails before committing), the user may receive two identical emails. This is accepted as a known edge case for MVP.

#### Scenario: Webhook Resend failure — scheduler retries and succeeds

- GIVEN a `Purchase` with `status=PAID`, `email_sent_at=NULL`, `paid_at=20 min ago`
- AND the transactional email was not sent during the webhook (Resend was down)
- WHEN the scheduler runs Function B
- THEN `TransactionalEmailPort.send(...)` is called
- AND Resend returns success
- THEN `emailSentAt` is set to `now()` and persisted
- AND the purchase is NOT selected in subsequent scheduler runs (email_sent_at is no longer NULL)

#### Scenario: Webhook Resend failure — scheduler retries and also fails

- GIVEN a `Purchase` with `status=PAID`, `email_sent_at=NULL`, `paid_at=20 min ago`
- WHEN the scheduler runs Function B and Resend returns an error
- THEN a WARN log is emitted with `Purchase.id`
- AND `emailSentAt` remains NULL
- AND the purchase will be retried in the next scheduler tick (15 min later)

#### Scenario: Extended Resend outage — backlog processed when Resend recovers

- GIVEN Resend was down for 1 hour during which 5 purchases were marked PAID
- AND all 5 have `email_sent_at=NULL`
- WHEN Resend recovers and the next scheduler tick runs
- THEN all 5 purchases are selected by Function B
- AND emails are sent to all 5 buyers
- AND `emailSentAt` is set for each successful send

#### Scenario: Email already sent by webhook — scheduler skips

- GIVEN a `Purchase` with `status=PAID` and `email_sent_at` set (not NULL)
- WHEN the scheduler runs Function B
- THEN this purchase is NOT selected (filtered out by `email_sent_at IS NULL`)
- AND no duplicate email is sent

#### Scenario: PAID purchase within 5-minute grace period

- GIVEN a `Purchase` with `status=PAID`, `email_sent_at=NULL`, `paid_at=3 min ago`
- WHEN the scheduler runs Function B
- THEN this purchase is NOT selected (grace period not elapsed)
- AND no email is sent yet (the webhook may still be processing)

---

### Requirement: Scheduler Logging and Error Handling

The scheduler MUST log at INFO level: the number of eligible PENDING purchases found on each tick, and the outcome for each purchase (reconciled to PAID, FAILED, or left PENDING). If the Stripe API call fails for a specific purchase (network error, timeout), the scheduler MUST log the error at ERROR level with `Purchase.id` and MUST continue processing the remaining eligible purchases (fail-open per record, not per batch).

#### Scenario: Stripe API fails for one purchase in a batch

- GIVEN three eligible PENDING purchases and the Stripe API call fails for the second one
- WHEN the scheduler processes the batch
- THEN purchases 1 and 3 are reconciled normally
- AND purchase 2 remains PENDING with an ERROR log entry containing its ID
- AND no exception propagates that would stop the scheduler from running on the next tick
