# Exploration: descarga-temario-stripe

> Flujo completo de descarga digital tras pago Stripe del "Temario Subalterno GVA" (PDF, 15€ pago único). Cubre: webhook, persistencia de compras, email transaccional y endpoint de descarga sobre el MVP de Stripe ya en marcha.

Jira: AKDMIA-214 (rama `feature/AKDMIA-214-stripe-integration`).

---

## 1. Contexto / Estado Actual

El MVP de Stripe ya:
- Crea un `PaymentIntent` por importe fijo (1500c EUR) sin metadata.
- Devuelve `clientSecret` al frontend.
- Muestra mensaje de éxito en `PaymentModal` y cierra el modal a los 1.5s, sin redirección, sin captura de email, sin entrega del producto.

Lo que **falta** para cerrar el ciclo de compra:
1. Capturar email del comprador en el frontend (guest checkout).
2. Crear un registro `Purchase(PENDING)` antes de confirmar el pago.
3. Webhook de Stripe que marque `PAID` (idempotente) y dispare el email.
4. Email transaccional con el enlace de descarga.
5. Endpoint público `GET /api/v1/downloads/{token}` que sirva el PDF.
6. Página post-pago `/descarga/:token` con botón de descarga y CTA opcional de creación de cuenta.
7. Almacenamiento del PDF separado del storage RAG (`PRODUCTS_STORAGE_PATH`).
8. Catálogo de producto hardcoded (1 SKU MVP).

### Validación de archivos existentes

Confirmado leyendo el código de la rama:

| Archivo | Estado actual | Acción |
|---|---|---|
| `backend/.../PaymentController.java` | Sólo `POST /create-intent` sin parámetros, sin webhook | Aceptar body `{email, productId}` + añadir `POST /webhook` |
| `backend/.../StripePaymentAdapter.java` | `createIntent()` sin metadata, hard-codea 1500c EUR | Aceptar `(productId, email, userId?)`; añadir metadata; precio desde catálogo |
| `backend/.../StripeProperties.java` | Ya tiene `secretKey` + `webhookSecret` | Reutilizar |
| `backend/.../SecurityConfig.java` | `/payments/webhook` ya `permitAll`; `/payments/create-intent` ya `permitAll` (POST) | Confirmado: línea 73-74. **No requiere cambio para guest checkout** (la suposición del brief de "abrir create-intent" ya está hecha) |
| `backend/.../LocalFileStorageAdapter.java` | Implementa `FileStoragePort` (`store`, `open`) usando `RagProperties.storagePath` | Reutilizar el port; necesita o un segundo bean o un parámetro de "ruta base"; hoy está acoplado a `RagProperties` |
| `backend/.../FileStoragePort.java` | `store(filename, bytes) → Path` y `open(storagePath) → InputStream` | Suficiente para abrir PDFs estáticos colocados manualmente en `PRODUCTS_STORAGE_PATH` |
| `frontend/src/components/PaymentModal.tsx` | Sin captura de email, sin redirect | Añadir `LinkAuthenticationElement`; redirigir a `/descarga/:token` tras éxito |
| `frontend/src/api/paymentApi.ts` | `createPaymentIntent()` sin args | Aceptar `{email, productId}` |
| `frontend/src/constants/routes.ts` | No hay ruta de descarga | Añadir `download: (token) => "/descarga/${token}"` |
| `frontend/src/pages/SubalternoGVAPage.tsx` | Usa `PaymentModal` en 2 botones | Sin cambios funcionales (modal absorbe el cambio) |

**Hallazgo importante**: `LocalFileStorageAdapter` está hoy acoplado a `RagProperties` (no a un `BasePathProvider` genérico). La decisión #4 ("reutilizar `FileStoragePort` con prefijo `products/`") tiene **dos sub-opciones implementables**:

- **(A)** Añadir un campo `productsPath` a una nueva `ProductsProperties` y crear un segundo bean `LocalProductFileStorageAdapter` (o parametrizar el adapter actual con una `enum StorageZone`).
- **(B)** Dejar el adapter actual servir RAG y crear un adapter nuevo y pequeño (`LocalProductStorageAdapter`) sólo para abrir PDFs por nombre, leyendo `PRODUCTS_STORAGE_PATH`. No re-implementa nada salvo `open()`.

Preferencia: **(B)**. El port `FileStoragePort` mezcla `store` (escritura subida por usuario) con `open` (lectura). Para productos sólo necesitamos `open` y los PDFs llegan al servidor por proceso fuera de banda (deploy / scp / volumen montado). Un `ProductFileStoragePort { open(filename) }` separado es más limpio y evita un acoplamiento con `RagProperties`. Se consigna como **decisión técnica abierta para sdd-design** (no la cierro aquí porque hay dos caminos razonables).

### Stack confirmado

- Backend: Java 21 + Spring Boot 3.4.13, Gradle, `com.stripe:stripe-java:26.3.0` (verificado), patrón hexagonal por feature, Flyway con próxima migración disponible (`V011__purchases.sql`).
- Frontend: React 18 + TS + Vite + Tailwind, feature-based, `@stripe/react-stripe-js` ya integrado.
- Config: `application.properties` con env-vars; `stripe.webhook-secret=${STRIPE_WEBHOOK_SECRET:}` ya declarado.

---

## 2. Arquitectura Propuesta (visión a alto nivel — el detalle vive en `design.md`)

### Backend (hexagonal)

Nuevos componentes:

```
domain/
  model/
    Purchase.java                 ← agregado (status enum, downloadToken, monetary fields)
    PurchaseStatus.java           ← PENDING | PAID | FAILED
    DigitalProduct.java           ← record (sku, name, amountCents, currency, storageKey, displayFilename)
  port/
    in/
      CreatePaymentIntentUseCase  ← MODIFICADO: (email, productId, userId?) → {clientSecret, purchaseId}
      HandleStripeWebhookUseCase  ← NUEVO: handleEvent(rawPayload, signatureHeader)
      DownloadPurchaseUseCase     ← NUEVO: openByToken(token) → (DigitalProduct, InputStream) | throws
    out/
      PurchaseRepository          ← NUEVO
      ProductFileStoragePort      ← NUEVO (open por filename, separado del FileStoragePort RAG)
      TransactionalEmailPort      ← NUEVO (sendDownloadEmail(toEmail, downloadUrl, productName))
      ProductCatalog              ← NUEVO (in-memory map, getById)

application/
  service/
    PurchaseService.java          ← orquesta create-intent + handle-webhook + download
  config/
    ResendProperties.java         ← NUEVO (apiKey, from)
    ProductsProperties.java       ← NUEVO (storagePath)

adapter/
  inbound/web/
    PaymentController.java        ← MODIFICADO: body con email/productId; nuevo POST /webhook
    DownloadController.java       ← NUEVO: GET /api/v1/downloads/{token}
    dto/...                       ← CreatePaymentIntentRequest, DownloadResponse (stream)
  outbound/
    persistence/
      PurchaseEntity.java         ← @Entity (tabla purchases)
      PurchaseJpaRepository.java  ← Spring Data
      PurchaseRepositoryAdapter.java  ← implementa PurchaseRepository
    stripe/
      StripePaymentAdapter.java   ← MODIFICADO (metadata) — sigue implementando CreatePaymentIntentUseCase
      StripeWebhookAdapter.java   ← NUEVO o lógica en service; verifica firma con Webhook.constructEvent
    storage/
      LocalProductFileStorageAdapter.java  ← NUEVO (implementa ProductFileStoragePort, lee PRODUCTS_STORAGE_PATH)
    email/
      ResendEmailAdapter.java     ← NUEVO (HTTP a https://api.resend.com/emails)

resources/db/migration/
  V011__purchases.sql             ← create table purchases (...)
```

### Frontend

```
src/
  api/paymentApi.ts               ← MODIFICADO (acepta email, productId; devuelve clientSecret + purchaseId)
  api/downloadApi.ts              ← NUEVO (downloadByToken: builds URL con apiBase)
  components/
    PaymentModal.tsx              ← MODIFICADO: <LinkAuthenticationElement>, mailcheck, redirect a /descarga/:token
    MailcheckHint.tsx             ← NUEVO (sugerencia "did you mean gmail.com?")
  pages/
    DownloadPage.tsx              ← NUEVO: ruta /descarga/:token; botón "Descargar PDF" + CTA "Crea cuenta"
  constants/routes.ts             ← MODIFICADO (download(token))
  App.tsx                         ← MODIFICADO (registrar ruta)
  package.json                    ← + mailcheck (npm)
```

### Flujo end-to-end (resumen, secuencia detallada en design.md)

```
[User clicks "Comprar ahora"]
  ↓
[PaymentModal abre]
  ↓ user introduce email (LinkAuthenticationElement) + paga
  ↓
POST /api/v1/payments/create-intent {email, productId="TEMARIO_SUBALTERNO_GVA"}
  ↓ Backend:
    1. catalog.findById(productId) → DigitalProduct
    2. stripe.PaymentIntent.create(amount=1500, currency=eur, metadata={email, productId, userId?})
    3. purchaseRepo.save(Purchase(PENDING, paymentIntentId, downloadToken=UUID, ...))
    4. return {clientSecret, downloadToken}    ← se devuelve YA para que el front conozca la URL post-pago
  ↓
[stripe.confirmPayment()] (front)
  ↓
[Stripe procesa pago en background y llama webhook]
  ↓
POST /api/v1/payments/webhook  (firmado)
  ↓ Backend:
    1. Webhook.constructEvent(payload, sig, secret)
    2. event.type == "payment_intent.succeeded" → markPaid(paymentIntentId)
       - SELECT FOR UPDATE / unique index garantiza idempotencia
       - SI ya estaba PAID → no-op, return 200
       - SI estaba PENDING → status=PAID, paidAt=now, sendEmail(...)
    3. event.type == "payment_intent.payment_failed" → markFailed(paymentIntentId)
    4. Cualquier excepción → 500 (Stripe reintenta 3 días)
  ↓
[Email Resend con enlace https://akademia.../descarga/{downloadToken}]
  ↓
[Front, tras confirmPayment OK] redirige a /descarga/:token
  ↓
[DownloadPage muestra botón → GET /api/v1/downloads/{token} → PDF stream]
  ↓ Backend valida UUID + status=PAID + carga DigitalProduct + ProductFileStoragePort.open(...)
  ↓ Response: application/pdf, Content-Disposition: attachment; filename="..."
```

---

## 3. Alternativas consideradas

### A) Modelo de checkout (yA decidido — Guest checkout)

| Opción | Pros | Cons |
|---|---|---|
| **A. Auth obligatoria** | Usuario queda en BD, podemos asociar compras siempre | Fricción altísima para 1 PDF de 15€; bloquea conversión |
| **B. Auth opcional pre-pago** ("login o continuar") | Híbrido | Más UX a diseñar; mismo problema de fricción |
| **C. Guest checkout** ✅ | Mínima fricción; el email es el ID natural; userId nullable cubre upgrade futuro | Tenemos que enviar email transaccional sí o sí (sin él, el comprador queda sin acceso si pierde la pestaña) |

→ **Decidido: C** (en el brief).

### B) Catálogo de productos

| Opción | Pros | Cons |
|---|---|---|
| **B1. Hardcoded `ProductCatalog`** ✅ | 1 SKU; cero overhead; deploy simple | Cambiar precio o añadir producto = redeploy |
| **B2. Tabla `products`** | Admin puede tocar precio sin redeploy | Sobreingeniería para 1 producto; UI admin pendiente |
| **B3. Stripe Products + Prices API** | Stripe es la fuente de verdad; admin via dashboard | Acoplamiento adicional; latencia extra; complica tests |

→ **Decidido: B1** para MVP. Migrar a B2 cuando haya N>1 productos.

### C) Almacenamiento de PDF

| Opción | Pros | Cons |
|---|---|---|
| **C1. Filesystem local** (`PRODUCTS_STORAGE_PATH`) ✅ | Cero infra extra; usa volumen Docker | Escala mal con N instancias; sin CDN |
| **C2. S3 / Cloudflare R2** | Escalable; firmable URL directa | Necesita credenciales + bucket; sobreingeniería para 1 producto MVP |
| **C3. Embebido en el JAR (resources)** | Sin filesystem extra | El JAR crece a 50MB+; ridículo para PDFs |

→ **Decidido: C1**. Riesgo asumido: si pasamos a multi-instancia (k8s) habrá que migrar a C2.

### D) Provider de email transaccional

| Opción | Pros | Cons |
|---|---|---|
| **D1. Resend** ✅ | API JSON simple; dominio verificable; precio razonable; modo "sandbox" con dominio onboarding | Provider externo más; nuevo secret a gestionar |
| **D2. SMTP genérico (Gmail / Mailgun)** | Universal; funciona en cualquier proveedor | Más boilerplate (JavaMail); reputación de IP frágil |
| **D3. AWS SES** | Barato; bien integrado si ya hay AWS | No usamos AWS aún; añade dependencia |

→ **Decidido: D1**.

### E) Idempotencia del webhook

| Opción | Pros | Cons |
|---|---|---|
| **E1. Unique index en `stripe_payment_intent_id` + `UPDATE … WHERE status=PENDING`** ✅ | Simple, atómico a nivel BD | Race condition en lectura previa (mitigada por `UPDATE` condicional) |
| **E2. Tabla `webhook_events` con `stripe_event_id` + index único** | Auditoría de eventos | Doble insert; complejidad |
| **E3. Distributed lock (Redis)** | Robusto en multi-instancia | Hay que meter Redis |

→ **Decidido: E1** (suficiente para 1 instancia + Stripe garantiza `event.id` único pero el patrón "UPDATE WHERE status=PENDING" es la red de seguridad real). Si hace falta auditoría futura, añadir E2 sin reescribir.

### F) Mecanismo de reconciliación para `Purchase` quedando en `PENDING`

| Opción | Pros | Cons |
|---|---|---|
| **F1. Cron diario que reconsulta a Stripe los `PENDING > 1h`** | Cubre fallos de webhook; reportable | Necesita scheduler; lógica adicional |
| **F2. Cron + endpoint manual `/admin/reconcile-purchases`** | Pánico-friendly | Más superficie API |
| **F3. No hacer nada (asumir webhook fiable)** | Cero código | Riesgo real: usuario paga, no recibe nada; soporte manual |

→ **Recomendación**: incluir F1 ya en MVP (Spring `@Scheduled` consultando `PaymentIntent.retrieve(id)` para los pendientes con > 1h). Si el orchestrator quiere apretar scope, dejarlo como tarea de v1.1 — pero entonces hay que documentar el procedimiento manual de reconciliación.

→ **Open question** (ver §6): ¿Cron de reconciliación dentro de este change, o lo movemos a v1.1?

---

## 4. Áreas afectadas

### Backend (modificar)
- `backend/src/main/java/com/akdemya/adapter/inbound/web/PaymentController.java` — add `@RequestBody`, add webhook endpoint
- `backend/src/main/java/com/akdemya/adapter/infrastructure/stripe/StripePaymentAdapter.java` — accept email/productId/userId, set metadata
- `backend/src/main/java/com/akdemya/domain/port/in/CreatePaymentIntentUseCase.java` — change signature
- `backend/src/main/java/com/akdemya/adapter/infrastructure/security/SecurityConfig.java` — confirmar que `/api/v1/downloads/**` queda público (no parece necesario cambiar más; los dos endpoints de payments ya están whitelisted)

### Backend (crear)
- `domain/model/Purchase.java`, `PurchaseStatus.java`, `DigitalProduct.java`
- `domain/port/in/HandleStripeWebhookUseCase.java`, `DownloadPurchaseUseCase.java`
- `domain/port/out/PurchaseRepository.java`, `ProductFileStoragePort.java`, `TransactionalEmailPort.java`, `ProductCatalog.java`
- `application/service/PurchaseService.java` (puede unificar las 3 use cases o dividir en 3 services pequeños)
- `application/config/ResendProperties.java`, `ProductsProperties.java`
- `adapter/inbound/web/DownloadController.java` + DTO requests
- `adapter/outbound/persistence/PurchaseEntity.java`, `PurchaseJpaRepository.java`, `PurchaseRepositoryAdapter.java`
- `adapter/outbound/stripe/StripeWebhookSignatureVerifier.java` (o inline en service)
- `adapter/outbound/storage/LocalProductFileStorageAdapter.java`
- `adapter/outbound/email/ResendEmailAdapter.java`
- `application/service/InMemoryProductCatalog.java` (`@Component`)
- `resources/db/migration/V011__purchases.sql`
- `resources/application.properties` — añadir `app.products.storage-path=${PRODUCTS_STORAGE_PATH:/tmp/akademia-products}`, `app.email.resend.api-key=${RESEND_API_KEY:}`, `app.email.resend.from=${RESEND_FROM:noreply@akademia.diegobarrioh.dev}`

### Frontend (modificar)
- `frontend/src/components/PaymentModal.tsx` — add LinkAuthenticationElement, mailcheck integration, redirect to `/descarga/:token`
- `frontend/src/api/paymentApi.ts` — accept `{email, productId}` body; return `{clientSecret, downloadToken}`
- `frontend/src/pages/SubalternoGVAPage.tsx` — pasar `productId` al modal
- `frontend/src/constants/routes.ts` — añadir `download: (t) => /descarga/${t}`
- `frontend/src/App.tsx` — registrar ruta
- `frontend/package.json` — add `mailcheck` (verificar tipados; suele requerir `@types/mailcheck`)

### Frontend (crear)
- `frontend/src/pages/DownloadPage.tsx`
- `frontend/src/api/downloadApi.ts` (helper para construir URL absoluta del endpoint protegido por token)

### Tests
- Backend unitarios: `PurchaseServiceTest` (transiciones de status, idempotencia), `StripePaymentAdapterTest` (metadata), `InMemoryProductCatalogTest`
- Backend integración: `PaymentControllerIntegrationTest` (POST create-intent guest), `WebhookControllerTest` (firma válida/inválida, doble entrega), `DownloadControllerTest` (token PAID / PENDING / desconocido)
- Frontend: `PaymentModal.test.tsx` (flujo email → submit → redirect), `DownloadPage.test.tsx`

---

## 5. Decisiones técnicas (resumen consolidado)

> Todas pre-acordadas en el brief y validadas contra el código real. Listadas aquí para que `sdd-propose` y `sdd-design` partan con base firme.

1. **Guest checkout** → `POST /api/v1/payments/create-intent` público (ya lo es), body `{email, productId}`. `userId` se rellena si el JWT está presente (read-only sniff por filtro existente), no obligatorio.
2. **Agregado `Purchase`** con campos especificados en el brief; `status` enum, `downloadToken UUID UNIQUE`, `stripePaymentIntentId UNIQUE`. PK = UUID v4 generada en dominio.
3. **Crear `Purchase(PENDING)` en `create-intent`** — atómico con la creación del PaymentIntent. Devolver `downloadToken` al front YA, no al webhook.
4. **`PRODUCTS_STORAGE_PATH`** separado de `RAG_STORAGE_PATH` vía `ProductsProperties`. Crear `ProductFileStoragePort` (interface dominio) y `LocalProductFileStorageAdapter` (implementa solo `open`); el `FileStoragePort` actual sigue sirviendo a RAG sin cambios.
5. **`GET /api/v1/downloads/{token}`** público; valida UUID + `status==PAID`; `Content-Type: application/pdf`, `Content-Disposition: attachment; filename="<displayFilename>"`. 404 si token desconocido o `PENDING`/`FAILED` (no filtrar diferente para no enumerar).
6. **Resend** vía nuevo `TransactionalEmailPort` + `ResendEmailAdapter` (HTTP) + `ResendProperties (apiKey, from)`. Plantilla mínima inline con producto y enlace de descarga.
7. **Frontend**: `<LinkAuthenticationElement>` de `@stripe/react-stripe-js` para email; `mailcheck` (npm) para sugerencia "did you mean gmail.com?".
8. **Post-pago** redirige a `/descarga/:token` con botón "Descargar PDF" + CTA "Crea cuenta para no perder tu compra" (ver §6 sobre alcance del CTA).
9. **Webhook** `POST /api/v1/payments/webhook`; verifica firma con `Webhook.constructEvent(payload, sig, secret)` (stripe-java SDK); maneja `payment_intent.succeeded` y `payment_intent.payment_failed`; idempotente por `UPDATE … WHERE status=PENDING`.
10. **Catálogo hardcoded** `InMemoryProductCatalog` con un único `DigitalProduct("TEMARIO_SUBALTERNO_GVA", "Temario Subalterno GVA", 1500, "eur", "temario-subalterno-gva.pdf", "Temario Subalterno GVA.pdf")`. NO entidad BD.
11. **`STRIPE_WEBHOOK_SECRET`** se inyecta vía env-var; en tests se mockea o se usa secret fijo + payload con firma calculada manualmente.

---

## 6. Open Questions (para el orchestrator)

1. **CTA "Crea cuenta para no perder tu compra"** en `/descarga/:token`: ¿entra en MVP como flujo funcional (asocia `Purchase.userId` al recién creado por email match), o queda como botón visual que lleva al `/register` normal sin asociación retroactiva? El brief dice "me inclino por dejar el CTA para v2 si complica". → Recomendación: **dejarlo como link visual a `/register?email=<prefilled>` en este change; el endpoint que asocia compras retroactivas se difiere a un change separado**. Confirmar.
2. **Cron de reconciliación de `PENDING > 1h`**: ¿incluir en este change (Spring `@Scheduled` cada 6h consultando Stripe) o diferir a v1.1? → Recomendación: incluir un `PurchaseReconciliationScheduler` mínimo en este change para no quedar a ciegas si Stripe rechaza el webhook por configuración. El precio es ~30 líneas y un test.
3. **`ProductFileStoragePort` separado vs reutilizar `FileStoragePort`**: el brief dice "reutilizar `FileStoragePort` con prefijo `products/`" pero el adapter actual está acoplado a `RagProperties`. ¿Aceptas la decisión de crear un port nuevo y dedicado (`ProductFileStoragePort.open(filename)`) en lugar de generalizar el actual? → Recomendación: port nuevo (más limpio, sin tocar RAG).
4. **Entrega del PDF**: ¿se asume que el operador (Diego) coloca manualmente el archivo `temario-subalterno-gva.pdf` en `${PRODUCTS_STORAGE_PATH}` antes del go-live? No hay UI ni endpoint admin para subirlo en MVP. Confirmar — si sí, documentar en el README de despliegue.
5. **Plantilla del email**: ¿texto plano + HTML mínimo inline en el adapter, o introducir un sistema de templates (Thymeleaf / freemarker / Mustache)? → Recomendación MVP: HTML inline en el adapter (1 producto, 1 plantilla), refactor cuando haya N>1.
6. **Dominio "from" del email Resend**: `noreply@akademia.diegobarrioh.dev` necesita verificación DNS en Resend. ¿Está hecho ya? Si no, hay un paso operativo previo bloqueante para envío real (mockeable en tests).
7. **Logging y trazabilidad**: ¿queremos un log estructurado de todos los eventos del webhook (event.id, event.type, processed_at) en una tabla `stripe_events`, o basta con logs estándar? → Recomendación MVP: logs estándar + métrica de "webhook ok/fail"; tabla de eventos a futuro.
8. **`POST /api/v1/payments/create-intent`** hoy NO acepta body. ¿Confirmas que el cambio del contrato (de `()` a `{email, productId}`) es compatible con cualquier consumidor existente? → Verificado: el único consumidor es `PaymentModal.tsx`, así que sí, no rompe nada externo.

---

## 7. Riesgos

> Listados con probabilidad (P) e impacto (I) en escala L/M/H.

- **(P:H, I:M)** `STRIPE_WEBHOOK_SECRET` aún no obtenido. Bloquea E2E real en staging/prod. Mockeable en tests con secret fijo. **Mitigación**: configurar en Stripe Dashboard antes del despliegue prod; tests usan firma calculada con secret dummy.
- **(P:M, I:H)** Idempotencia del webhook crítica. Stripe reintenta hasta 3 días; un fallo en idempotencia → emails duplicados, doble registro `PAID`. **Mitigación**: `UPDATE … SET status=PAID WHERE id=? AND status=PENDING` + sólo enviar email si `update count == 1`.
- **(P:M, I:H)** `Purchase` quedando `PENDING` para siempre si el webhook nunca llega. Usuario paga, no recibe nada. **Mitigación**: cron de reconciliación (open question #2) + email de soporte visible.
- **(P:M, I:M)** Emails ficticios o tipeados (gmial.com). El comprador no recibe el enlace. **Mitigación 1**: `mailcheck` en frontend sugiere corrección antes de pago. **Mitigación 2**: la página `/descarga/:token` es el camino primario; el email es backup. El UUID del token en el `localStorage` o cookie de 7 días sería extra defensa (decidir en design).
- **(P:M, I:M)** Catálogo hardcoded acopla cambios de producto/precio al deploy. Aceptable para 1 producto; dolor cuando haya 3+. **Mitigación**: ya documentado en alternativas; esperar a N>1.
- **(P:L, I:M)** `LinkAuthenticationElement` puede no encajar visualmente con el modal actual (que tiene su propio padding/spacing). **Mitigación**: prototipar antes; fallback a un `<input type="email">` propio + Stripe `PaymentElement` si choca. No re-debatir hasta tener evidencia visual.
- **(P:H, I:L)** PDF en filesystem local del backend. Si en el futuro despliegan múltiples instancias, cada una necesita el archivo. **Mitigación**: aceptable para deploy actual mono-instancia (Docker Compose con volumen). Documentar como deuda técnica para v2 (S3/R2).
- **(P:L, I:H)** Race condition: el front recibe `clientSecret` pero el `confirmPayment` falla; el `Purchase(PENDING)` queda huérfano. **Mitigación**: idéntica a #3 (cron de reconciliación), o eliminar `Purchase` si Stripe nos avisa de `payment_intent.canceled` (no incluido en este change — añadir si trivial).
- **(P:L, I:M)** Endpoint de descarga sin rate limit → abuso de fuerza bruta sobre tokens UUID. **Mitigación**: UUID v4 tiene 122 bits de entropía; brute force inviable. Si el `RateLimitFilter` actual es global por IP, ya hay alguna protección; si no, añadir un límite específico es trivial pero no MVP.
- **(P:L, I:M)** GDPR / borrado de datos: `email` queda en BD para siempre. Aceptable mientras no haya solicitud de borrado; documentar como deuda. No incluir en este change.

---

## 8. Out of Scope (diferido explícitamente)

- Listado "Mis compras" en el área de usuario.
- Watermarking del PDF con email del comprador.
- Manejo de `charge.refunded` (refund flow).
- Endpoint para reenviar el email de descarga.
- Catálogo de productos en BD + UI admin.
- Asociación retroactiva de compras a un usuario nuevo por email match (CTA queda visual; ver Open Question #1).
- Migración a object storage (S3/R2).
- Plantillas de email externas (templating engine).
- Auditoría de eventos webhook en tabla dedicada.
- Soporte para múltiples productos / múltiples monedas.

---

## 9. Dependencias técnicas

- `com.stripe:stripe-java:26.3.0` ya presente — sí trae `Webhook.constructEvent` y modelos de `PaymentIntent`. ✅
- Spring Boot HTTP client (`RestClient` / `WebClient`) para llamar a Resend REST API. Ya disponible.
- Flyway ya configurado; añadir `V011__purchases.sql` siguiendo el patrón existente.
- `mailcheck` npm (~3KB) + tipos. Permitido por bundle budget.
- `@stripe/react-stripe-js` ya presente — `LinkAuthenticationElement` exportado por la librería sin instalación adicional.
- Variable de entorno nueva: `RESEND_API_KEY`, `RESEND_FROM`, `PRODUCTS_STORAGE_PATH`. Documentar en `.env.example` y deploy scripts.
- Endpoint webhook necesita registrarse en Stripe Dashboard (test + live mode) → tarea operativa, no de código.

---

## 10. Ready for Proposal

**Sí, con caveats**: las decisiones arquitectónicas están cerradas excepto las 3 primeras Open Questions, que requieren confirmación corta del orchestrator/usuario antes de redactar `proposal.md`:
1. ¿CTA "Crea cuenta" como link visual o flujo funcional?
2. ¿Cron de reconciliación dentro o fuera de este change?
3. ¿`ProductFileStoragePort` nuevo (recomendado) o generalizar `FileStoragePort`?

Las 5 restantes (operativas: dominio Resend verificado, PDF colocado manualmente en deploy, plantilla email inline, logging estándar, contrato create-intent) son aclaratorias y no bloquean la propuesta.

**Next**: `sdd-propose` con las respuestas a las 3 preguntas integradas, o `sdd-propose` directamente con las recomendaciones tomadas como default si el usuario las ratifica en bloque.
