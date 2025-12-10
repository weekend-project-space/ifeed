package org.bitmagic.ifeed.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.ai.provider")
public class AiProviderProperties {

    /**
     * Whether to call the external AI provider. When disabled, the system falls back to deterministic heuristics.
     */
    private boolean enabled = false;

    /**
     * Full endpoint URL of the AI service.
     */
    private String baseUrl;

    /**
     * API key or bearer token used for authentication.
     */
    private String apiKey;
    /**
     * Model identifier used by the AI provider.
     */
    private String model = "gpt-4o-mini";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
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
