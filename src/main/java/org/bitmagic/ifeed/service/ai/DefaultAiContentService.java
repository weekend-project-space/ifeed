package org.bitmagic.ifeed.service.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.bitmagic.ifeed.config.AiProviderProperties;
import org.bitmagic.ifeed.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultAiContentService implements AiContentService {

    private static final Pattern WORD_PATTERN = Pattern.compile("[a-zA-Z]{4,}");
    private static final int DEFAULT_SUMMARY_LENGTH = 280;
    private static final String SYSTEM_PROMPT = "You are an assistant that analyzes RSS article content and produces JSON with summary, category, tags and embedding array. {summary:'',tags:[''],embedding:[0],category:'string'}";
    private static final String USER_PROMPT_TEMPLATE = "Title: %s\n\nContent:\n%s";

    private final AiProviderProperties properties;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    @Override
    public AiContent analyze(String title, String content) {
        if (!StringUtils.hasText(content)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Article content cannot be empty");
        }

        if (properties.isEnabled() && StringUtils.hasText(properties.getEndpoint()) && Strings.isNotBlank(content) && content.length() > DEFAULT_SUMMARY_LENGTH * 2) {
            try {
                return callExternalProvider(title, content);
            } catch (Exception ex) {
                log.warn("AI provider call failed, falling back to heuristic summary", ex);
            }
        }

        return fallbackContent(title, content);
    }

    private AiContent callExternalProvider(String title, String content) {
        var client = restClientBuilder
                .baseUrl(properties.getEndpoint())
                .defaultStatusHandler(org.springframework.http.HttpStatusCode::isError, (req, res) -> {
                    throw new ApiException(HttpStatus.valueOf(res.getStatusCode().value()), "AI provider error: " + res.getStatusCode());
                })
                .build();

        var model = Optional.ofNullable(properties.getModel()).filter(StringUtils::hasText).orElse("gpt-4o-mini");
        var messages = List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", USER_PROMPT_TEMPLATE.formatted(title, content))
        );

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", model);
        request.put("messages", messages);
        request.put("temperature", 0.2);
        request.put("max_tokens", 512);
        request.put("response_format", Map.of(
                "type", "json_schema",
                "json_schema", Map.of(
                        "name", "ArticleEnrichment",
                        "schema", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "summary", Map.of("type", "string"),
                                        "category", Map.of("type", "string"),
                                        "tags", Map.of(
                                                "type", "array",
                                                "items", Map.of("type", "string")
                                        ),
                                        "embedding", Map.of(
                                                "type", "array",
                                                "items", Map.of("type", "number")
                                        )
                                ),
                                "required", List.of("summary", "category", "tags"),
                                "additionalProperties", false
                        )
                )
        ));

        var responseBody = client.post()
                .uri("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> {
                    if (StringUtils.hasText(properties.getApiKey())) {
                        headers.setBearerAuth(properties.getApiKey());
                    }
                    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
                    headers.setAcceptCharset(List.of(StandardCharsets.UTF_8));
                })
                .body(request)
                .retrieve()
                .body(String.class);

        if (!StringUtils.hasText(responseBody)) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Empty response from AI provider");
        }

        try {
            var chatResponse = objectMapper.readValue(responseBody, ChatCompletionResponse.class);
            var messageContent = Optional.ofNullable(chatResponse)
                    .map(ChatCompletionResponse::choices)
                    .filter(list -> !list.isEmpty())
                    .map(list -> list.get(0).message())
                    .map(ChatMessage::content)
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_GATEWAY, "AI provider returned no message"));

            if (!StringUtils.hasText(messageContent)) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "AI provider returned empty message");
            }

            var jsonNode = objectMapper.readTree(messageContent);
            ProviderResponse aiResponse;
            if (jsonNode.isObject()) {
                aiResponse = objectMapper.treeToValue(jsonNode, ProviderResponse.class);
            } else {
                aiResponse = objectMapper.readValue(messageContent, ProviderResponse.class);
            }

            return new AiContent(
                    StringUtils.hasText(aiResponse.summary()) ? aiResponse.summary() : generateSummary(content),
                    StringUtils.hasText(aiResponse.category()) ? aiResponse.category() : guessCategory(title, content),
                    aiResponse.tags() != null && !aiResponse.tags().isEmpty() ? aiResponse.tags() : generateTags(content),
                    aiResponse.embedding() != null ? aiResponse.embedding() : List.of()
            );
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Failed to parse AI provider response", ex);
        }
    }

    private AiContent fallbackContent(String title, String content) {
        return new AiContent(
                generateSummary(content),
                guessCategory(title, content),
                generateTags(content),
                List.of()
        );
    }

    private String generateSummary(String content) {
        var normalized = content.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= DEFAULT_SUMMARY_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, DEFAULT_SUMMARY_LENGTH) + "...";
    }

    private String guessCategory(String title, String content) {
        var normalized = (title + " " + content).toLowerCase(Locale.ENGLISH);
        if (normalized.contains("ai") || normalized.contains("artificial intelligence")) {
            return "AI";
        }
        if (normalized.contains("startup") || normalized.contains("business")) {
            return "Business";
        }
        if (normalized.contains("security") || normalized.contains("privacy")) {
            return "Security";
        }
        if (normalized.contains("science") || normalized.contains("research")) {
            return "Science";
        }
        return "General";
    }

    private List<String> generateTags(String content) {
        var matcher = WORD_PATTERN.matcher(content.toLowerCase(Locale.CHINESE));
        var frequencies = new java.util.HashMap<String, Integer>();
        while (matcher.find()) {
            var word = matcher.group();
            frequencies.merge(word, 1, Integer::sum);
        }

        return frequencies.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ProviderResponse(String summary,
                                    String category,
                                    List<String> tags,
                                    @JsonProperty("embedding") List<Double> embedding) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChatCompletionResponse(List<Choice> choices) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Choice(ChatMessage message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChatMessage(String role, String content) {
    }
}
