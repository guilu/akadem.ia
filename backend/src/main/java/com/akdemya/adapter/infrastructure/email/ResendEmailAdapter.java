package com.akdemya.adapter.infrastructure.email;

import com.akdemya.application.config.ResendProperties;
import com.akdemya.domain.port.out.TransactionalEmailPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * Resend-backed {@link TransactionalEmailPort} for post-purchase download emails.
 *
 * <p>Calls {@code POST https://api.resend.com/emails} with the API key from
 * {@link ResendProperties}. Following the port contract, never propagates
 * provider exceptions: any {@link RestClientException} (network, 4xx, 5xx) is
 * caught, logged at ERROR level, and reported as {@code false} so the caller
 * can decide between webhook idempotency and scheduler retry.</p>
 */
@Component
public class ResendEmailAdapter implements TransactionalEmailPort {

  private static final Logger log = LoggerFactory.getLogger(ResendEmailAdapter.class);

  private static final String DEFAULT_BASE_URL = "https://api.resend.com";

  private final ResendProperties props;
  private final RestClient restClient;

  public ResendEmailAdapter(ResendProperties props) {
    this(props, DEFAULT_BASE_URL);
  }

  // Test-friendly constructor allowing the base URL to be overridden (e.g. MockWebServer)
  ResendEmailAdapter(ResendProperties props, String baseUrl) {
    this.props = props;
    this.restClient = RestClient.builder()
        .baseUrl(baseUrl)
        .build();
  }

  @Override
  public boolean sendDownloadEmail(String toEmail, String downloadUrl, String productDisplayName) {
    Map<String, Object> body = Map.of(
        "from", props.getFrom(),
        "to", List.of(toEmail),
        "subject", "Tu descarga: " + productDisplayName,
        "html", buildHtml(downloadUrl, productDisplayName),
        "text", buildText(downloadUrl, productDisplayName)
    );

    try {
      restClient.post()
          .uri("/emails")
          .header("Authorization", "Bearer " + props.getApiKey())
          .header("Content-Type", "application/json")
          .body(body)
          .retrieve()
          .toBodilessEntity();
      return true;
    } catch (RestClientException ex) {
      log.error("Resend delivery failed for email={}: {}", toEmail, ex.getMessage(), ex);
      return false;
    }
  }

  private String buildHtml(String url, String productDisplayName) {
    return "<!DOCTYPE html>"
        + "<html><body style=\"font-family: Arial, sans-serif; color: #1a1a1a;\">"
        + "<h2 style=\"margin-bottom: 12px;\">Tu compra está lista</h2>"
        + "<p>Gracias por adquirir <strong>" + productDisplayName + "</strong>.</p>"
        + "<p>Pulsa el botón para descargar tu PDF:</p>"
        + "<p style=\"margin: 24px 0;\">"
        + "<a href=\"" + url + "\" "
        + "style=\"display:inline-block; padding:12px 20px; background:#2563eb; "
        + "color:#ffffff; text-decoration:none; border-radius:6px; font-weight:600;\">"
        + "Descargar " + productDisplayName + "</a></p>"
        + "<p style=\"font-size:12px; color:#6b7280;\">Si el botón no funciona, copia y pega este enlace en tu navegador:<br/>"
        + "<a href=\"" + url + "\">" + url + "</a></p>"
        + "</body></html>";
  }

  private String buildText(String url, String productDisplayName) {
    return "Gracias por adquirir " + productDisplayName + ".\n\n"
        + "Descarga tu PDF en este enlace:\n"
        + url + "\n";
  }
}
