-- 1. Create purchases table — tracks Stripe payments for digital products
CREATE TABLE purchases (
    id                        UUID         PRIMARY KEY,
    stripe_payment_intent_id  VARCHAR(255) NOT NULL,
    download_token            UUID         NOT NULL,
    email                     VARCHAR(320) NOT NULL,
    product_id                VARCHAR(100) NOT NULL,
    user_id                   UUID,
    status                    VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    amount_cents              BIGINT       NOT NULL,
    currency                  VARCHAR(10)  NOT NULL,
    created_at                TIMESTAMPTZ  NOT NULL DEFAULT now(),
    paid_at                   TIMESTAMPTZ,
    email_sent_at             TIMESTAMPTZ,
    CONSTRAINT chk_purchases_status CHECK (status IN ('PENDING', 'PAID', 'FAILED'))
);

-- 2. Unique index on Stripe PaymentIntent id — enforces idempotency at DB level
--    Used by webhook handler (findByStripePaymentIntentId, markPaid, markFailed).
CREATE UNIQUE INDEX uq_purchases_stripe_pi_id
    ON purchases (stripe_payment_intent_id);

-- 3. Unique index on download_token — used by GET /api/v1/downloads/{token} and /info
CREATE UNIQUE INDEX uq_purchases_download_token
    ON purchases (download_token);

-- 4. Partial index for the reconciliation scheduler Function A
--    (findPendingOlderThan). Narrow index — ignored once row transitions to PAID/FAILED.
CREATE INDEX idx_purchases_status_created_at
    ON purchases (status, created_at)
    WHERE status = 'PENDING';

-- 5. Partial index for the reconciliation scheduler Function B
--    (findPaidWithoutEmail). Only keeps PAID rows with email_sent_at IS NULL.
CREATE INDEX idx_purchases_pending_email
    ON purchases (paid_at)
    WHERE status = 'PAID' AND email_sent_at IS NULL;
