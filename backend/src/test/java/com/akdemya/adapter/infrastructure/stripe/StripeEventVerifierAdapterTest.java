package com.akdemya.adapter.infrastructure.stripe;

import com.akdemya.application.config.StripeProperties;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StripeEventVerifierAdapterTest {

  private static final String SECRET = "whsec_test_secret_for_unit_tests_0123456789";

  private static final String PAYLOAD = """
      {"id":"evt_test_123","object":"event","type":"payment_intent.succeeded",\
      "data":{"object":{"id":"pi_test_123","object":"payment_intent","status":"succeeded"}},\
      "api_version":"2024-04-10"}""";

  @Test
  void constructEvent_acceptsValidSignature() throws Exception {
    String header = buildSignatureHeader(PAYLOAD, SECRET);
    StripeEventVerifierAdapter adapter = newAdapter(SECRET);

    Event event = adapter.constructEvent(PAYLOAD, header);

    assertNotNull(event);
    assertEquals("payment_intent.succeeded", event.getType());
  }

  @Test
  void constructEvent_rejectsTamperedPayload() throws Exception {
    String header = buildSignatureHeader(PAYLOAD, SECRET);
    StripeEventVerifierAdapter adapter = newAdapter(SECRET);

    String tampered = PAYLOAD.replace("succeeded", "failed");

    assertThrows(SignatureVerificationException.class,
        () -> adapter.constructEvent(tampered, header));
  }

  @Test
  void constructEvent_rejectsInvalidSecret() throws Exception {
    String header = buildSignatureHeader(PAYLOAD, SECRET);
    StripeEventVerifierAdapter adapter = newAdapter("whsec_other_secret_xxxxxxxxxxxxxxxxxxxx");

    assertThrows(SignatureVerificationException.class,
        () -> adapter.constructEvent(PAYLOAD, header));
  }

  private static StripeEventVerifierAdapter newAdapter(String webhookSecret) {
    StripeProperties props = new StripeProperties();
    props.setSecretKey("sk_test_dummy");
    props.setWebhookSecret(webhookSecret);
    return new StripeEventVerifierAdapter(props);
  }

  private static String buildSignatureHeader(String payload, String secret) throws Exception {
    long timestamp = System.currentTimeMillis() / 1000L;
    String signedPayload = timestamp + "." + payload;
    String v1 = Webhook.Util.computeHmacSha256(secret, signedPayload);
    return "t=" + timestamp + ",v1=" + v1;
  }
}
