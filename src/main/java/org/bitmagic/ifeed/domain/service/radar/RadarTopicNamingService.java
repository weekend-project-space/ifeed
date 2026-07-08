package org.bitmagic.ifeed.domain.service.radar;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RadarTopicNamingService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ChatClient chatClient;

    public TopicNameResult nameTopic(List<String> titles) {
        String joined = titles.stream().filter(StringUtils::hasText).limit(10).collect(Collectors.joining("\n"));
        String prompt = "你是信息流编辑。根据以下文章标题，为它们归纳一个主题。\n" +
                "要求：\n" +
                "- 输出 JSON，字段为 title, description, keywords\n" +
                "- title: 8~14 个中文字符（尽量像栏目名）\n" +
                "- description: 20~50 个中文字符，一句话说明为什么值得关注\n" +
                "- keywords: 5~8 个关键词数组\n" +
                "只输出 JSON，不要额外解释。\n\n" +
                "标题列表：\n" + joined;

        try {
            String content = chatClient.prompt(prompt).call().content();
            if (!StringUtils.hasText(content)) {
                return fallback(titles);
            }
            JsonNode node = OBJECT_MAPPER.readTree(content);
            String title = node.path("title").asText(null);
            String description = node.path("description").asText(null);
            List<String> keywords = OBJECT_MAPPER.convertValue(node.path("keywords"), OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, String.class));
            if (!StringUtils.hasText(title)) {
                return fallback(titles);
            }
            return new TopicNameResult(title, description, keywords);
        } catch (Exception e) {
            log.warn("Failed to name radar topic via AI, fallback", e);
            return fallback(titles);
        }
    }

    private TopicNameResult fallback(List<String> titles) {
        String title = titles.stream().filter(StringUtils::hasText).findFirst().orElse("今日话题");
        title = title.length() > 14 ? title.substring(0, 14) : title;
        return new TopicNameResult(title, null, List.of());
    }

    public record TopicNameResult(String title, String description, List<String> keywords) {
    }
}
