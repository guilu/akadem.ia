package com.akdemya.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.email.resend")
public class ResendProperties {

    private String apiKey = "";
    private String from = "onboarding@resend.dev";

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }
}
