# Frontend Specification

## Purpose

Covers two frontend areas: the `PaymentModal` (email capture, mailcheck suggestion, post-payment redirect) and the `DownloadPage` (`/descarga/:token` route with download button and account creation CTA).

---

## Requirements

### Requirement: PaymentModal — Two-Step Email + Payment Flow

The `PaymentModal` MUST implement a two-step UX flow within a single modal:

**Step 1 — Email capture**: A standard HTML `<input type="email">` (NOT `LinkAuthenticationElement`). The "Continuar" button MUST be disabled until the email field contains a syntactically valid email address. On "Continuar", the frontend calls `POST /api/v1/payments/create-intent` with `{ email, productId }`.

**Intermediate state — Loading**: After clicking "Continuar" and while awaiting the `create-intent` response, the modal MUST display a spinner and the label "Cargando...". The user cannot interact with the form in this state.

**Step 2 — Payment**: Once `create-intent` responds with `{ clientSecret, downloadToken }`, the modal mounts `<Elements>` with the received `clientSecret` and renders `<PaymentElement>` along with a "Pagar" button. A "Volver" button is also present to return to Step 1 without losing the typed email.

**Error on create-intent**: If `create-intent` returns an error (any non-2xx), the modal returns to Step 1 and displays an error message. A "Reintentar" affordance allows the user to re-submit.

**Re-entry from Step 2**: If the user clicks "Volver" in Step 2 and then changes the email and clicks "Continuar" again, a new call to `create-intent` is made. Each call produces a **new `Purchase(PENDING)`** with a new `paymentIntentId` and a new `downloadToken`. Previous PENDING purchases from earlier `create-intent` calls become orphaned and are handled by the `PurchaseReconciliationScheduler`.

The `downloadToken` used for the post-payment redirect is always taken from the **most recent** `create-intent` response.

#### Scenario: User enters email and pays successfully

- GIVEN the PaymentModal is open at Step 1
- AND the user enters `buyer@example.com` in the email input
- WHEN the user clicks "Continuar"
- THEN `POST /api/v1/payments/create-intent` is called with `{ email: "buyer@example.com", productId: "TEMARIO_SUBALTERNO_GVA" }`
- AND a spinner "Cargando..." appears while awaiting response
- WHEN the response returns `{ clientSecret, downloadToken }`
- THEN the modal transitions to Step 2, mounts `<Elements>` with `clientSecret`, and shows `<PaymentElement>` + "Pagar" button
- WHEN the user completes the payment form and clicks "Pagar"
- THEN `stripe.confirmPayment()` is called

#### Scenario: create-intent returns error

- GIVEN the user is in the loading state after clicking "Continuar"
- WHEN `create-intent` returns a 500 error
- THEN the modal returns to Step 1 with an error message visible
- AND the user can retry without reopening the modal

#### Scenario: User returns to Step 1 to change email

- GIVEN the user is at Step 2 with email `a@example.com` and an active `clientSecret`
- WHEN the user clicks "Volver"
- THEN the modal returns to Step 1 with `a@example.com` pre-filled in the input
- WHEN the user changes the email to `b@example.com` and clicks "Continuar"
- THEN a new `create-intent` call is made with `{ email: "b@example.com", productId: "..." }`
- AND a new `downloadToken` and `clientSecret` are returned and stored
- AND the previous PENDING purchase is orphaned (to be cleaned up by the scheduler)

---

### Requirement: PaymentModal — Mailcheck Typo Suggestion

The `PaymentModal` MUST integrate the `mailcheck` npm library to detect common email domain typos (e.g., `gmial.com`, `yaho.com`). When a typo is detected, the UI MUST display a non-blocking suggestion "¿Quisiste decir @gmail.com?" (or equivalent). The suggestion MUST be actionable (clicking it corrects the email field). The suggestion MUST NOT block form submission.

#### Scenario: User types a common domain typo

- GIVEN the user is at Step 1 and types `buyer@gmial.com` in the email input and blurs the field
- WHEN `mailcheck` detects a suggestion (`gmail.com`)
- THEN the UI shows a suggestion hint "¿Quisiste decir @gmail.com?"
- AND the hint is clickable and replaces the email with `buyer@gmail.com`
- AND after accepting the suggestion the user can click "Continuar" to proceed normally

#### Scenario: User ignores the suggestion and submits

- GIVEN the user is at Step 1, has typed `buyer@gmial.com`, and a mailcheck hint is shown
- WHEN the user clicks "Continuar" without correcting the email
- THEN the form proceeds normally and `create-intent` is called with `buyer@gmial.com`
- AND no error is thrown (suggestion is advisory only)

#### Scenario: User types a valid email with no suggestion

- GIVEN the user types `buyer@gmail.com`
- WHEN the field is blurred
- THEN no suggestion hint is shown

---

### Requirement: PaymentModal — Post-Payment Redirect

After `stripe.confirmPayment()` succeeds, the frontend MUST redirect the user to `/descarga/:token` using the `downloadToken` received from `create-intent`. The redirect MUST happen client-side (React Router navigation). The PaymentModal MUST NOT close to a blank success state — the redirect is the confirmation UX.

#### Scenario: Successful payment confirmation

- GIVEN `create-intent` returned `downloadToken=abc-123`
- WHEN `stripe.confirmPayment()` resolves without error
- THEN the browser navigates to `/descarga/abc-123`
- AND the PaymentModal is no longer visible

#### Scenario: User closes browser after create-intent but before confirmPayment

- GIVEN the user received the PaymentModal and closed the browser before completing payment
- WHEN the user later opens the email received after payment
- THEN the email link `/descarga/{token}` is the recovery path (no in-app session recovery required in MVP)

---

### Requirement: DownloadPage — Download CTA

The `DownloadPage` at `/descarga/:token` MUST display a prominent "Descargar PDF" button. Clicking the button MUST trigger `GET /api/v1/downloads/{token}` which streams the PDF file. The browser MUST receive the file as a download (enforced by `Content-Disposition: attachment` from the backend).

The page MUST be accessible without authentication.

On load, the page MUST call `GET /api/v1/downloads/{token}/info` to retrieve purchase metadata. Based on the returned `status`, the page renders:
- `PAID`: "Descargar PDF" button active.
- `PENDING`: informational message "Tu compra está siendo procesada. Revisa tu email en unos minutos." Download button disabled or hidden.
- `FAILED`: error message "El pago no se pudo completar." Download button hidden.
- `404` from `/info`: generic "Enlace no válido" message.

#### Scenario: Valid token — download available

- GIVEN the user navigates to `/descarga/abc-123` where `abc-123` is a PAID purchase token
- WHEN the page loads and `GET /api/v1/downloads/abc-123/info` returns `{ status: "PAID", ... }`
- THEN a "Descargar PDF" button is visible and enabled
- WHEN the user clicks the button
- THEN the browser initiates a file download of the PDF

#### Scenario: Valid token — Purchase still PENDING

- GIVEN the user navigates to `/descarga/{token}` and `GET /{token}/info` returns `{ status: "PENDING" }`
- WHEN the page loads
- THEN the page displays "Tu compra está siendo procesada. Revisa tu email en unos minutos."
- AND no download button is shown

#### Scenario: Valid token — Purchase FAILED

- GIVEN the user navigates to `/descarga/{token}` and `GET /{token}/info` returns `{ status: "FAILED" }`
- WHEN the page loads
- THEN the page displays "El pago no se pudo completar."
- AND no download button is shown

---

### Requirement: DownloadPage — Account Creation CTA

The `DownloadPage` MUST display a secondary call-to-action "Crea una cuenta para no perder tu compra" that links to `/register?email=<purchaseEmail>`. This CTA is visual-only (no retroactive purchase association in MVP). The email pre-fill MUST use the `email` field returned by `GET /api/v1/downloads/{token}/info` (not sessionStorage). Clicking the CTA navigates to the registration page with the email pre-filled.

The CTA is rendered only when the `/info` response is available and `email` is non-empty. If `/info` returns 404, the CTA is omitted.

#### Scenario: CTA renders with prefilled email

- GIVEN the `DownloadPage` loaded `/info` and received `{ email: "buyer@example.com", status: "PAID", ... }`
- WHEN the page is rendered
- THEN the secondary CTA link points to `/register?email=buyer%40example.com`
- AND clicking the link navigates to the registration page

#### Scenario: CTA does not block the download flow

- GIVEN the CTA is visible on the page
- WHEN the user ignores the CTA and clicks "Descargar PDF"
- THEN the download proceeds normally without any account creation step
