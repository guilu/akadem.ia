# Digital purchases — Stripe + Resend operator guide

This document describes how to deploy and operate the **digital product
purchase flow** introduced by the `descarga-temario-stripe` change
(branch `feature/AKDMIA-214-stripe-integration`).

The flow lets an anonymous visitor buy the *Temario Subalterno GVA* PDF
(15 €) through Stripe, receive a download link by email, and download
the file from `/descarga/{token}` without needing an akadem.ia account.

---

## High-level flow

```
SubalternoGVAPage  ──▶  PaymentModal (email step)
                              │
                              ▼
                  POST /api/v1/payments/create-intent
                  { email, productId }
                              │
                              ▼
              backend creates Purchase (PENDING)
              + Stripe PaymentIntent (15 €)
                              │
                              ▼
                       returns { clientSecret, downloadToken }
                              │
                              ▼
                   PaymentModal shows Stripe <PaymentElement>
                              │
                              ▼
                       buyer pays
                              │
                  ┌───────────┴───────────┐
                  ▼                       ▼
   stripe.confirmPayment OK        Stripe webhook
   (frontend redirect)             payment_intent.succeeded
   navigate(/descarga/{token})     │
                                   ▼
                       Purchase → PAID
                       Resend email sent
                       (link to /descarga/{token})
```

A scheduler (`PurchaseReconciliationScheduler`, runs every 15 min) covers
two failure modes: Stripe events never delivered, and Resend deliveries
that failed transiently.

---

## Required environment variables

`.env` (repository root, gitignored — copy it from `.env.example`):

| Variable | Where to get it | Example |
|---|---|---|
| `STRIPE_SECRET_KEY` | Stripe Dashboard → Developers → API keys → **Secret key** | `sk_test_…` (test) / `sk_live_…` (prod) |
| `STRIPE_WEBHOOK_SECRET` | Per-endpoint signing secret. Created in step *Configure the Stripe webhook* below. | `whsec_…` |
| `RESEND_API_KEY` | Resend dashboard → API Keys → Create API Key. *Send emails* permission is enough. | `re_…` |
| `RESEND_FROM_EMAIL` | A verified sender address (see *Configure Resend*). | `noreply@yourdomain.com` |
| `PRODUCTS_STORAGE_PATH` | Filesystem path holding the PDF file(s). | `/var/lib/akademia/products` (prod), `/tmp/akademia-products` (default in dev) |
| `FRONTEND_URL` | Public URL of the frontend. Used to build the email download link. | `https://akademia.example.com` (prod), `http://localhost:3000` (dev) |

`frontend/.env` (gitignored):

| Variable | Where to get it | Example |
|---|---|---|
| `VITE_STRIPE_PUBLISHABLE_KEY` | Stripe Dashboard → Developers → API keys → **Publishable key**. Public, safe to ship in the bundle. | `pk_test_…` / `pk_live_…` |
| `VITE_API_URL` *(optional)* | Backend base URL, only if the Vite dev proxy is bypassed. | `https://akademia.example.com` |

---

## Configure the Stripe webhook

The backend exposes `POST /api/v1/payments/webhook` and verifies every
delivery via HMAC-SHA256 against `STRIPE_WEBHOOK_SECRET`. Each
environment (local, staging, prod) needs its **own endpoint and own
secret** — they are not interchangeable.

### Production / staging

1. Stripe Dashboard → Developers → Webhooks → **Add endpoint**.
2. Endpoint URL: `${FRONTEND_URL}/api/v1/payments/webhook`
   (e.g. `https://akademia.example.com/api/v1/payments/webhook`).
3. Events to send:
   - `payment_intent.succeeded`
   - `payment_intent.payment_failed`
4. After saving, click **Reveal** under *Signing secret* and copy the
   `whsec_…` value into `STRIPE_WEBHOOK_SECRET`. Restart the backend.
5. Send a test event from the Dashboard. Backend logs should show
   `INFO ... payment_intent.succeeded` and a 200 response.

### Local development

The dashboard cannot reach `localhost`, so use the Stripe CLI to forward
events:

```bash
stripe listen --forward-to localhost:8080/api/v1/payments/webhook
# → "Ready! Your webhook signing secret is whsec_…"
```

Copy the printed `whsec_…` into `.env` as `STRIPE_WEBHOOK_SECRET` and
restart the backend. Trigger a purchase from the UI (test card
`4242 4242 4242 4242`, any future expiry, any CVC); the CLI should log
the events being forwarded and the Purchase row should flip to PAID.

---

## Configure Resend

1. Sign in at **resend.com** → Domains → **Add domain**, enter the
   sending domain (e.g. `yourdomain.com`).
2. Add the SPF, DKIM and (optional) MX records Resend prints to your
   DNS provider. Wait until all rows are green in the dashboard.
3. API Keys → **Create API Key** → copy the `re_…` value into
   `RESEND_API_KEY`.
4. Set `RESEND_FROM_EMAIL` to an address on the verified domain
   (e.g. `noreply@yourdomain.com`).

> While testing, you can skip steps 1–2 and use `onboarding@resend.dev`
> as the sender, but Resend will only deliver to the address registered
> on your Resend account — fine for smoke tests, not for buyers.

---

## Place the PDF

`InMemoryProductCatalog` (the seed catalog) maps the SKU
`TEMARIO_SUBALTERNO_GVA` to the storage key
`temario-subalterno-gva.pdf`. The file must live at:

```
${PRODUCTS_STORAGE_PATH}/temario-subalterno-gva.pdf
```

If the file is missing, `GET /api/v1/downloads/{token}` returns 500 and
the buyer's download page shows the generic error view.

In Docker deployments, mount the host directory into the backend
container at the same path — see `dist/docker-compose-prod.yaml`.

---

## Smoke test checklist

After deploying:

- [ ] `GET /actuator/health` (or backend root) returns 200.
- [ ] Open `${FRONTEND_URL}/temario/subalterno-gva` → "Comprar" button
      opens the modal.
- [ ] Enter `you@example.com`, click **Continuar** → Stripe
      `<PaymentElement>` mounts.
- [ ] Pay with `4242 4242 4242 4242` → modal redirects to
      `${FRONTEND_URL}/descarga/{token}`.
- [ ] The Download button streams the PDF (`application/pdf` +
      `Content-Disposition: attachment`).
- [ ] Resend email arrives at the entered address with a working
      download link.
- [ ] `purchases` table shows one row with `status = PAID`,
      `paid_at IS NOT NULL`, `email_sent_at IS NOT NULL`.
- [ ] Re-deliver the same Stripe event from the dashboard → backend
      logs the second delivery as a no-op (no second email).

---

## Troubleshooting

| Symptom | Likely cause |
|---|---|
| Webhook returns 400 `Invalid Stripe signature` | Wrong `STRIPE_WEBHOOK_SECRET` for this endpoint, or clock skew > 5 min between server and Stripe. |
| `purchases.status` stays PENDING after a successful payment | Webhook not reaching the backend (check Stripe Dashboard → Webhooks → *Events* tab). The `PurchaseReconciliationScheduler` will pick it up within 15 min if the PaymentIntent itself is `succeeded`. |
| `email_sent_at` stays NULL | Resend rejected the message. Check backend logs for `Resend delivery failed`. The scheduler retries failed deliveries every 15 min for purchases older than 5 min. |
| Download endpoint returns 404 | Token unknown, or the Purchase is not yet PAID. The same response is used for both to avoid leaking purchase existence. |
| Download endpoint returns 500 | PDF missing at `${PRODUCTS_STORAGE_PATH}/temario-subalterno-gva.pdf`. |
