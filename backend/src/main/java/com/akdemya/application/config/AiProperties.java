package com.akdemya.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI provider configuration.
 *
 * Primary LLM:   Groq  (fast inference, llama models)
 * Fallback LLM:  OpenRouter (universal gateway, activates on any Groq error)
 * Embeddings:    OpenRouter (Groq does not offer embedding models)
 */
@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    private GroqConfig groq = new GroqConfig();
    private OpenRouterConfig openrouter = new OpenRouterConfig();

    public GroqConfig getGroq() { return groq; }
    public void setGroq(GroqConfig groq) { this.groq = groq; }

    public OpenRouterConfig getOpenrouter() { return openrouter; }
    public void setOpenrouter(OpenRouterConfig openrouter) { this.openrouter = openrouter; }

    // -------------------------------------------------------------------------

    public static class GroqConfig {
        private String apiKey = "";
        private String baseUrl = "https://api.groq.com/openai/v1";
        private String chatModel = "llama-3.3-70b-versatile";

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

        public String getChatModel() { return chatModel; }
        public void setChatModel(String chatModel) { this.chatModel = chatModel; }

        public boolean isConfigured() { return apiKey != null && !apiKey.isBlank(); }
    }

    public static class OpenRouterConfig {
        private String apiKey = "";
        private String baseUrl = "https://openrouter.ai/api/v1";
        private String chatModel = "meta-llama/llama-3.3-70b-instruct";
        private String embeddingModel = "openai/text-embedding-3-small";
        /** Optional: shown in OpenRouter dashboard rankings */
        private String siteUrl = "https://akademia.diegobarrioh.dev";
        private String appName = "Akademia";

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

        public String getChatModel() { return chatModel; }
        public void setChatModel(String chatModel) { this.chatModel = chatModel; }

        public String getEmbeddingModel() { return embeddingModel; }
        public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }

        public String getSiteUrl() { return siteUrl; }
        public void setSiteUrl(String siteUrl) { this.siteUrl = siteUrl; }

        public String getAppName() { return appName; }
        public void setAppName(String appName) { this.appName = appName; }

        public boolean isConfigured() { return apiKey != null && !apiKey.isBlank(); }
    }
}
