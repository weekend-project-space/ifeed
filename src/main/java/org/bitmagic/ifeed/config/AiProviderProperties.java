package org.bitmagic.ifeed.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ai.provider")
public class AiProviderProperties {

    /**
     * Whether to call the external AI provider. When disabled, the system falls back to deterministic heuristics.
     */
    private boolean enabled = false;

    /**
     * Full endpoint URL of the AI service.
     */
    private String endpoint;

    /**
     * API key or bearer token used for authentication.
     */
    private String apiKey;

    /**
     * Model identifier used by the AI provider.
     */
    private String model = "gemini-pro";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
