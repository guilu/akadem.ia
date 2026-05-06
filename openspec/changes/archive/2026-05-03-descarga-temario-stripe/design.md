# Design: Descarga del Temario Subalterno GVA tras pago Stripe

## 1. Arquitectura — vista general

```
[Browser]
   │  POST /api/v1/payments/create-intent {email, productId}
   │  POST /api/v1/payments/webhook (Stripe-Signature header)
   │  GET  /api/v1/downloads/{token}
   ▼
[PaymentController / DownloadController]
   │
   ▼
[PurchaseService] ──────────────────────────────────────────────┐
   │  CreatePaymentIntentUseCase                                 │
   │  HandleStripeWebhookUseCase                                 │
   │  DownloadPurchaseUseCase                                    │
   ▼                                                             │
[Domain ports/out]                                              │
   ├── PurchaseRepository ──► PurchaseRepositoryAdapter         │
   │                            └── JpaPurchaseRepository       │
   │                                    └── PostgreSQL           │
   ├── ProductCatalog ──────► InMemoryProductCatalog (hardcoded)│
   ├── ProductFileStoragePort ► LocalProductFileStorageAdapter   │
   │                            └── PRODUCTS_STORAGE_PATH/      │
   └── TransactionalEmailPort ► ResendEmailAdapter              │
                                 └── https://api.resend.com     │
                                                                 │
[StripePaymentAdapter] ◄─────────────────────────────────────── ┘
   └── Stripe API (PaymentIntent.create / retrieve)

[PurchaseReconciliationScheduler] (every 15 min)
   └── PurchaseRepository (findPendingOlderThan)
       └── Stripe.PaymentIntent.retrieve → markPaid / markFailed
```

## 2. Backend — paquetización hexagonal

Base: `backend/src/main/java/com/akdemya/`

### Domain model

| File | Type | Description |
|------|------|-------------|
| `domain/model/Purchase.java` | Class | Aggregate: id (UUID), stripePaymentIntentId, downloadToken (UUID), email, productId, userId (nullable), status, amountCents, currency, createdAt, paidAt (nullable), emailSentAt (nullable). Factory: `Purchase.create(email, productId, piId, downloadToken)` → status=PENDING. Setter `markEmailSent(Instant)` sets emailSentAt. |
| `domain/model/PurchaseStatus.java` | Enum | `PENDING`, `PAID`, `FAILED` |
| `domain/model/DigitalProduct.java` | Class | Fields: sku (String), displayName, amountCents (long), currency, storageKey (filename on disk), displayFilename. Constructor + getters only. |

**Domain model convention**: plain class with constructor-based validation + static factory (`create`), same as `Flashcard`. No Java records for entities.

### Domain ports in (`domain/port/in/`)

```java
// CreatePaymentIntentUseCase.java  (MODIFY existing)
public interface CreatePaymentIntentUseCase {
    record Command(String email, String productId, @Nullable UUID userId) {}
    record Result(String clientSecret, UUID downloadToken) {}
    Result createIntent(Command command);
}

// HandleStripeWebhookUseCase.java  (NEW)
public interface HandleStripeWebhookUseCase {
    void handleEvent(String rawPayload, String stripeSignatureHeader);
}

// DownloadPurchaseUseCase.java  (NEW)
public interface DownloadPurchaseUseCase {
    record Result(DigitalProduct product, InputStream stream) {}
    Result openByToken(UUID downloadToken);
    // throws NoSuchElementException if token unknown or status != PAID
}

// GetPurchaseInfoUseCase.java  (NEW)
public interface GetPurchaseInfoUseCase {
    record PurchaseInfo(String email, String productName, String status, long amountCents, String currency) {}
    PurchaseInfo getInfo(UUID downloadToken);
    // throws NoSuchElementException if token unknown → 404
    // throws IllegalArgumentException if token UUID is malformed → 400 (validated by Spring before reaching use case)
}
```

### Domain ports out (`domain/port/out/`)

```java
// PurchaseRepository.java  (NEW)
public interface PurchaseRepository {
    Purchase save(Purchase purchase);
    Optional<Purchase> findByDownloadToken(UUID token);
    Optional<Purchase> findByStripePaymentIntentId(String piId);
    // Returns 1 if updated (was PENDING), 0 if already PAID/FAILED (idempotent)
    int markPaid(String stripePaymentIntentId, Instant paidAt);
    int markFailed(String stripePaymentIntentId);
    List<Purchase> findPendingOlderThan(Instant cutoff);
    // Returns PAID purchases where emailSentAt IS NULL and paidAt < graceCutoff
    List<Purchase> findPaidWithoutEmail(Instant graceCutoff);
    // Sets emailSentAt on the given Purchase (UPDATE purchases SET email_sent_at=? WHERE id=?)
    void updateEmailSentAt(UUID purchaseId, Instant emailSentAt);
}

// ProductFileStoragePort.java  (NEW)
public interface ProductFileStoragePort {
    InputStream open(String storageKey);
    // throws RuntimeException if file not found on disk
}

// TransactionalEmailPort.java  (NEW)
public interface TransactionalEmailPort {
    // Returns true if email was delivered successfully (Resend returned 2xx).
    // Returns false on any error (network, 4xx, 5xx) — MUST NOT throw unchecked exception.
    boolean sendDownloadEmail(String toEmail, String downloadUrl, String productDisplayName);
}

// ProductCatalog.java  (NEW — out port, in-memory impl)
public interface ProductCatalog {
    Optional<DigitalProduct> findById(String productId);
}
```

### Application services (`application/service/`)

| File | Action | Description |
|------|--------|-------------|
| `PurchaseService.java` | New | Implements `CreatePaymentIntentUseCase`, `HandleStripeWebhookUseCase`, `DownloadPurchaseUseCase`, `GetPurchaseInfoUseCase`. Orchestrates domain flow, delegates to ports. `@Service @Transactional`. |
| `InMemoryProductCatalog.java` | New | `@Component` implementing `ProductCatalog`. Hardcoded map with one entry: `TEMARIO_SUBALTERNO_GVA`. |

### Application config (`application/config/`)

| File | Action | Fields |
|------|--------|--------|
| `ResendProperties.java` | New | `@ConfigurationProperties(prefix = "app.email.resend")` — `apiKey`, `from` |
| `ProductsProperties.java` | New | `@ConfigurationProperties(prefix = "app.products")` — `storagePath` (default `/tmp/akademia-products`) |
| `AppProperties.java` | New | `@ConfigurationProperties(prefix = "app")` — `baseUrl` (APP_BASE_URL). Note: `app.frontend-url` already exists — reuse as `baseUrl` if semantically the same, else add `app.base-url`. |

> **Decision on AppProperties**: `app.frontend-url` is the SPA URL (e.g. `https://akademia.diegobarrioh.dev`). The download link in the email points to the same host (the SPA route `/descarga/:token`), so `FRONTEND_URL` is reused directly via `app.frontend-url`. No new property needed for the email link.

### Inbound adapters (`adapter/inbound/web/`)

| File | Action | Description |
|------|--------|-------------|
| `PaymentController.java` | Modify | Accept `@RequestBody CreateIntentRequest`; new `@PostMapping("/webhook")` accepting raw `byte[]` body via `@RequestBody` + `Stripe-Signature` header |
| `DownloadController.java` | New | Two endpoints: `@GetMapping("/api/v1/downloads/{token}")` streams PDF (existing design); `@GetMapping("/api/v1/downloads/{token}/info")` returns `PurchaseInfoResponse` JSON (new — see Resolution 1). UUID path variable validated by Spring (`@Valid` + `@NotNull`; malformed UUID → 400 via `MethodArgumentTypeMismatchException` → `GlobalExceptionHandler`). |

**DTOs** (`adapter/inbound/web/dto/`):

```java
// CreateIntentRequest.java  (NEW — record)
public record CreateIntentRequest(
    @NotBlank String email,
    @NotBlank String productId
) {}

// CreateIntentResponse.java  (NEW — record, replaces PaymentIntentResponse)
public record CreateIntentResponse(String clientSecret, UUID downloadToken) {}

// WebhookResponse.java  (NEW — record)
public record WebhookResponse(String status) {}

// PurchaseInfoResponse.java  (NEW — record)
// Response DTO for GET /api/v1/downloads/{token}/info
// Exposes only buyer-safe fields; never exposes stripePaymentIntentId, downloadToken, userId, paidAt, createdAt
public record PurchaseInfoResponse(
    String email,
    String productName,
    String status,
    long amountCents,
    String currency
) {}
```

> `PaymentIntentResponse.java` is replaced by `CreateIntentResponse`. The old record can be deleted as its sole consumer (`PaymentController`) is being modified.

### Infrastructure adapters

| File | Action | Description |
|------|--------|-------------|
| `adapter/infrastructure/stripe/StripePaymentAdapter.java` | Modify | New `createIntent(Command cmd)` signature; builds metadata `{email, productId, purchaseId, downloadToken}`; reads `amountCents` and `currency` from `DigitalProduct`; still implements `CreatePaymentIntentUseCase` |
| `adapter/infrastructure/stripe/StripeEventVerifierAdapter.java` | New | `@Component`. Wraps `Webhook.constructEvent(rawPayload, signature, webhookSecret)`. Throws `SignatureVerificationException` on tampered payload. |
| `adapter/infrastructure/storage/LocalProductFileStorageAdapter.java` | New | `@Component` implementing `ProductFileStoragePort`. Reads `ProductsProperties.storagePath`; opens file as `FileInputStream`. Path sanitized with `Paths.get(storageKey).getFileName()` to prevent traversal. |
| `adapter/infrastructure/email/ResendEmailAdapter.java` | New | `@Component` implementing `TransactionalEmailPort`. Uses `RestClient` (same pattern as `OpenAiEmbeddingAdapter`). POST to `https://api.resend.com/emails`. |
| `adapter/infrastructure/scheduler/PurchaseReconciliationScheduler.java` | New | `@Component`. `@Scheduled(cron = "0 */15 * * * *")` (every 15 min, matching cron style of existing `RefreshTokenCleanupJob`). |

### Outbound persistence adapters

| File | Action | Description |
|------|--------|-------------|
| `adapter/outbound/persistence/entity/PurchaseEntity.java` | New | `@Entity @Table(name = "purchases")`. Fields match DB schema below. Uses `Instant` (same as `FlashcardEntity`). |
| `adapter/outbound/persistence/repository/JpaPurchaseRepository.java` | New | `JpaRepository<PurchaseEntity, UUID>` + custom queries for `findByDownloadToken`, `findByStripePaymentIntentId`, `findPendingOlderThan`. |
| `adapter/outbound/persistence/PurchaseRepositoryAdapter.java` | New | `@Component` implementing `PurchaseRepository`. Uses `JpaPurchaseRepository`. `markPaid` / `markFailed` via `@Modifying @Query("UPDATE ... WHERE status = 'PENDING'")`. |
| `adapter/outbound/persistence/mapper/PurchaseMapper.java` | New | `toDomain(PurchaseEntity)` / `toEntity(Purchase)`. |

## 3. Base de datos

**Flyway migration**: `V011__purchases.sql` (confirmed — last existing is `V010__syllabuses.sql`).

```sql
CREATE TABLE purchases (
    id                        UUID        PRIMARY KEY,
    stripe_payment_intent_id  VARCHAR(255) NOT NULL,
    download_token            UUID        NOT NULL,
    email                     VARCHAR(320) NOT NULL,
    product_id                VARCHAR(100) NOT NULL,
    user_id                   UUID,
    status                    VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    amount_cents              BIGINT      NOT NULL,
    currency                  VARCHAR(10)  NOT NULL,
    created_at                TIMESTAMPTZ  NOT NULL DEFAULT now(),
    paid_at                   TIMESTAMPTZ,
    email_sent_at             TIMESTAMPTZ
);

CREATE UNIQUE INDEX uq_purchases_stripe_pi_id
    ON purchases (stripe_payment_intent_id);
-- Used by: webhook handler (findByStripePaymentIntentId, markPaid, markFailed)

CREATE UNIQUE INDEX uq_purchases_download_token
    ON purchases (download_token);
-- Used by: GET /api/v1/downloads/{token} and GET /api/v1/downloads/{token}/info

CREATE INDEX idx_purchases_status_created_at
    ON purchases (status, created_at)
    WHERE status = 'PENDING';
-- Used by: reconciliation scheduler Function A (findPendingOlderThan)

CREATE INDEX idx_purchases_pending_email
    ON purchases (paid_at)
    WHERE status = 'PAID' AND email_sent_at IS NULL;
-- Used by: reconciliation scheduler Function B (findPaidWithoutEmail)
```

**Index rationale**:
- `uq_purchases_stripe_pi_id`: enforces idempotency at DB level; webhook `UPDATE WHERE status=PENDING` is a point lookup.
- `uq_purchases_download_token`: download endpoints do a UUID point lookup.
- Partial index on `(status, created_at) WHERE status='PENDING'`: narrow index for scheduler Function A; ignored once row transitions to PAID/FAILED.
- Partial index on `(paid_at) WHERE status='PAID' AND email_sent_at IS NULL`: used by scheduler Function B to efficiently find purchases needing email retry; the partial filter keeps the index small (only un-emailed rows).

## 4. Contratos de API

### POST /api/v1/payments/create-intent
Public. Security already configured (`permitAll`).

```
Request:
  Content-Type: application/json
  { "email": "user@example.com", "productId": "TEMARIO_SUBALTERNO_GVA" }

Response 200:
  { "clientSecret": "pi_xxx_secret_yyy", "downloadToken": "550e8400-e29b-41d4-a716-446655440000" }

Response 400:
  { "error": "validation_failed", "fields": { "email": "must not be blank" } }
  (handled by GlobalExceptionHandler + @Valid on @RequestBody)

Response 500:
  { "error": "internal_server_error" }
  (StripeException wrapped → RuntimeException → GlobalExceptionHandler.handleGeneric)
```

### POST /api/v1/payments/webhook
Public. `Content-Type: application/octet-stream` or raw body.

```
Request:
  Header: Stripe-Signature: t=...,v1=...
  Body: raw JSON bytes (Stripe event payload)

Response 200:
  { "status": "ok" }

Response 400:
  { "error": "bad_request" }
  (signature verification failed → IllegalArgumentException → GlobalExceptionHandler)

Response 500:
  { "error": "internal_server_error" }
  (Stripe reintenta hasta 3 días si recibe 5xx — correcto)
```

> Spring parses `@RequestBody` as `byte[]` when controller parameter type is `byte[]` — this preserves the raw body for Stripe signature verification. Alternatively use `HttpServletRequest.getInputStream()`. Chosen: `byte[]` for simplicity.

### GET /api/v1/downloads/{token}
Public. Token is a UUID string.

```
Response 200:
  Content-Type: application/pdf
  Content-Disposition: attachment; filename="Temario Subalterno GVA.pdf"
  Body: PDF binary stream (StreamingResponseBody)

Response 404:
  { "error": "not_found" }
  (token unknown, status=PENDING, or status=FAILED → same 404; GlobalExceptionHandler.handleNotFound via NoSuchElementException)
```

SecurityConfig change: add `.requestMatchers(HttpMethod.GET, "/api/v1/downloads/**").permitAll()`.

### GET /api/v1/downloads/{token}/info
Public. Returns purchase metadata without serving the file. Returns 200 for any valid token regardless of status.

```
Response 200:
  Content-Type: application/json
  { "email": "buyer@example.com", "productName": "Temario Subalterno GVA", "status": "PAID", "amountCents": 1500, "currency": "eur" }

Response 200 (PENDING):
  { ..., "status": "PENDING" }

Response 200 (FAILED):
  { ..., "status": "FAILED" }

Response 404:
  { "error": "not_found" }
  (token unknown → NoSuchElementException → GlobalExceptionHandler.handleNotFound)

Response 400:
  { "error": "bad_request" }
  (malformed UUID → MethodArgumentTypeMismatchException → GlobalExceptionHandler)
```

`productName` is resolved via `ProductCatalog.findById(purchase.productId).displayName`.

## 5. Integración Stripe

### Metadata en PaymentIntent
```java
PaymentIntentCreateParams.builder()
    .setAmount(product.getAmountCents())
    .setCurrency(product.getCurrency())
    .putMetadata("purchaseId",    purchase.getId().toString())
    .putMetadata("downloadToken", purchase.getDownloadToken().toString())
    .putMetadata("productId",     product.getSku())
    .putMetadata("email",         command.email())
    // userId only if present (optional logged-in user)
    .build()
```

### Flujo create-intent (paso a paso)
1. Validate `CreateIntentRequest` (`@Valid`).
2. `productCatalog.findById(productId)` → `DigitalProduct` or throw `IllegalArgumentException` (→ 400).
3. Generate `downloadToken = UUID.randomUUID()`.
4. `StripePaymentAdapter.createIntent(command)` → calls `PaymentIntent.create(...)` → returns `clientSecret`.
5. `purchaseRepository.save(Purchase.create(email, productId, piId, downloadToken, amountCents, currency))`.
6. Return `CreateIntentResponse(clientSecret, downloadToken)`.

> Steps 4 and 5 ordering: Stripe is called first; if it throws, no `Purchase` is saved. If `save` fails after Stripe succeeds, the `PaymentIntent` is orphaned in Stripe — reconciliation scheduler picks it up if the user actually pays.

### Flujo webhook (paso a paso)
1. `stripeEventVerifierAdapter.constructEvent(rawPayload, signatureHeader)` → `Event` or throw.
2. Extract `paymentIntentId` from event object (`event.getDataObjectDeserializer().getObject()`).
3. Switch on `event.getType()`:
   - `payment_intent.succeeded` → `purchaseRepository.markPaid(piId, Instant.now())`.
     - If `rowsAffected == 1`: load `Purchase`, build download URL, call `emailPort.sendDownloadEmail(...)`.
       - If `sendDownloadEmail` returns `true`: call `purchaseRepository.updateEmailSentAt(purchaseId, now())`.
       - If `sendDownloadEmail` returns `false`: log ERROR, leave `emailSentAt=NULL` (scheduler will retry).
       - Either way: webhook continues to return 200 OK to Stripe.
     - If `rowsAffected == 0`: already PAID — log, return 200 (idempotent).
   - `payment_intent.payment_failed` → `purchaseRepository.markFailed(piId)`. No email.
   - Other types → log and return 200 (ignored).
4. Return `WebhookResponse("ok")`.

### Error mapping Stripe
| Stripe exception | HTTP response |
|------------------|--------------|
| `SignatureVerificationException` | 400 via `IllegalArgumentException` in `GlobalExceptionHandler` |
| `StripeException` on `create-intent` | 500 via `RuntimeException` in `GlobalExceptionHandler` |
| `StripeException` in webhook | 500 (Stripe will retry) |

## 6. Integración Resend

**HTTP client**: `RestClient` — confirmed as the project pattern (`OpenAiEmbeddingAdapter` uses `RestClient`, no WebClient or RestTemplate present).

**ResendEmailAdapter** structure (mirrors `OpenAiEmbeddingAdapter`):
```java
@Component
public class ResendEmailAdapter implements TransactionalEmailPort {
    private final ResendProperties props;
    private final RestClient restClient;

    public ResendEmailAdapter(ResendProperties props) {
        this.props = props;
        this.restClient = RestClient.builder()
            .baseUrl("https://api.resend.com")
            .build();
    }

    @Override
    public boolean sendDownloadEmail(String toEmail, String downloadUrl, String productDisplayName) {
        try {
            Map<String, Object> body = Map.of(
                "from", props.getFrom(),
                "to",   List.of(toEmail),
                "subject", "Tu descarga: " + productDisplayName,
                "html", buildHtml(downloadUrl, productDisplayName),
                "text", buildText(downloadUrl, productDisplayName)
            );
            restClient.post()
                .uri("/emails")
                .header("Authorization", "Bearer " + props.getApiKey())
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .toBodilessEntity();
            return true;  // 2xx
        } catch (RestClientException ex) {
            log.error("Resend delivery failed for email={}: {}", toEmail, ex.getMessage(), ex);
            return false;  // caller decides what to do
        }
    }
}
```

**Email template**: HTML inline with inline styles (no Thymeleaf/Freemarker). Contains: product name, primary CTA button linking to `downloadUrl`, plain-text fallback with the URL. Minimal — button + URL only.

**Error handling contract**: `sendDownloadEmail` MUST NOT propagate `RestClientException` or any other unchecked exception to the caller. It catches internally, logs at ERROR, and returns `false`. The caller (`PurchaseService.markPaid` and `ReconciliationService.retryFailedEmails`) checks the boolean and updates `emailSentAt` only on `true`.

**emailSentAt update responsibility**: when `sendDownloadEmail` returns `true`, the calling service MUST call `purchaseRepository.updateEmailSentAt(purchaseId, Instant.now())` to persist the delivery timestamp. This update happens in the same transaction as the email call where possible, or as a subsequent update if outside a transaction boundary.

## 7. Scheduler de reconciliación

**Cron expression**: `"0 */15 * * * *"` (every 15 min on the minute). Matches the existing `RefreshTokenCleanupJob` cron style.

**Note**: `@EnableScheduling` is already present in `Application.java` — no change needed.

The scheduler executes two functions per tick, in order: **Function A** (PENDING reconciliation) then **Function B** (email retry).

### Function A — Reconcile PENDING purchases with Stripe

```
findPendingOlderThan(now() - 1 hour)  →  List<Purchase>
for each purchase:
  pi = Stripe.PaymentIntent.retrieve(purchase.stripePaymentIntentId)
  switch pi.getStatus():
    "succeeded":
      rows = markPaid(piId, now())
      if rows == 1:
        sent = sendDownloadEmail(...)  // idempotent guard via rowsAffected
        if sent: updateEmailSentAt(purchase.id, now())
    "canceled", "payment_failed":
      markFailed(piId)
    "requires_payment_method", "requires_action", "processing":
      skip (still live)
```

**Concurrency**: `markPaid` / `markFailed` use `UPDATE ... WHERE status = 'PENDING'` — atomic at DB level. A race between a late webhook and the scheduler resolves correctly: whichever `UPDATE` runs first wins; the second gets `rowsAffected == 0` and skips the email. No distributed lock needed (single instance).

### Function B — Retry email for PAID purchases without emailSentAt

Implemented in `application/service/ReconciliationService.retryFailedEmails()`. Called by the scheduler after Function A completes.

```
graceCutoff = now() - 5 minutes
findPaidWithoutEmail(graceCutoff)  →  List<Purchase>
for each purchase:
  sent = sendDownloadEmail(purchase.email, downloadUrl, productName)
  if sent:
    updateEmailSentAt(purchase.id, now())
    log INFO "Email resent for purchase {id}"
  else:
    log WARN "Email retry failed for purchase {id} — will retry next tick"
```

The 5-minute grace period (`paid_at < now() - 5 minutes`) prevents sending duplicate emails when the webhook is still active (e.g., the webhook set PAID but `updateEmailSentAt` hasn't committed yet). This window makes a double-send extremely unlikely but not impossible (accepted risk).

**Application service**: `ReconciliationService.java` (NEW in `application/service/`). The scheduler (`PurchaseReconciliationScheduler`) delegates to both `PurchaseService` and `ReconciliationService` to keep concerns separated.

## 8. Frontend — estructura

```
frontend/src/
  constants/routes.ts          MODIFY  add: download: (token: string) => `/descarga/${token}`
  api/paymentApi.ts            MODIFY  signature: createPaymentIntent({email, productId}) → {clientSecret, downloadToken}
  api/downloadApi.ts           NEW     downloadUrl(token): builds absolute URL to GET /api/v1/downloads/{token}
                                       purchaseInfoUrl(token): builds URL to GET /api/v1/downloads/{token}/info
  components/PaymentModal.tsx  MODIFY  see state machine below
  components/MailcheckHint.tsx NEW     inline suggestion "¿Quisiste decir @gmail.com?"
  pages/DownloadPage.tsx       NEW     route /descarga/:token — loads /info, download button + register CTA
  App.tsx                      MODIFY  register <Route path="/descarga/:token" element={<DownloadPage />} />
  package.json                 MODIFY  add mailcheck + @types/mailcheck
```

### PaymentModal state machine

The modal uses a two-step flow. There is NO `LinkAuthenticationElement` — email is captured with a plain HTML input BEFORE mounting `<Elements>`.

```
idle
  │  isOpen=true
  ▼
email-entry        ← plain <input type="email"> + MailcheckHint
  │  email valid → "Continuar" enabled
  │  user clicks "Continuar" → call createPaymentIntent({email, productId})
  ▼
loading-intent     ← spinner "Cargando..."
  │  success → store {clientSecret, downloadToken}
  │  error → back to email-entry with error message
  ▼
payment-confirming ← <Elements clientSecret={clientSecret}> + <PaymentElement> + "Pagar" button
  │                  "Volver" button → back to email-entry (email preserved in state)
  │  user clicks "Pagar" → stripe.confirmPayment(elements, ...)
  │  success
  ▼
success
  │  setTimeout 1500ms
  ▼
redirect to ROUTES.download(downloadToken)  [via useNavigate]
```

**Re-entry from payment-confirming**: if the user clicks "Volver" and changes the email, clicking "Continuar" again triggers a new `createPaymentIntent` call. Each call produces a **new** `Purchase(PENDING)` with a new `paymentIntentId` and a new `downloadToken`. The previous PENDING purchase is orphaned — the `PurchaseReconciliationScheduler` will mark it `FAILED` when Stripe confirms the PaymentIntent was canceled or expired. The `downloadToken` used for the final redirect is always the most recent one.

`redirect: 'if_required'` is passed to `stripe.confirmPayment` — avoids full-page redirect for cards that don't need 3DS.

### DownloadPage flow

`/descarga/:token`:
1. Extract `token` from `useParams()`.
2. On mount: call `GET /api/v1/downloads/{token}/info` → store `{ email, productName, status, amountCents, currency }`.
3. Render based on `status`:
   - `PAID`: "Descargar PDF" button enabled → on click, `window.open(downloadApi.downloadUrl(token), '_blank')`.
   - `PENDING`: info message "Tu compra está siendo procesada. Revisa tu email en unos minutos." (no download button).
   - `FAILED`: error message "El pago no se pudo completar." (no download button).
   - `/info` returns 404: "Enlace no válido." (no download button, no CTA).
4. Below the main section (when `/info` succeeded and `email` is available): CTA card → `<a href={ROUTES.register + '?email=' + encodeURIComponent(email)}>Crea tu cuenta</a>`.
5. No authentication required. Email is sourced from the `/info` response, NOT from sessionStorage.

## 9. Configuración

### Nuevas env vars

| Variable | Ejemplo prod | Ejemplo dev | Dónde se usa |
|----------|-------------|-------------|--------------|
| `STRIPE_WEBHOOK_SECRET` | `whsec_xxx` | `whsec_test_xxx` | `StripeProperties.webhookSecret` (ya declarado) |
| `RESEND_API_KEY` | `re_xxx` | `re_test_xxx` | `ResendProperties.apiKey` |
| `RESEND_FROM_EMAIL` | `noreply@akademia.diegobarrioh.dev` | `onboarding@resend.dev` | `ResendProperties.from` |
| `PRODUCTS_STORAGE_PATH` | `/data/akademia-products` | `/tmp/akademia-products` | `ProductsProperties.storagePath` |

> `APP_BASE_URL` is NOT needed — email download link uses `app.frontend-url` (already declared as `FRONTEND_URL`).

### application.properties additions

```properties
# Products — digital file storage
app.products.storage-path=${PRODUCTS_STORAGE_PATH:/tmp/akademia-products}

# Email — Resend
app.email.resend.api-key=${RESEND_API_KEY:}
app.email.resend.from=${RESEND_FROM_EMAIL:onboarding@resend.dev}
```

### @EnableScheduling
Already active — `Application.java` has `@EnableScheduling`. No change required.

### SecurityConfig change
Add to `authorizeHttpRequests`:
```java
.requestMatchers(HttpMethod.GET, "/api/v1/downloads/**").permitAll()
```

## 10. Testing strategy

| Component | Type | Tool | Notes |
|-----------|------|------|-------|
| `PurchaseService` — status transitions | Unit | JUnit 5 + Mockito | Mock all ports; verify `markPaid` called once, email called only when `rowsAffected==1`; verify `updateEmailSentAt` called when `sendDownloadEmail` returns `true`; verify `updateEmailSentAt` NOT called when `sendDownloadEmail` returns `false` |
| `PurchaseService` — idempotency | Unit | Mockito | Stub `markPaid` → returns 0; assert email NOT sent |
| `PurchaseService.getInfo` | Unit | Mockito | Token exists → returns `PurchaseInfo` with resolved `productName`; token missing → `NoSuchElementException` |
| `InMemoryProductCatalog` | Unit | JUnit 5 | findById known SKU / unknown SKU |
| `StripeEventVerifierAdapter` | Unit | JUnit 5 | Compute real HMAC signature with fixed secret + payload; assert success. Invalid sig → exception. |
| `ResendEmailAdapter` | Unit | MockWebServer (OkHttp) | Wire a fake HTTP server; assert correct headers + body |
| `PurchaseReconciliationScheduler` + `ReconciliationService` | Unit | Mockito | Function A: mock `PurchaseRepository` + Stripe client; assert markPaid for `succeeded`, markFailed for `canceled`, skip for `processing`, `updateEmailSentAt` called when email sends successfully. Function B: stub `findPaidWithoutEmail` with 2 purchases; mock `sendDownloadEmail` returning true for #1 and false for #2; assert `updateEmailSentAt` called only for #1 |
| `PaymentController.createIntent` | Integration | `@SpringBootTest` + Testcontainers Postgres + MockBean StripePaymentAdapter | Assert 200 + body shape; assert 400 on missing email |
| `PaymentController.webhook` | Integration | `@SpringBootTest` + valid computed signature | Assert 200 idempotent on double delivery; Assert 400 on bad signature |
| `DownloadController` | Integration | `@SpringBootTest` + seed Purchase(PAID) | Assert 200 + PDF headers; Assert 404 for PENDING token; Assert 404 for unknown UUID. For `/info`: Assert 200 + JSON for PAID, PENDING, FAILED tokens; Assert 404 for unknown token; Assert 400 for malformed UUID |
| `PaymentModal` | Component | Vitest + Testing Library | Step 1: assert email input visible, "Continuar" disabled until valid email entered. Step 2: mock `createPaymentIntent` success → assert `<PaymentElement>` renders; assert "Volver" returns to Step 1 preserving email. Error path: mock `createPaymentIntent` error → assert error message shown. Redirect: mock `stripe.confirmPayment` success → assert navigate to `/descarga/:token` |
| `DownloadPage` | Component | Vitest + Testing Library | Mock `GET /info` returning PAID → assert "Descargar PDF" button and CTA link with email. Mock PENDING → assert info message, no download button. Mock FAILED → assert error message. Mock 404 → assert generic invalid link message |
| E2E (manual) | Manual | Stripe test card `4242 4242 4242 4242` | Full flow: pay → email received → download link → PDF |

## 11. Riesgos arquitectónicos

| Riesgo | Origen | Mitigación |
|--------|--------|------------|
| Email perdido si Resend falla durante webhook | Resend outage durante el webhook | **Resuelto**: `emailSentAt` tracking + scheduler Function B reintenta automáticamente. Navegador también redirige a `/descarga/:token` como camino primario inmediato. |
| Double-send de email (race scheduler + webhook) | `updateEmailSentAt` puede no haber commiteado cuando el scheduler evalúa | Improbable (5-minute grace period). Si ocurre, usuario recibe 2 emails idénticos — aceptado MVP. |
| Webhook recibe evento desconocido y lanza NullPointerException al extraer PI | Deserialización Stripe | Wrap `getDataObjectDeserializer()` en Optional; log + return 200 para tipos no manejados |
| Flyway conflict si otro dev añade V011 en paralelo | Dos branches activas | Comunicar en PR; el check de la migration verifica el número antes del merge |
| `LocalProductFileStorageAdapter` path traversal | `storageKey` venida del catálogo hardcoded (no de usuario) | Sanitizar con `Paths.get(storageKey).getFileName()` de todas formas; storageKey no es user input |
| PDF no presente en PRODUCTS_STORAGE_PATH en primera deploy | Operativo | Documentar checklist pre-deploy; `open()` lanza RuntimeException → 500 al usuario |
| Purchases PENDING huérfanas por re-entrada del usuario en PaymentModal | Usuario vuelve a Step 1 y cambia email → múltiples create-intent | Scheduler Function A las marca FAILED cuando Stripe confirma cancelación. Sin impacto funcional. |

## 12. Rollback / migration plan

Reutilizar el plan de `proposal.md`:

1. **Pre-merge**: descartar rama — cero impacto.
2. **Post-merge, sin compras**: `git revert -m 1 <sha>` + DROP TABLE purchases (Flyway repair).
3. **Post-merge, con compras reales**: NO revertir migración; deshabilitar scheduler con `app.scheduler.reconciliation.enabled=false` + retornar `PaymentController` a comportamiento previo en un parche; procesar manualmente vía Stripe Dashboard.
4. **Webhook problemático aislado**: deshabilitar endpoint en Stripe Dashboard sin redeploy; scheduler cubre el gap.

## Technical Approach Summary

| Layer | Approach |
|-------|----------|
| Domain | New `Purchase` aggregate with factory method; `PurchaseStatus` enum; `DigitalProduct` value object |
| Application | Single `PurchaseService` implementing 3 use cases; `InMemoryProductCatalog` as `@Component` |
| Stripe integration | Extend existing `StripePaymentAdapter`; new `StripeEventVerifierAdapter` wrapping `Webhook.constructEvent` |
| Email | `ResendEmailAdapter` using `RestClient`; returns `boolean` — never throws; `emailSentAt` tracked in `Purchase` |
| Persistence | JPA entity + Spring Data repo + `PurchaseRepositoryAdapter`; `markPaid`/`markFailed` as `@Modifying @Query`; `updateEmailSentAt` as separate update; `findPaidWithoutEmail` for scheduler Function B |
| Scheduler | `PurchaseReconciliationScheduler` (cron `0 */15 * * * *`): Function A reconciles PENDING with Stripe; Function B retries email for PAID with `emailSentAt=NULL` |
| Frontend | `PaymentModal` 2-step flow: email input → create-intent → `<Elements>` + `<PaymentElement>`; no `LinkAuthenticationElement`; `DownloadPage` calls `/info` endpoint to determine page state and source buyer email |

## Open Questions

All open questions from the previous draft have been resolved:

- [x] **Email + LinkAuthenticationElement timing** → **Resolved**: two-step PaymentModal. Plain `<input type="email">` in Step 1, `<Elements>` only mounted in Step 2 after `create-intent` responds. No `LinkAuthenticationElement`. See §8.
- [x] **`ResendEmailAdapter` error on webhook** → **Resolved**: `emailSentAt` column added to `purchases`. `sendDownloadEmail` returns boolean, never throws. Scheduler Function B retries PAID purchases with `email_sent_at IS NULL` on every 15-min tick. See §6 and §7.
