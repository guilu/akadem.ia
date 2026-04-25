package com.akdemya.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "stripe")
@Component
public record StripeProperties(String secretKey, String webhookSecret) {}
