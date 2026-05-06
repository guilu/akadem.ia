package com.akdemya.adapter.infrastructure.email;

import com.akdemya.application.config.ResendProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResendEmailAdapterTest {

  private MockWebServer server;
  private ResendEmailAdapter adapter;

  @BeforeEach
  void setUp() throws IOException {
    server = new MockWebServer();
    server.start();

    ResendProperties props = new ResendProperties();
    props.setApiKey("re_test_api_key");
    props.setFrom("noreply@akademia.test");

    String baseUrl = server.url("/").toString();
    // Strip trailing slash so RestClient builds /emails correctly
    String trimmed = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    adapter = new ResendEmailAdapter(props, trimmed);
  }

  @AfterEach
  void tearDown() throws IOException {
    server.shutdown();
  }

  @Test
  void sendDownloadEmail_returnsTrueAndPostsExpectedPayload() throws Exception {
    server.enqueue(new MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody("{\"id\":\"resend-msg-123\"}"));

    boolean result = adapter.sendDownloadEmail(
        "buyer@example.com",
        "https://akademia.test/descarga/abc-token",
        "Temario Subalterno GVA");

    assertTrue(result);

    RecordedRequest recorded = server.takeRequest();
    assertEquals("POST", recorded.getMethod());
    assertEquals("/emails", recorded.getPath());
    assertEquals("Bearer re_test_api_key", recorded.getHeader("Authorization"));
    assertNotNull(recorded.getHeader("Content-Type"));
    assertTrue(recorded.getHeader("Content-Type").startsWith("application/json"));

    String body = recorded.getBody().readUtf8();
    JsonNode json = new ObjectMapper().readTree(body);
    assertEquals("noreply@akademia.test", json.get("from").asText());
    assertEquals("buyer@example.com", json.get("to").get(0).asText());
    assertTrue(json.get("subject").asText().contains("Temario Subalterno GVA"));
    assertTrue(json.get("html").asText().contains("https://akademia.test/descarga/abc-token"));
    assertTrue(json.get("html").asText().contains("Temario Subalterno GVA"));
    assertTrue(json.get("text").asText().contains("https://akademia.test/descarga/abc-token"));
  }

  @Test
  void sendDownloadEmail_returnsFalseOn500() {
    server.enqueue(new MockResponse()
        .setResponseCode(500)
        .setBody("{\"error\":\"upstream\"}"));

    boolean result = adapter.sendDownloadEmail(
        "buyer@example.com",
        "https://akademia.test/descarga/abc-token",
        "Temario Subalterno GVA");

    assertFalse(result);
  }
}
