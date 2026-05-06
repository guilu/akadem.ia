package com.akdemya.domain.port.out;

/**
 * Outbound port for transactional email delivery (e.g. post-purchase download links).
 *
 * <p>Implementations MUST NOT propagate provider exceptions — any delivery
 * failure (network, 4xx, 5xx) is reported via a {@code false} return value so
 * the caller can decide whether to retry. This contract lets the webhook
 * handler always return 200 OK to Stripe even when email delivery fails, while
 * the reconciliation scheduler reattempts delivery for purchases whose
 * {@code emailSentAt} is still null.</p>
 */
public interface TransactionalEmailPort {

  /**
   * Sends the download email for a completed purchase.
   *
   * @return {@code true} if the provider accepted the message (2xx response),
   *         {@code false} on any error.
   */
  boolean sendDownloadEmail(String toEmail, String downloadUrl, String productDisplayName);
}
