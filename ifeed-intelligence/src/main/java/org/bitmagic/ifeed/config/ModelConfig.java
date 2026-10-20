package org.bitmagic.ifeed.config;

import org.bitmagic.ifeed.config.properties.AiProviderProperties;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * @author yangrd
 * @date 2026/10/13
 **/
@Configuration
public class ModelConfig {

    @Bean
    @Primary
    public EmbeddingModel embeddingModel(@Qualifier("openAiEmbeddingModel") EmbeddingModel embeddingModel) {
        return embeddingModel;
    }

    @Bean
    public EmbeddingModel embeddingModelMini(AiProviderProperties properties) {
        AiProviderProperties.ModelInfo embeddingMini = properties.getEmbeddingMini();
        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder().model(embeddingMini.getModel()).dimensions((int) embeddingMini.getOptions().getOrDefault("dimensions", 384)).build();
        return new OpenAiEmbeddingModel(new OpenAiApi(embeddingMini.getBaseUrl(), embeddingMini.getApiKey()), MetadataMode.NONE, options);
    }

}
