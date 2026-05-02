# Proposal: Descarga del Temario Subalterno GVA tras pago Stripe

> Jira: AKDMIA-214 · Branch: `feature/AKDMIA-214-stripe-integration`

## Intent

Cerrar el ciclo de compra del MVP Stripe (1500c EUR, pago único): del `PaymentIntent` actual sin entrega, a un flujo guest-checkout con captura de email, persistencia de `Purchase`, webhook idempotente, email transaccional y descarga del PDF por token. Hoy el usuario paga y no recibe nada — esto desbloquea la primera venta real.

## Scope

### In Scope
- Agregado `Purchase` (PENDING/PAID/FAILED) con `downloadToken` UUID y `stripePaymentIntentId` UNIQUE — migración Flyway `V011__purchases.sql`.
- `POST /api/v1/payments/create-intent` cambia contrato: `{email, productId}` → `{clientSecret, downloadToken}`. Crea `Purchase(PENDING)` + PaymentIntent con metadata `{userId?, productId, email}`. Sigue público (guest checkout).
- `POST /api/v1/payments/webhook` nuevo, público, verifica firma con `Webhook.constructEvent`. Idempotente vía `UPDATE … WHERE status=PENDING`. Maneja `payment_intent.succeeded` → markPaid + email; `payment_intent.payment_failed` → markFailed. Logging SLF4J de `event.id`/`type`/`paymentIntentId`/resultado.
- `GET /api/v1/downloads/{token}` nuevo, público, devuelve `application/pdf` stream si `status=PAID`; 404 en cualquier otro caso (no enumerable).
- `ProductFileStoragePort` nuevo + `LocalProductFileStorageAdapter` + `ProductsProperties` (`products.storage-path`). NO se toca el `FileStoragePort` actual de RAG.
- `TransactionalEmailPort` + `ResendEmailAdapter` (HTTP a `api.resend.com/emails`) + `ResendProperties (apiKey, from)`. Plantilla HTML inline (cabecera + botón Descargar + texto fallback con URL `${APP_BASE_URL}/descarga/{token}`).
- `ProductCatalog` hardcoded con un único `DigitalProduct("TEMARIO_SUBALTERNO_GVA", 1500, "eur", "temario-subalterno-gva.pdf", "Temario Subalterno GVA.pdf")`.
- `PurchaseReconciliationScheduler` (`@Scheduled` cada 15 min): `PENDING && createdAt < now()-1h` → `PaymentIntent.retrieve` → reconcilia (`succeeded`→PAID+email; `canceled`/`payment_failed`→FAILED).
- Frontend: `PaymentModal` con `LinkAuthenticationElement` + `mailcheck` (anti-typo); `paymentApi.createPaymentIntent({email, productId})`; redirect post-pago a `/descarga/:token`; nueva ruta `DownloadPage` (botón descarga + CTA visual `/register?email=...`).

### Out of Scope (diferido)
- Listado "Mis compras"; asociación retroactiva de `Purchase` a usuario nuevo por email match (CTA es solo visual).
- Watermarking del PDF; `charge.refunded`/refund flow; endpoint de reenvío de email; endpoint de borrado GDPR.
- Catálogo de productos en BD; UI admin para subir PDFs; tabla `stripe_events` de auditoría.
- Migración a object storage (S3/R2); plantillas externas (Thymeleaf/etc.); soporte multi-producto/multi-moneda.

## Approach

Patrón hexagonal estricto siguiendo el módulo `flashcards` como referencia, dividido en 3 use cases dominio:

- **`CreatePaymentIntentUseCase`** (modificado): catalog → Stripe `PaymentIntent.create` con metadata → `PurchaseRepository.save(PENDING)` → devuelve `clientSecret + downloadToken` al front antes del confirm. Esto permite que el front conozca la URL de éxito sin esperar al webhook.
- **`HandleStripeWebhookUseCase`** (nuevo): verifica firma, despacha por `event.type`. La idempotencia se garantiza atómicamente por `UPDATE purchases SET status=PAID, paidAt=now WHERE stripePaymentIntentId=? AND status=PENDING` — solo se envía email si `rowsAffected==1`.
- **`DownloadPurchaseUseCase`** (nuevo): valida UUID + `status=PAID` → carga `DigitalProduct` del catálogo → `ProductFileStoragePort.open(storageKey)` → stream con `Content-Disposition: attachment`.

El `PurchaseReconciliationScheduler` es la red de seguridad: si Stripe nunca llama al webhook (mal configurado, bug, downtime), el cron resuelve el `Purchase` consultando directamente a la API de Stripe.

El PDF se coloca manualmente por el operador en `${PRODUCTS_STORAGE_PATH}` antes del go-live (ver Dependencies). Sin UI admin.

## Decisions (resoluciones del orchestrator, ya ratificadas)

| # | Decisión |
|---|----------|
| 1 | CTA "Crea cuenta" = link visual a `/register?email=...`. Asociación retroactiva → fuera de scope (v2). |
| 2 | `PurchaseReconciliationScheduler` SÍ entra en este change (intervalo 15 min). |
| 3 | `ProductFileStoragePort` nuevo y dedicado. NO modificar `FileStoragePort` (acoplado a `RagProperties`). |
| 4 | PDF colocado manualmente por el operador en `${PRODUCTS_STORAGE_PATH}`. SIN UI admin en MVP. |
| 5 | Plantilla email = HTML inline en `ResendEmailAdapter`. Sin motor de plantillas. |
| 6 | `RESEND_FROM_EMAIL` env-var (`noreply@akademia.diegobarrioh.dev` en prod; `onboarding@resend.dev` en dev). |
| 7 | Logging webhook = SLF4J estándar. Sin tabla `stripe_events`. |
| 8 | Cambio contrato `POST /create-intent` `() → {email, productId}`. Único consumidor (`PaymentModal.tsx`) se actualiza en este change. Sin breaking changes externos. |

## Affected Areas

### Backend (`backend/src/main/java/com/akdemya/...`)

| Área | Impacto | Descripción |
|------|---------|-------------|
| `domain/model/` | New | `Purchase`, `PurchaseStatus`, `DigitalProduct` |
| `domain/port/in/` | New + Modified | `HandleStripeWebhookUseCase`, `DownloadPurchaseUseCase` (new); `CreatePaymentIntentUseCase` (sig change) |
| `domain/port/out/` | New | `PurchaseRepository`, `ProductFileStoragePort`, `TransactionalEmailPort`, `ProductCatalog` |
| `application/service/` | New | `PurchaseService` (orquesta los 3 use cases), `InMemoryProductCatalog`, `PurchaseReconciliationScheduler` |
| `application/config/` | New | `ResendProperties`, `ProductsProperties` |
| `adapter/inbound/web/PaymentController.java` | Modified | Body en `create-intent`; nuevo `POST /webhook` |
| `adapter/inbound/web/DownloadController.java` | New | `GET /api/v1/downloads/{token}` |
| `adapter/outbound/persistence/` | New | `PurchaseEntity`, `PurchaseJpaRepository`, `PurchaseRepositoryAdapter` |
| `adapter/outbound/stripe/StripePaymentAdapter.java` | Modified | Acepta `(email, productId, userId?)`; metadata; precio desde catálogo |
| `adapter/outbound/storage/LocalProductFileStorageAdapter.java` | New | Implementa `ProductFileStoragePort.open(filename)` sobre `PRODUCTS_STORAGE_PATH` |
| `adapter/outbound/email/ResendEmailAdapter.java` | New | HTTP a Resend API; HTML inline |
| `resources/db/migration/V011__purchases.sql` | New | Tabla `purchases` con índices únicos |
| `resources/application.properties` | Modified | `app.products.storage-path`, `app.email.resend.{api-key,from}` |

### Frontend (`frontend/src/...`)

| Área | Impacto | Descripción |
|------|---------|-------------|
| `components/PaymentModal.tsx` | Modified | `LinkAuthenticationElement`, `mailcheck`, redirect a `/descarga/:token` |
| `components/MailcheckHint.tsx` | New | Sugerencia "did you mean gmail.com?" |
| `pages/DownloadPage.tsx` | New | Ruta `/descarga/:token`; botón Descargar + CTA visual |
| `api/paymentApi.ts` | Modified | `createPaymentIntent({email, productId}) → {clientSecret, downloadToken}` |
| `api/downloadApi.ts` | New | Helper para construir URL absoluta del endpoint de descarga |
| `pages/SubalternoGVAPage.tsx` | Modified | Pasar `productId` al modal |
| `constants/routes.ts` | Modified | `download(token)` |
| `App.tsx` | Modified | Registrar ruta |
| `package.json` | Modified | `+ mailcheck` (+ `@types/mailcheck`) |

### Tests
- Backend unit: `PurchaseServiceTest` (transiciones + idempotencia), `StripePaymentAdapterTest`, `InMemoryProductCatalogTest`, `PurchaseReconciliationSchedulerTest`.
- Backend integration: `PaymentControllerIntegrationTest` (guest), `WebhookControllerTest` (firma válida/inválida, doble entrega), `DownloadControllerTest` (PAID/PENDING/desconocido).
- Frontend: `PaymentModal.test.tsx`, `DownloadPage.test.tsx`.

## Risks

| Riesgo | Likelihood | Mitigation |
|--------|------------|------------|
| `STRIPE_WEBHOOK_SECRET` no configurado en prod | High | Tarea operativa pre-deploy; tests usan secret dummy con firma calculada |
| Idempotencia rota → emails/registros duplicados | Med | `UPDATE … WHERE status=PENDING` atómico; email solo si `rowsAffected==1` |
| `Purchase` huérfanos en `PENDING` (webhook nunca llega) | Med | `PurchaseReconciliationScheduler` cada 15 min consulta Stripe directamente |
| Email tipeado (`gmial.com`) → comprador sin acceso | Med | `mailcheck` en frontend; `/descarga/:token` es camino primario, email es backup |
| PDF en filesystem local no escala a multi-instancia | Low (deploy actual mono-instancia) | Documentado como deuda técnica para v2 (S3/R2) |
| `LinkAuthenticationElement` choca visualmente con modal | Low | Fallback: `<input type="email">` + `PaymentElement` si choca |
| Race: front falla `confirmPayment` → `Purchase(PENDING)` huérfano | Low | Cubierto por scheduler de reconciliación |
| GDPR: email queda en BD indefinidamente | Low | Documentado como deuda; no hay solicitudes activas |

## Rollback Plan

Cambio aislado en `feature/AKDMIA-214-stripe-integration`. Pasos para revertir:

1. **Pre-merge a main**: descartar la rama. Cero impacto en prod.
2. **Post-merge, sin compras reales**: revertir el merge (`git revert -m 1 <merge-sha>`); ejecutar Flyway `repair`/migración inversa manual de `V011__purchases.sql` (DROP TABLE `purchases`); restaurar contrato `POST /create-intent ()` en frontend.
3. **Post-merge, con compras reales en BD**: NO revertir migración. Desplegar parche que devuelva el `PaymentController` y `PaymentModal` al comportamiento original (sin webhook ni download); mantener tabla `purchases` para preservar historial; deshabilitar el `PurchaseReconciliationScheduler` vía property `app.scheduler.reconciliation.enabled=false`. Procesar manualmente los compradores pendientes vía Stripe Dashboard + email manual.
4. **Webhook problemático específicamente**: deshabilitar el endpoint en Stripe Dashboard (no requiere redeploy); el scheduler de reconciliación cubre los pagos hasta que se rehabilite.

Stripe en sí mismo no se desactiva (ya estaba en main antes de este change).

## Dependencies

Bloqueantes externos previos al go-live:

- **`STRIPE_WEBHOOK_SECRET`** generado en Stripe Dashboard (Developers → Webhooks → Add endpoint apuntando a `${APP_BASE_URL}/api/v1/payments/webhook`, eventos `payment_intent.succeeded` + `payment_intent.payment_failed`). Inyectado vía env-var.
- **`RESEND_API_KEY`** generada en dashboard de Resend.
- **`RESEND_FROM_EMAIL`** verificado en DNS: SPF/DKIM del dominio `akademia.diegobarrioh.dev` antes de prod. En dev se admite `onboarding@resend.dev`.
- **PDF físico** `temario-subalterno-gva.pdf` colocado manualmente por el operador en `${PRODUCTS_STORAGE_PATH}` (volumen Docker Compose montado) antes del go-live. Sin UI admin.
- **Variables de entorno nuevas**: `STRIPE_WEBHOOK_SECRET`, `RESEND_API_KEY`, `RESEND_FROM_EMAIL`, `PRODUCTS_STORAGE_PATH`, `APP_BASE_URL`. Documentar en `.env.example` y deploy scripts.
- **Dependencias de código**: `com.stripe:stripe-java:26.3.0` (ya presente), `@stripe/react-stripe-js` (ya presente), `mailcheck` npm (~3KB) + `@types/mailcheck` (a instalar).
- **Flyway**: próximo número de migración disponible es `V011`.

## Success Criteria

- [ ] Un comprador anónimo puede comprar el temario sin crear cuenta, recibe el email de descarga y puede descargar el PDF desde el enlace.
- [ ] Tras `confirmPayment` exitoso, el front redirige a `/descarga/:token` y el botón "Descargar PDF" sirve el archivo.
- [ ] El webhook de Stripe procesa `payment_intent.succeeded` exactamente una vez aunque Stripe lo reintente (verificado por test de doble entrega).
- [ ] Tests backend unit + integration verdes; tests frontend verdes; SonarCloud quality gate verde.
- [ ] Una `Purchase(PENDING)` con `createdAt < now()-1h` se reconcilia automáticamente vía scheduler en el siguiente tick.
- [ ] Email transaccional Resend llega a la bandeja del comprador en <60s en condiciones normales.
- [ ] El endpoint `GET /api/v1/downloads/{token}` devuelve 404 si el token no existe, está en PENDING o FAILED (sin filtrar entre los casos).
- [ ] La migración Flyway `V011__purchases.sql` aplica limpia en BD existente y crea `purchases` con índices únicos en `stripe_payment_intent_id` y `download_token`.
- [ ] Sin breaking changes para consumidores externos (único consumidor frontend del contrato modificado se actualiza en el mismo PR).

## Estimated Effort

**M (Medium)** — desglose por capa:

| Capa | Esfuerzo |
|------|----------|
| Backend domain + application + adapters (incluyendo Stripe webhook + Resend) | M (~1.5 días) |
| Frontend (PaymentModal + DownloadPage + mailcheck) | S (~0.5 día) |
| Migración Flyway + tests integración | S (~0.5 día) |
| Tests unit + cobertura SonarCloud | S (~0.5 día) |
| Tarea operativa (Stripe webhook config, Resend DNS, PDF deploy) | XS (~30 min, externo a code) |

Total: ~3 días de desarrollo + ~30 min de configuración operativa.
