package org.bitmagic.ifeed.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Data
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

    private ModelInfo embedding;

    private ModelInfo embeddingMini;

    private ModelInfo reranker;


    @Data
    public static class ModelInfo{
        private String baseUrl;
        private String apiKey;
        private String model;
        private Map<String,Object> options;
    }
}
