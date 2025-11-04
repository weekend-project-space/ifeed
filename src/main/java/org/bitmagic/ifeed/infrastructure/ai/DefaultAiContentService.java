package org.bitmagic.ifeed.infrastructure.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.bitmagic.ifeed.config.properties.AiProviderProperties;
import org.bitmagic.ifeed.exception.ApiException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultAiContentService implements AiContentService {

    private static final Pattern WORD_PATTERN = Pattern.compile("[a-zA-Z]{4,}");
    private static final int DEFAULT_SUMMARY_LENGTH = 300;
    private static final String SYSTEM_PROMPT = "You are an RSS article content analysis assistant responsible for generating JSON data containing abstracts (please summarize the main content of this article in concise language, highlighting core points and key information) summary, categories, tags. Format example: {summary:'',tags:[''],category:'string'} 中文";
    private static final String USER_PROMPT_TEMPLATE = "Title: %s\n\nContent:\n%s";

    private final AiProviderProperties properties;
    private final ChatModel chatModel;

    @Override
    public AiContent analyze(String title, String content) {
        if (!StringUtils.hasText(content)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Article content cannot be empty");
        }
        log.debug("Analyze article title='{}', contentLength={}", title, content == null ? 0 : content.length());
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
        AiContent result = ChatClient.create(chatModel)
                .prompt(SYSTEM_PROMPT)
                .user(USER_PROMPT_TEMPLATE.formatted(title, content))
                .call()
                .entity(AiContent.class);
        log.debug("AI provider returned summaryLength={} tagsCount={}",
                result.summary() == null ? 0 : result.summary().length(),
                result.tags() == null ? 0 : result.tags().size());
        return result;
    }

    private AiContent fallbackContent(String title, String content) {
        log.debug("Using fallback heuristic summary for title='{}'", title);
        return new AiContent(
                generateSummary(content),
                guessCategory(title, content),
                generateTags(content)
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
}
