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

- [ ] 6.1 Crear `adapter/outbound/persistence/entity/PurchaseEntity.java`: `@Entity @Table(name="purchases")`. Campos: `id` (UUID PK), `stripePaymentIntentId`, `downloadToken`, `email`, `productId`, `userId`, `status` (String), `amountCents`, `currency`, `createdAt`, `paidAt`, `emailSentAt`. Tipos `Instant` igual que `FlashcardEntity`. **Done when**: compilación sin error de JPA.
  - Paths: `backend/src/main/java/com/akdemya/adapter/outbound/persistence/entity/PurchaseEntity.java`
  - Deps: 1.1, 1.4

- [ ] 6.2 Crear `adapter/outbound/persistence/repository/JpaPurchaseRepository.java`: `JpaRepository<PurchaseEntity, UUID>` con queries: `findByDownloadToken`, `findByStripePaymentIntentId`, `findPendingOlderThan` (JPQL con `status='PENDING' AND createdAt < :cutoff`), `findPaidWithoutEmail` (JPQL). **Done when**: compila.
  - Paths: `backend/src/main/java/com/akdemya/adapter/outbound/persistence/repository/JpaPurchaseRepository.java`
  - Deps: 6.1

- [ ] 6.3 Crear `adapter/outbound/persistence/mapper/PurchaseMapper.java`: `toDomain(PurchaseEntity)` y `toEntity(Purchase)`. **Done when**: compila.
  - Paths: `backend/src/main/java/com/akdemya/adapter/outbound/persistence/mapper/PurchaseMapper.java`
  - Deps: 6.1, 1.4

- [ ] 6.4 Crear `adapter/outbound/persistence/PurchaseRepositoryAdapter.java`: implementa `PurchaseRepository`. `markPaid`/`markFailed` via `@Modifying @Query("UPDATE PurchaseEntity p SET p.status=... WHERE p.stripePaymentIntentId=:piId AND p.status='PENDING'")`. `updateEmailSentAt` via `@Modifying @Query`.
  - [RED] Test de integración `PurchaseRepositoryAdapterIT` con Testcontainers Postgres: `save` persiste; `markPaid` retorna 1 primera vez, 0 segunda vez (idempotencia); `findPendingOlderThan` filtra correctamente; `findPaidWithoutEmail` respeta grace cutoff.
  - [GREEN] Implementar `PurchaseRepositoryAdapter` hasta que el test IT pase.
  - **Done when**: `PurchaseRepositoryAdapterIT` verde.
  - Paths: `...persistence/PurchaseRepositoryAdapter.java`, `...test/.../PurchaseRepositoryAdapterIT.java`
  - Deps: 6.2, 6.3, 2.1
  - Test cmd: `./gradlew test --tests "*.PurchaseRepositoryAdapterIT"`

---

## Fase 7 — Application services

- [ ] 7.1 Modificar `adapter/infrastructure/stripe/StripePaymentAdapter.java`: nueva firma `createIntent(CreatePaymentIntentUseCase.Command cmd)`, obtiene `DigitalProduct` del catálogo (inyectado), pone metadata `{purchaseId, downloadToken, productId, email, userId?}`, usa `product.getAmountCents()/getCurrency()`. Eliminar hard-code de 1500 EUR. **Done when**: compila y `StripePaymentAdapterTest` (si existe) sigue verde o se actualiza.
  - Paths: `backend/src/main/java/com/akdemya/adapter/infrastructure/stripe/StripePaymentAdapter.java`
  - Deps: 3.1, 2.4

- [ ] 7.2 [RED] Test unitario `PurchaseServiceTest` (Mockito, sin Spring): escenarios:
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

- [ ] 7.3 [GREEN] Crear `application/service/PurchaseService.java`: `@Service @Transactional`, implementa `CreatePaymentIntentUseCase`, `HandleStripeWebhookUseCase`, `DownloadPurchaseUseCase`, `GetPurchaseInfoUseCase`. Orquesta ports inyectados. `handleEvent` usa `StripeEventVerifierAdapter` (inyectado) y switch sobre `event.getType()`. **Done when**: `PurchaseServiceTest` verde.
  - Paths: `backend/src/main/java/com/akdemya/application/service/PurchaseService.java`
  - Deps: 7.2, 5.4
  - Test cmd: `./gradlew test --tests "*.PurchaseServiceTest"`

- [ ] 7.4 Crear `application/service/ReconciliationService.java`: Function B — `retryFailedEmails(Instant graceCutoff)`. Usa `PurchaseRepository.findPaidWithoutEmail(graceCutoff)`, llama `TransactionalEmailPort`, si true → `updateEmailSentAt`.
  - [RED] Test `ReconciliationServiceTest`: stub `findPaidWithoutEmail` con 2 purchases; email retorna true para #1 y false para #2 → `updateEmailSentAt` llamado solo para #1; WARN log emitido para #2.
  - [GREEN] Implementar `ReconciliationService`.
  - **Done when**: test verde.
  - Paths: `...service/ReconciliationService.java`, `...test/.../ReconciliationServiceTest.java`
  - Deps: 2.1, 2.3
  - Test cmd: `./gradlew test --tests "*.ReconciliationServiceTest"`

---

## Fase 8 — Scheduler

- [ ] 8.1 Crear `adapter/infrastructure/scheduler/PurchaseReconciliationScheduler.java`: `@Component`, `@Scheduled(cron = "0 */15 * * * *")`. Function A: `purchaseRepository.findPendingOlderThan(now()-1h)` → por cada compra → `PaymentIntent.retrieve(piId)` → switch(status): `succeeded`→ markPaid + email; `canceled`/`payment_failed`→ markFailed; otros→ log WARN; excepción por compra → log ERROR + continuar. Function B: delegar a `ReconciliationService.retryFailedEmails(now()-5min)`. LOG INFO con count al inicio de cada función.
  - [RED] Test `PurchaseReconciliationSchedulerTest` (Mockito): Function A — mock repo + Stripe client: assert markPaid for succeeded, markFailed for canceled, skip for processing, updateEmailSentAt si email=true; Function B — delega a ReconciliationService mockeado; fallo Stripe en una compra → las otras se procesan.
  - [GREEN] Implementar scheduler hasta que test pase.
  - **Done when**: test verde.
  - Paths: `...scheduler/PurchaseReconciliationScheduler.java`, `...test/.../PurchaseReconciliationSchedulerTest.java`
  - Deps: 7.3, 7.4
  - Test cmd: `./gradlew test --tests "*.PurchaseReconciliationSchedulerTest"`

---

## Fase 9 — Adaptadores inbound REST

- [ ] 9.1 Eliminar `adapter/inbound/web/dto/PaymentIntentResponse.java` (si existe) y crear DTOs nuevos:
  - `CreateIntentRequest.java` (record): `@NotBlank String email`, `@NotBlank String productId`.
  - `CreateIntentResponse.java` (record): `String clientSecret`, `UUID downloadToken`.
  - `WebhookResponse.java` (record): `String status`.
  - `PurchaseInfoResponse.java` (record): `String email`, `String productName`, `String status`, `long amountCents`, `String currency`.
  - **Done when**: todas las clases compilan.
  - Paths: `backend/src/main/java/com/akdemya/adapter/inbound/web/dto/`
  - Deps: —

- [ ] 9.2 Modificar `adapter/inbound/web/PaymentController.java`:
  - `POST /api/v1/payments/create-intent`: aceptar `@Valid @RequestBody CreateIntentRequest`, llamar `CreatePaymentIntentUseCase.createIntent(Command(...))`, retornar `CreateIntentResponse`.
  - Añadir `POST /api/v1/payments/webhook`: `@RequestBody byte[] payload`, `@RequestHeader("Stripe-Signature") String sig` → `HandleStripeWebhookUseCase.handleEvent(new String(payload), sig)` → `WebhookResponse("ok")`. `SignatureVerificationException` → `GlobalExceptionHandler` debe mapear a 400.
  - **Done when**: controlador compila y test de integración (tarea 9.4) pasa.
  - Paths: `backend/src/main/java/com/akdemya/adapter/inbound/web/PaymentController.java`
  - Deps: 3.1, 3.2, 9.1, 7.3

- [ ] 9.3 Crear `adapter/inbound/web/DownloadController.java`:
  - `GET /api/v1/downloads/{token}`: `@PathVariable UUID token` → `DownloadPurchaseUseCase.openByToken(token)` → `StreamingResponseBody` con `Content-Type: application/pdf`, `Content-Disposition: attachment; filename="..."`. `NoSuchElementException` → 404.
  - `GET /api/v1/downloads/{token}/info`: → `GetPurchaseInfoUseCase.getInfo(token)` → `PurchaseInfoResponse`. UUID malformado → 400 via `MethodArgumentTypeMismatchException`.
  - **Done when**: controlador compila.
  - Paths: `backend/src/main/java/com/akdemya/adapter/inbound/web/DownloadController.java`
  - Deps: 3.3, 3.4, 9.1

- [ ] 9.4 Modificar `adapter/infrastructure/security/SecurityConfig.java`: añadir `.requestMatchers(HttpMethod.GET, "/api/v1/downloads/**").permitAll()` y verificar que `POST /api/v1/payments/webhook` también es público. **Done when**: `@SpringBootTest` arranca y los endpoints son accesibles sin auth.
  - Paths: `backend/src/main/java/com/akdemya/adapter/infrastructure/security/SecurityConfig.java`
  - Deps: 9.2, 9.3

---

## Fase 10 — Tests de integración de controllers

- [ ] 10.1 [RED+GREEN] `PaymentControllerIT` (`@SpringBootTest` + Testcontainers Postgres + MockBean StripePaymentAdapter):
  - POST create-intent body válido → 200 + `{clientSecret, downloadToken}` + Purchase en BD con status=PENDING.
  - POST create-intent sin email → 400.
  - POST create-intent productId desconocido → 404 (o 400 según GlobalExceptionHandler — verificar).
  - **Done when**: test verde.
  - Paths: `backend/src/test/java/com/akdemya/adapter/inbound/web/PaymentControllerIT.java`
  - Deps: 9.2, 6.4
  - Test cmd: `./gradlew test --tests "*.PaymentControllerIT"`

- [ ] 10.2 [RED+GREEN] `WebhookControllerIT` (`@SpringBootTest` + firma HMAC calculada + Testcontainers):
  - Firma válida + `payment_intent.succeeded` → 200, Purchase PAID (MockBean email port).
  - Segunda entrega mismo evento → 200 idempotente, email no enviado segunda vez.
  - Firma inválida → 400.
  - `payment_intent.payment_failed` → 200, Purchase FAILED.
  - **Done when**: test verde.
  - Paths: `backend/src/test/java/com/akdemya/adapter/inbound/web/WebhookControllerIT.java`
  - Deps: 9.2, 6.4, 5.4
  - Test cmd: `./gradlew test --tests "*.WebhookControllerIT"`

- [ ] 10.3 [RED+GREEN] `DownloadControllerIT` (`@SpringBootTest` + Testcontainers + seed Purchase):
  - GET `/{token}` con Purchase PAID + archivo PDF en tmpdir → 200 + `application/pdf` + `Content-Disposition`.
  - GET `/{token}` con Purchase PENDING → 404.
  - GET `/{token}` UUID desconocido → 404.
  - GET `/{token}/info` con PAID → 200 + JSON con campos esperados.
  - GET `/{token}/info` con PENDING → 200 + status=PENDING.
  - GET `/{token}/info` con FAILED → 200 + status=FAILED.
  - GET `/{token}/info` UUID desconocido → 404.
  - GET `/{token}/info` con valor no-UUID → 400.
  - **Done when**: test verde.
  - Paths: `backend/src/test/java/com/akdemya/adapter/inbound/web/DownloadControllerIT.java`
  - Deps: 9.3, 6.4, 5.3
  - Test cmd: `./gradlew test --tests "*.DownloadControllerIT"`

---

## Fase 11 — Frontend

- [ ] 11.1 Modificar `frontend/src/constants/routes.ts`: añadir `download: (token: string) => \`/descarga/${token}\``. **Done when**: compila sin error de tipo.
  - Paths: `frontend/src/constants/routes.ts`
  - Deps: —

- [ ] 11.2 Modificar `frontend/src/api/paymentApi.ts`: nueva firma `createPaymentIntent(email: string, productId: string): Promise<{clientSecret: string, downloadToken: string}>`. Eliminar firma anterior sin args. **Done when**: compila, único consumidor `PaymentModal.tsx` actualizado en 11.4.
  - Paths: `frontend/src/api/paymentApi.ts`
  - Deps: —

- [ ] 11.3 Crear `frontend/src/api/downloadApi.ts`: `downloadUrl(token: string): string` retorna URL absoluta a `GET /api/v1/downloads/${token}`; `purchaseInfoUrl(token: string): string` retorna URL a `/info`. **Done when**: compila.
  - Paths: `frontend/src/api/downloadApi.ts`
  - Deps: —

- [ ] 11.4 Crear `frontend/src/components/MailcheckHint.tsx`: props `{suggestion: string | null, onAccept: (corrected: string) => void}`. Renderiza "¿Quisiste decir @{domain}?" clickable. **Done when**: compila.
  - Paths: `frontend/src/components/MailcheckHint.tsx`
  - Deps: 0.3

- [ ] 11.5 [RED] Test `PaymentModal.test.tsx` (Vitest + Testing Library):
  - Step 1: email input visible, "Continuar" disabled hasta email válido.
  - Loading: mock `createPaymentIntent` con delay → spinner visible.
  - Step 2: mock retorna `{clientSecret, downloadToken}` → `<PaymentElement>` montado.
  - "Volver" → vuelve a Step 1 con email preservado.
  - Error path: mock 500 → mensaje de error en Step 1.
  - Mailcheck: blur con `gmial.com` → hint visible; click hint → email corregido.
  - Redirect: mock `stripe.confirmPayment` OK → `navigate` llamado con `/descarga/{token}`.
  **Done when**: tests compilan pero fallan.
  - Paths: `frontend/src/components/__tests__/PaymentModal.test.tsx`
  - Deps: 11.2, 11.4

- [ ] 11.6 [GREEN] Modificar `frontend/src/components/PaymentModal.tsx`: state machine 5 estados (`idle → email-entry → loading-intent → payment-confirming → success`). Step 1: `<input type="email">` + `MailcheckHint` + `mailcheck` on blur + "Continuar" disabled si email inválido. Step 2: `<Elements clientSecret>` + `<PaymentElement>` + "Pagar" + "Volver". Post-confirm: `navigate(ROUTES.download(downloadToken))`. **Done when**: `PaymentModal.test.tsx` verde.
  - Paths: `frontend/src/components/PaymentModal.tsx`
  - Deps: 11.5, 11.4, 11.1, 11.2
  - Test cmd: `cd frontend && npm test -- PaymentModal`

- [ ] 11.7 [RED] Test `DownloadPage.test.tsx` (Vitest + Testing Library + MSW o fetch mock):
  - Mock `/info` PAID → "Descargar PDF" visible + CTA link con email encodado.
  - Mock `/info` PENDING → mensaje "procesando" + sin botón descarga.
  - Mock `/info` FAILED → mensaje "pago fallido" + sin botón descarga.
  - Mock `/info` 404 → "Enlace no válido" + sin CTA.
  **Done when**: tests compilan pero fallan.
  - Paths: `frontend/src/pages/__tests__/DownloadPage.test.tsx`
  - Deps: 11.3, 11.1

- [ ] 11.8 [GREEN] Crear `frontend/src/pages/DownloadPage.tsx`: `useParams()` → token. Fetch `GET /api/v1/downloads/{token}/info` on mount. Switch por status: PAID → botón que llama `window.open(downloadUrl(token), '_blank')`; PENDING → mensaje; FAILED → mensaje; 404 → "Enlace no válido". CTA link `ROUTES.register + ?email=...` si email disponible. **Done when**: `DownloadPage.test.tsx` verde.
  - Paths: `frontend/src/pages/DownloadPage.tsx`
  - Deps: 11.7, 11.3
  - Test cmd: `cd frontend && npm test -- DownloadPage`

- [ ] 11.9 Modificar `frontend/src/App.tsx`: registrar `<Route path="/descarga/:token" element={<DownloadPage />} />`. **Done when**: ruta accesible en dev server.
  - Paths: `frontend/src/App.tsx`
  - Deps: 11.8, 11.1

- [ ] 11.10 Modificar `frontend/src/pages/SubalternoGVAPage.tsx`: pasar `productId="TEMARIO_SUBALTERNO_GVA"` prop al `PaymentModal`. **Done when**: compila y modal recibe el productId.
  - Paths: `frontend/src/pages/SubalternoGVAPage.tsx`
  - Deps: 11.6

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
