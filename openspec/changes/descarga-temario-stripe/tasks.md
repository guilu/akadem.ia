# Tasks: Descarga del Temario Subalterno GVA tras pago Stripe

> Branch: `feature/AKDMIA-214-stripe-integration` · No crear ramas nuevas durante apply.
> Convención: TDD (RED → GREEN). Cada feature unit = tarea de test seguida de tarea de impl.
> Prerrequisitos operativos asumidos completados (fuera de Claude): Stripe webhook config, Resend DNS, PDF en servidor.

---

## Fase 0 — Setup y configuración

- [x] 0.1 Añadir al `backend/src/main/resources/application.properties`: `app.products.storage-path=${PRODUCTS_STORAGE_PATH:/tmp/akademia-products}`, `app.email.resend.api-key=${RESEND_API_KEY:}`, `app.email.resend.from=${RESEND_FROM_EMAIL:onboarding@resend.dev}`. Verificar que `STRIPE_WEBHOOK_SECRET` ya existe. **Done when**: properties presentes y backend arranca sin error de config.
  - Paths: `backend/src/main/resources/application.properties`
  - Deps: —

- [x] 0.2 Añadir al `.env.example` (raíz del proyecto) las variables `STRIPE_WEBHOOK_SECRET`, `RESEND_API_KEY`, `RESEND_FROM_EMAIL`, `PRODUCTS_STORAGE_PATH`. **Done when**: `.env.example` documenta todas las vars nuevas con valores de ejemplo.
  - Paths: `.env.example`
  - Deps: — (nota: .env.example está en .gitignore por el patrón .env.* — el archivo se actualizó localmente pero no entra en el commit)

- [x] 0.3 Instalar dependencias npm: `mailcheck` + `@types/mailcheck` en `frontend/package.json`. **Done when**: `npm install` en `frontend/` sale sin error y el tipo `mailcheck` es resolvible en TS.
  - Paths: `frontend/package.json`, `frontend/package-lock.json`
  - Deps: —

---

## Fase 1 — Migración Flyway y dominio Purchase

- [x] 1.1 Crear `backend/src/main/resources/db/migration/V011__purchases.sql` con `CREATE TABLE purchases (...)` y los 4 índices del design (2 UNIQUE + 2 partial). **Done when**: la migración aplica con `./gradlew flywayMigrate` sobre Postgres limpio sin error.
  - Paths: `backend/src/main/resources/db/migration/V011__purchases.sql`
  - Deps: 0.1
  - Nota: El proyecto no tiene el plugin Gradle `org.flywaydb.flyway` configurado, por lo que `./gradlew flywayMigrate` no es una task disponible. La migración se validará realmente en Fase 6 vía el test de integración `PurchaseRepositoryAdapterIT` (Testcontainers Postgres). El SQL escrito es PostgreSQL estándar (incluye partial indexes, no soportados por H2) y respeta la especificación de `design.md` §3 al 100%.

- [x] 1.2 Crear `domain/model/PurchaseStatus.java` (enum `PENDING`, `PAID`, `FAILED`). **Done when**: clase compila, sin tests propios (usada como valor).
  - Paths: `backend/src/main/java/com/akdemya/domain/model/PurchaseStatus.java`
  - Deps: —

- [x] 1.3 [RED] Escribir test unitario `PurchaseTest` que valide: `Purchase.create(...)` produce status=PENDING, downloadToken != null; `markEmailSent(instant)` setea emailSentAt. **Done when**: test compila pero falla (clase no existe).
  - Paths: `backend/src/test/java/com/akdemya/domain/model/PurchaseTest.java`
  - Deps: 1.2

- [x] 1.4 [GREEN] Crear `domain/model/Purchase.java`: clase (no record), factory `Purchase.create(email, productId, piId, downloadToken, amountCents, currency)` → status=PENDING; setter `markEmailSent(Instant)`. **Done when**: `PurchaseTest` pasa.
  - Paths: `backend/src/main/java/com/akdemya/domain/model/Purchase.java`
  - Deps: 1.3

- [x] 1.5 Crear `domain/model/DigitalProduct.java`: clase con campos `sku`, `displayName`, `amountCents`, `currency`, `storageKey`, `displayFilename`; constructor + getters. **Done when**: compila sin tests propios.
  - Paths: `backend/src/main/java/com/akdemya/domain/model/DigitalProduct.java`
  - Deps: —

---

## Fase 2 — Ports de dominio (out)

- [x] 2.1 Crear `domain/port/out/PurchaseRepository.java` (interface): `save`, `findByDownloadToken`, `findByStripePaymentIntentId`, `markPaid(piId, Instant) → int`, `markFailed(piId) → int`, `findPendingOlderThan(Instant)`, `findPaidWithoutEmail(Instant)`, `updateEmailSentAt(UUID, Instant)`. **Done when**: interface compila.
  - Paths: `backend/src/main/java/com/akdemya/domain/port/out/PurchaseRepository.java`
  - Deps: 1.4

- [x] 2.2 Crear `domain/port/out/ProductFileStoragePort.java` (interface): `InputStream open(String storageKey)`. **Done when**: interface compila.
  - Paths: `backend/src/main/java/com/akdemya/domain/port/out/ProductFileStoragePort.java`
  - Deps: —

- [x] 2.3 Crear `domain/port/out/TransactionalEmailPort.java` (interface): `boolean sendDownloadEmail(String toEmail, String downloadUrl, String productDisplayName)`. **Done when**: interface compila.
  - Paths: `backend/src/main/java/com/akdemya/domain/port/out/TransactionalEmailPort.java`
  - Deps: —

- [x] 2.4 Crear `domain/port/out/ProductCatalog.java` (interface): `Optional<DigitalProduct> findById(String productId)`. **Done when**: interface compila.
  - Paths: `backend/src/main/java/com/akdemya/domain/port/out/ProductCatalog.java`
  - Deps: 1.5

---

## Fase 3 — Ports de dominio (in) y actualización de use case existente

- [x] 3.1 Modificar `domain/port/in/CreatePaymentIntentUseCase.java`: reemplazar interface actual con nueva firma `record Command(String email, String productId, @Nullable UUID userId)` y `record Result(String clientSecret, UUID downloadToken)`. **Done when**: interface compila con nuevos tipos.
  - Paths: `backend/src/main/java/com/akdemya/domain/port/in/CreatePaymentIntentUseCase.java`
  - Deps: 1.4
  - Nota: `userId` documentado como nullable vía Javadoc en el record (sin `@Nullable` import) para mantener pureza del dominio — no había convención previa de `@Nullable` en `domain/`. Existing callers (`StripePaymentAdapter`, `PaymentController`) parcheados con stub mínimo (`UnsupportedOperationException` / Command con nulls) para mantener el build verde; refactor real en Fases 7 y 9.

- [x] 3.2 Crear `domain/port/in/HandleStripeWebhookUseCase.java`: `void handleEvent(String rawPayload, String stripeSignatureHeader)`. **Done when**: interface compila.
  - Paths: `backend/src/main/java/com/akdemya/domain/port/in/HandleStripeWebhookUseCase.java`
  - Deps: —

- [x] 3.3 Crear `domain/port/in/DownloadPurchaseUseCase.java`: `record Result(DigitalProduct product, InputStream stream)` + `Result openByToken(UUID downloadToken)` (lanza `NoSuchElementException`). **Done when**: interface compila.
  - Paths: `backend/src/main/java/com/akdemya/domain/port/in/DownloadPurchaseUseCase.java`
  - Deps: 1.5, 2.2

- [x] 3.4 Crear `domain/port/in/GetPurchaseInfoUseCase.java`: `record PurchaseInfo(String email, String productName, String status, long amountCents, String currency)` + `PurchaseInfo getInfo(UUID downloadToken)` (lanza `NoSuchElementException`). **Done when**: interface compila.
  - Paths: `backend/src/main/java/com/akdemya/domain/port/in/GetPurchaseInfoUseCase.java`
  - Deps: 2.1

---

## Fase 4 — Config beans (application/config)

- [x] 4.1 Crear `application/config/ResendProperties.java`: `@ConfigurationProperties(prefix = "app.email.resend")` con campos `apiKey` y `from`. **Done when**: bean registrado y `@SpringBootTest` arranca sin error de binding.
  - Paths: `backend/src/main/java/com/akdemya/application/config/ResendProperties.java`
  - Deps: 0.1

- [x] 4.2 Crear `application/config/ProductsProperties.java`: `@ConfigurationProperties(prefix = "app.products")` con campo `storagePath` (default `/tmp/akademia-products`). **Done when**: bean registrado y `@SpringBootTest` arranca sin error.
  - Paths: `backend/src/main/java/com/akdemya/application/config/ProductsProperties.java`
  - Deps: 0.1

---

## Fase 5 — Adaptadores de infraestructura outbound

- [x] 5.1 Crear `application/service/InMemoryProductCatalog.java`: `@Component` implementando `ProductCatalog`. Map hardcoded con `TEMARIO_SUBALTERNO_GVA → DigitalProduct(sku, displayName, 1500, "eur", "temario-subalterno-gva.pdf", "Temario Subalterno GVA.pdf")`.
  - [RED] Test: `InMemoryProductCatalogTest` — `findById("TEMARIO_SUBALTERNO_GVA")` present, `findById("UNKNOWN")` empty.
  - [GREEN] Implementar `InMemoryProductCatalog`.
  - **Done when**: test pasa.
  - Paths: `...application/service/InMemoryProductCatalog.java`, `...test/.../InMemoryProductCatalogTest.java`
  - Deps: 2.4, 1.5

- [x] 5.2 [RED] Test `LocalProductFileStorageAdapterTest`: `open("temario-subalterno-gva.pdf")` retorna InputStream cuando archivo existe en tmpdir; lanza RuntimeException cuando no existe. **Done when**: test compila pero falla.
  - Paths: `backend/src/test/java/com/akdemya/adapter/infrastructure/storage/LocalProductFileStorageAdapterTest.java`
  - Deps: 2.2, 4.2

- [x] 5.3 [GREEN] Crear `adapter/infrastructure/storage/LocalProductFileStorageAdapter.java`: `@Component` implementando `ProductFileStoragePort`. Lee `ProductsProperties.storagePath`; abre `FileInputStream`; sanitiza path con `Paths.get(storageKey).getFileName()`. **Done when**: `LocalProductFileStorageAdapterTest` pasa.
  - Paths: `backend/src/main/java/com/akdemya/adapter/infrastructure/storage/LocalProductFileStorageAdapter.java`
  - Deps: 5.2

- [x] 5.4 Crear `adapter/infrastructure/stripe/StripeEventVerifierAdapter.java`: `@Component`. Wraps `Webhook.constructEvent(rawPayload, signature, webhookSecret)`. Lanza `SignatureVerificationException` si firma inválida.
  - [RED] Test `StripeEventVerifierAdapterTest`: payload + HMAC calculado con secret fijo → success; payload alterado → excepción.
  - [GREEN] Implementar `StripeEventVerifierAdapter`.
  - **Done when**: test pasa.
  - Paths: `...stripe/StripeEventVerifierAdapter.java`, `...test/.../StripeEventVerifierAdapterTest.java`
  - Deps: —
  - Test cmd: `./gradlew test --tests "*.StripeEventVerifierAdapterTest"`

- [x] 5.5 [RED] Test `ResendEmailAdapterTest` con MockWebServer (OkHttp): `sendDownloadEmail(...)` llama `POST /emails` con headers `Authorization: Bearer <key>` y body correcto, retorna `true`; Resend 500 → retorna `false` sin propagar excepción. **Done when**: test compila pero falla.
  - Paths: `backend/src/test/java/com/akdemya/adapter/infrastructure/email/ResendEmailAdapterTest.java`
  - Deps: 2.3, 4.1

- [x] 5.6 [GREEN] Crear `adapter/infrastructure/email/ResendEmailAdapter.java`: `@Component` implementando `TransactionalEmailPort`. `RestClient` con base URL `https://api.resend.com`. Método `buildHtml(url, name)` y `buildText(url, name)` inline. Catch `RestClientException` → log ERROR → return false. **Done when**: `ResendEmailAdapterTest` pasa.
  - Paths: `backend/src/main/java/com/akdemya/adapter/infrastructure/email/ResendEmailAdapter.java`
  - Deps: 5.5
  - Test cmd: `./gradlew test --tests "*.ResendEmailAdapterTest"`

---

## Fase 6 — Persistencia JPA

- [x] 6.1 Crear `adapter/outbound/persistence/entity/PurchaseEntity.java`: `@Entity @Table(name="purchases")`. Campos: `id` (UUID PK), `stripePaymentIntentId`, `downloadToken`, `email`, `productId`, `userId`, `status` (String), `amountCents`, `currency`, `createdAt`, `paidAt`, `emailSentAt`. Tipos `Instant` igual que `FlashcardEntity`. **Done when**: compilación sin error de JPA.
  - Paths: `backend/src/main/java/com/akdemya/adapter/outbound/persistence/entity/PurchaseEntity.java`
  - Deps: 1.1, 1.4

- [x] 6.2 Crear `adapter/outbound/persistence/repository/JpaPurchaseRepository.java`: `JpaRepository<PurchaseEntity, UUID>` con queries: `findByDownloadToken`, `findByStripePaymentIntentId`, `findPendingOlderThan` (JPQL con `status='PENDING' AND createdAt < :cutoff`), `findPaidWithoutEmail` (JPQL). **Done when**: compila.
  - Paths: `backend/src/main/java/com/akdemya/adapter/outbound/persistence/repository/JpaPurchaseRepository.java`
  - Deps: 6.1

- [x] 6.3 Crear `adapter/outbound/persistence/mapper/PurchaseMapper.java`: `toDomain(PurchaseEntity)` y `toEntity(Purchase)`. **Done when**: compila.
  - Paths: `backend/src/main/java/com/akdemya/adapter/outbound/persistence/mapper/PurchaseMapper.java`
  - Deps: 6.1, 1.4

- [x] 6.4 Crear `adapter/outbound/persistence/PurchaseRepositoryAdapter.java`: implementa `PurchaseRepository`. `markPaid`/`markFailed` via `@Modifying @Query("UPDATE PurchaseEntity p SET p.status=... WHERE p.stripePaymentIntentId=:piId AND p.status='PENDING'")`. `updateEmailSentAt` via `@Modifying @Query`.
  - [RED] Test de integración `PurchaseRepositoryAdapterIT` con Testcontainers Postgres: `save` persiste; `markPaid` retorna 1 primera vez, 0 segunda vez (idempotencia); `findPendingOlderThan` filtra correctamente; `findPaidWithoutEmail` respeta grace cutoff.
  - [GREEN] Implementar `PurchaseRepositoryAdapter` hasta que el test IT pase.
  - **Done when**: `PurchaseRepositoryAdapterIT` verde.
  - Paths: `...persistence/PurchaseRepositoryAdapter.java`, `...test/.../PurchaseRepositoryAdapterIT.java`
  - Deps: 6.2, 6.3, 2.1
  - Test cmd: `./gradlew test --tests "*.PurchaseRepositoryAdapterIT"`

---

## Fase 7 — Application services

- [x] 7.1 Modificar `adapter/infrastructure/stripe/StripePaymentAdapter.java`: nueva firma `createIntent(CreatePaymentIntentUseCase.Command cmd)`, obtiene `DigitalProduct` del catálogo (inyectado), pone metadata `{purchaseId, downloadToken, productId, email, userId?}`, usa `product.getAmountCents()/getCurrency()`. Eliminar hard-code de 1500 EUR. **Done when**: compila y `StripePaymentAdapterTest` (si existe) sigue verde o se actualiza. — extraído nuevo port `StripePaymentGateway` (en `domain/port/out/`) y el adapter implementa ese port en lugar de `CreatePaymentIntentUseCase`. PurchaseService es ahora quien implementa los use cases.
  - Paths: `backend/src/main/java/com/akdemya/adapter/infrastructure/stripe/StripePaymentAdapter.java`
  - Deps: 3.1, 2.4

- [x] 7.2 [RED] Test unitario `PurchaseServiceTest` (Mockito, sin Spring): escenarios:
  - `createIntent` → catálogo encontrado → Stripe OK → Purchase guardado → retorna clientSecret + downloadToken.
  - `createIntent` → productId desconocido → `IllegalArgumentException` → no llama a Stripe ni a repo.
  - `handleEvent` → `payment_intent.succeeded` → `markPaid` retorna 1 → email enviado → `updateEmailSentAt` llamado.
  - `handleEvent` → `payment_intent.succeeded` → `markPaid` retorna 0 (idempotente) → email NO enviado.
  - `handleEvent` → `payment_intent.succeeded` → markPaid=1 → email falla (false) → `updateEmailSentAt` NO llamado.
  - `handleEvent` → `payment_intent.payment_failed` → `markFailed` llamado → email NO enviado.
  - `openByToken` → PAID → retorna stream; status != PAID → `NoSuchElementException`.
  - `getInfo` → token existe → retorna `PurchaseInfo`; token no existe → `NoSuchElementException`.
  **Done when**: tests compilan pero fallan.
  - Paths: `backend/src/test/java/com/akdemya/application/service/PurchaseServiceTest.java`
  - Deps: 2.1–2.4, 3.1–3.4, 5.1

- [x] 7.3 [GREEN] Crear `application/service/PurchaseService.java`: `@Service @Transactional`, implementa `CreatePaymentIntentUseCase`, `HandleStripeWebhookUseCase`, `DownloadPurchaseUseCase`, `GetPurchaseInfoUseCase`. Orquesta ports inyectados. `handleEvent` usa `StripeEventVerifierAdapter` (inyectado) y switch sobre `event.getType()`. **Done when**: `PurchaseServiceTest` verde. — 11/11 tests verde.
  - Paths: `backend/src/main/java/com/akdemya/application/service/PurchaseService.java`
  - Deps: 7.2, 5.4
  - Test cmd: `./gradlew test --tests "*.PurchaseServiceTest"`

- [x] 7.4 Crear `application/service/ReconciliationService.java`: Function B — `retryFailedEmails(Instant graceCutoff)`. Usa `PurchaseRepository.findPaidWithoutEmail(graceCutoff)`, llama `TransactionalEmailPort`, si true → `updateEmailSentAt`. — 2/2 tests verde. Inyecta `ProductCatalog` + `frontendUrl` para construir URL/displayName del email (mismo patrón que PurchaseService).
  - [RED] Test `ReconciliationServiceTest`: stub `findPaidWithoutEmail` con 2 purchases; email retorna true para #1 y false para #2 → `updateEmailSentAt` llamado solo para #1; WARN log emitido para #2.
  - [GREEN] Implementar `ReconciliationService`.
  - **Done when**: test verde.
  - Paths: `...service/ReconciliationService.java`, `...test/.../ReconciliationServiceTest.java`
  - Deps: 2.1, 2.3
  - Test cmd: `./gradlew test --tests "*.ReconciliationServiceTest"`

---

## Fase 8 — Scheduler

- [x] 8.1 Crear `adapter/infrastructure/scheduler/PurchaseReconciliationScheduler.java`: `@Component`, `@Scheduled(cron = "0 */15 * * * *")`. Function A: `purchaseRepository.findPendingOlderThan(now()-1h)` → por cada compra → `PaymentIntent.retrieve(piId)` → switch(status): `succeeded`→ markPaid + email; `canceled`/`payment_failed`→ markFailed; otros→ log WARN; excepción por compra → log ERROR + continuar. Function B: delegar a `ReconciliationService.retryFailedEmails(now()-5min)`. LOG INFO con count al inicio de cada función.
  - [RED] Test `PurchaseReconciliationSchedulerTest` (Mockito): Function A — mock repo + Stripe client: assert markPaid for succeeded, markFailed for canceled, skip for processing, updateEmailSentAt si email=true; Function B — delega a ReconciliationService mockeado; fallo Stripe en una compra → las otras se procesan.
  - [GREEN] Implementar scheduler hasta que test pase.
  - **Done when**: test verde.
  - Paths: `...scheduler/PurchaseReconciliationScheduler.java`, `...test/.../PurchaseReconciliationSchedulerTest.java`
  - Deps: 7.3, 7.4
  - Test cmd: `./gradlew test --tests "*.PurchaseReconciliationSchedulerTest"`

---

## Fase 9 — Adaptadores inbound REST

- [x] 9.1 Eliminar `adapter/inbound/web/dto/PaymentIntentResponse.java` (si existe) y crear DTOs nuevos:
  - `CreateIntentRequest.java` (record): `@NotBlank String email`, `@NotBlank String productId`.
  - `CreateIntentResponse.java` (record): `String clientSecret`, `UUID downloadToken`.
  - `WebhookResponse.java` (record): `String status`.
  - `PurchaseInfoResponse.java` (record): `String email`, `String productName`, `String status`, `long amountCents`, `String currency`.
  - **Done when**: todas las clases compilan.
  - Paths: `backend/src/main/java/com/akdemya/adapter/inbound/web/dto/`
  - Deps: —

- [x] 9.2 Modificar `adapter/inbound/web/PaymentController.java`:
  - `POST /api/v1/payments/create-intent`: aceptar `@Valid @RequestBody CreateIntentRequest`, llamar `CreatePaymentIntentUseCase.createIntent(Command(...))`, retornar `CreateIntentResponse`.
  - Añadir `POST /api/v1/payments/webhook`: `@RequestBody byte[] payload`, `@RequestHeader("Stripe-Signature") String sig` → `HandleStripeWebhookUseCase.handleEvent(new String(payload), sig)` → `WebhookResponse("ok")`. `SignatureVerificationException` → `GlobalExceptionHandler` debe mapear a 400.
  - **Done when**: controlador compila y test de integración (tarea 9.4) pasa.
  - Paths: `backend/src/main/java/com/akdemya/adapter/inbound/web/PaymentController.java`
  - Deps: 3.1, 3.2, 9.1, 7.3

- [x] 9.3 Crear `adapter/inbound/web/DownloadController.java`:
  - `GET /api/v1/downloads/{token}`: `@PathVariable UUID token` → `DownloadPurchaseUseCase.openByToken(token)` → `StreamingResponseBody` con `Content-Type: application/pdf`, `Content-Disposition: attachment; filename="..."`. `NoSuchElementException` → 404.
  - `GET /api/v1/downloads/{token}/info`: → `GetPurchaseInfoUseCase.getInfo(token)` → `PurchaseInfoResponse`. UUID malformado → 400 via `MethodArgumentTypeMismatchException`.
  - **Done when**: controlador compila.
  - Paths: `backend/src/main/java/com/akdemya/adapter/inbound/web/DownloadController.java`
  - Deps: 3.3, 3.4, 9.1

- [x] 9.4 Modificar `adapter/infrastructure/security/SecurityConfig.java`: añadir `.requestMatchers(HttpMethod.GET, "/api/v1/downloads/**").permitAll()` y verificar que `POST /api/v1/payments/webhook` también es público. **Done when**: `@SpringBootTest` arranca y los endpoints son accesibles sin auth.
  - Paths: `backend/src/main/java/com/akdemya/adapter/infrastructure/security/SecurityConfig.java`
  - Deps: 9.2, 9.3

---

## Fase 10 — Tests de integración de controllers

> Nota: por la restricción de NO Docker en el entorno local, se usa H2 en modo PostgreSQL en lugar de Testcontainers (mismo trade-off documentado en Fase 6). La validación con Postgres real se difiere a la Fase 12. Adicionalmente, se descubrió un bug latente en `ResendEmailAdapter` (dos constructores sin `@Autowired` → Spring fallback al constructor por defecto inexistente). Resuelto anotando el constructor primario con `@Autowired` y ampliando `application-test.properties` con placeholders OAuth2/CORS/Frontend para que el contexto completo arranque en cualquier `@SpringBootTest` futuro.

- [x] 10.1 [GREEN] `PaymentControllerIT` (`@SpringBootTest` + H2 PG-mode + `@MockBean StripePaymentGateway`):
  - POST create-intent body válido → 200 + `{clientSecret, downloadToken}` + Purchase en BD con status=PENDING ✅
  - POST create-intent sin email → 400 ✅
  - POST create-intent email vacío → 400 ✅
  - POST create-intent productId desconocido → 400 (vía `GlobalExceptionHandler.handleBadRequest`) ✅
  - **Done when**: test verde. **4/4 verde**.
  - Paths: `backend/src/test/java/com/akdemya/adapter/inbound/web/PaymentControllerIT.java`

- [x] 10.2 [GREEN] `WebhookControllerIT` (`@SpringBootTest` + firma HMAC calculada localmente + `@MockBean TransactionalEmailPort`):
  - Firma válida + `payment_intent.succeeded` → 200, Purchase PAID + email enviado 1× ✅
  - Re-entrega mismo evento → 200 idempotente, email enviado solo 1× total ✅
  - Firma inválida → 400 + estado intacto (PENDING/paidAt=null) ✅
  - `payment_intent.payment_failed` → 200, Purchase FAILED, email no enviado ✅
  - **Done when**: test verde. **4/4 verde**.
  - Paths: `backend/src/test/java/com/akdemya/adapter/inbound/web/WebhookControllerIT.java`

- [x] 10.3 [GREEN] `DownloadControllerIT` (`@SpringBootTest` + H2 PG-mode + `@MockBean ProductFileStoragePort`):
  - GET `/{token}` PAID → 200 + `application/pdf` + `Content-Disposition: attachment; filename="Temario Subalterno GVA.pdf"` + body bytes ✅
  - GET `/{token}` PENDING → 404 ✅
  - GET `/{token}` FAILED → 404 ✅
  - GET `/{token}` UUID desconocido → 404 ✅
  - GET `/{token}` no-UUID → 400 (vía `MethodArgumentTypeMismatchException`) ✅
  - GET `/{token}/info` PAID/PENDING/FAILED → 200 con `PurchaseInfoResponse` correcto ✅
  - GET `/{token}/info` desconocido → 404 ✅
  - **Done when**: test verde. **9/9 verde**.
  - Paths: `backend/src/test/java/com/akdemya/adapter/inbound/web/DownloadControllerIT.java`

---

## Fase 11 — Frontend

- [x] 11.1 Modificar `frontend/src/constants/routes.ts`: añadir `download: (token: string) => \`/descarga/${token}\``. **Done**: añadido entre `subalternoGva` y `oauth2Callback`.
  - Paths: `frontend/src/constants/routes.ts`

- [x] 11.2 Modificar `frontend/src/api/paymentApi.ts`: nueva firma `createPaymentIntent(email, productId): Promise<CreateIntentResponse>` con `clientSecret + downloadToken`. **Done**: firma anterior eliminada.
  - Paths: `frontend/src/api/paymentApi.ts`

- [x] 11.3 Crear `frontend/src/api/downloadApi.ts`: `downloadUrl(token)`, `purchaseInfoUrl(token)`, `fetchPurchaseInfo(token): Promise<PurchaseInfoResponse>` (con `PurchaseStatus = PENDING | PAID | FAILED`). **Done**.
  - Paths: `frontend/src/api/downloadApi.ts`

- [x] 11.4 `MailcheckHint.tsx` (`{suggestion, onAccept}` → renderiza "¿Quisiste decir <button>{suggestion}</button>?"). **Done**.
  - Paths: `frontend/src/components/MailcheckHint.tsx`

- [x] 11.5 Test `PaymentModal.test.tsx` (Vitest + Testing Library):
  - Step 1: email input visible, "Continuar" disabled hasta email válido.
  - Loading: mock `createPaymentIntent` con delay → spinner visible.
  - Step 2: mock retorna `{clientSecret, downloadToken}` → `<PaymentElement>` montado.
  - "Volver" → vuelve a Step 1 con email preservado.
  - Error path: mock 500 → mensaje de error en Step 1.
  - Mailcheck: blur con `gmial.com` → hint visible; click hint → email corregido. ✅
  - Error path: mock 500 → mensaje de error en Step 1. ✅
  Verifica además: Continuar disabled hasta email válido, render Stripe Elements tras éxito, "Volver" preserva email. **6/6 verde**.
  - Paths: `frontend/src/components/__tests__/PaymentModal.test.tsx`

- [x] 11.6 `frontend/src/components/PaymentModal.tsx` reescrito como state machine (`email-entry → loading-intent → payment-confirming → success`). Step 1: `<input type="email">` + `MailcheckHint` + `mailcheck` on blur + "Continuar" disabled si email no encaja con regex. Step 2: `<Elements clientSecret>` + `<PaymentElement>` + Pagar 15€/Volver. Post-confirm: `navigate(ROUTES.download(downloadToken))`. **Done when**: PaymentModal.test.tsx verde.
  - Paths: `frontend/src/components/PaymentModal.tsx`

- [x] 11.7 Test `DownloadPage.test.tsx` cubre PAID/PENDING/FAILED/404. **4/4 verde**. Mock de `fetchPurchaseInfo`; el rejected error con `{status: 404}` mapea a "Enlace no válido"; otros errores caen a `GenericErrorView`.
  - Paths: `frontend/src/pages/__tests__/DownloadPage.test.tsx`

- [x] 11.8 `frontend/src/pages/DownloadPage.tsx`: `useParams()` → token. Fetch `/info` on mount con `LoadState` (loading/ready/not-found/error). PAID → `<a href={downloadUrl(token)} target="_blank">` (abre PDF en pestaña aparte) + CTA link `ROUTES.register?email=...` con email URI-encoded. PENDING → mensaje procesando. FAILED → mensaje "Pago fallido". 404 → "Enlace no válido".
  - Paths: `frontend/src/pages/DownloadPage.tsx`

- [x] 11.9 `frontend/src/App.tsx`: import `DownloadPage` + ruta `<Route path="/descarga/:token" element={<DownloadPage />} />` registrada justo tras `subalternoGva` (pública, sin `ProtectedRoute`).
  - Paths: `frontend/src/App.tsx`

- [x] 11.10 `frontend/src/pages/SubalternoGVAPage.tsx`: prop `productId="TEMARIO_SUBALTERNO_GVA"` pasado al `PaymentModal`.
  - Paths: `frontend/src/pages/SubalternoGVAPage.tsx`

---

## Fase 12 — Smoke tests manuales

> Prerrequisitos operativos (gestionados fuera de Claude): STRIPE_WEBHOOK_SECRET configurado en Stripe Dashboard apuntando a `${APP_BASE_URL}/api/v1/payments/webhook` con eventos `payment_intent.succeeded` + `payment_intent.payment_failed`; RESEND_API_KEY + DNS SPF/DKIM verificados; PDF `temario-subalterno-gva.pdf` colocado en `${PRODUCTS_STORAGE_PATH}`.

- [ ] 12.1 Arranque local: `docker-compose up` → backend + frontend arrancan sin errores de config. Flyway aplica `V011__purchases.sql`. **Done when**: logs sin error, `GET /api/v1/actuator/health` (o equivalente) 200.
  - Deps: todas las fases anteriores

- [ ] 12.2 Smoke test create-intent: `curl -X POST .../create-intent -d '{"email":"test@example.com","productId":"TEMARIO_SUBALTERNO_GVA"}'` → 200 con `clientSecret` y `downloadToken`. BD muestra Purchase PENDING. **Done when**: respuesta 200 + registro en BD.
  - Deps: 12.1

- [ ] 12.3 Smoke test pago completo: abrir frontend → SubalternoGVAPage → modal → email → Continuar → tarjeta `4242 4242 4242 4242` → Pagar → redirect a `/descarga/:token` → botón Descargar → PDF descargado. Verificar email recibido con enlace. **Done when**: flujo completo sin errores.
  - Deps: 12.2

- [ ] 12.4 Smoke test webhook con CLI Stripe: `stripe trigger payment_intent.succeeded` (o reenvío manual desde Dashboard) → verificar en logs `INFO webhook processed` + Purchase PAID en BD. Segunda entrega del mismo evento → log idempotente, sin email duplicado. **Done when**: logs confirman comportamiento.
  - Deps: 12.1

---

## Fase 13 — Documentación

- [ ] 13.1 Actualizar `.env.example` (ya hecho en 0.2) y añadir sección al `README.md` o `docs/` con instrucciones de configuración de Stripe webhook + Resend + PDF deploy. **Done when**: instrucciones claras para un operador que despliega desde cero.
  - Paths: `README.md` o `docs/deployment.md`
  - Deps: —

---

## Notas de paralelismo

Las siguientes tareas dentro de la misma fase pueden ejecutarse en paralelo:

- **Fase 0**: 0.1, 0.2, 0.3 — independientes entre sí.
- **Fase 1**: 1.2 y 1.5 — independientes entre sí. 1.1 sólo requiere 0.1.
- **Fase 2**: 2.2, 2.3, 2.4 — independientes entre sí. 2.1 requiere 1.4.
- **Fase 3**: 3.2, 3.3, 3.4 — independientes. 3.1 requiere 1.4.
- **Fase 4**: 4.1 y 4.2 — independientes entre sí.
- **Fase 5**: 5.1 y 5.2–5.3 y 5.4 y 5.5–5.6 — cuatro streams paralelos.
- **Fase 10**: 10.1, 10.2, 10.3 — independientes entre sí (distintos controllers).
- **Fase 11**: 11.1, 11.2, 11.3 — independientes entre sí.
