package org.bitmagic.ifeed.infrastructure.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bitmagic.ifeed.infrastructure.ai.rerank.RerankerModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;

/**
 * @author yangrd
 * @date 2026/2/28
 **/
@Slf4j
@Component
@RequiredArgsConstructor
public class NSFWService {

    private static final Set<String> NSFW_URL_PREFIXES = Set.of(
            "javdb.com",
            "pornhub.com",
            "t66y.com",
            "91porn.com",
            "141jav.com",
            "141ppv.com",
            "xsijishe.com",
            "1fuli.info",
            "fuliba2023.net",
            "xxxclub.to",
            "t.me/sexytoday",
            "t.me/xiaojiejiepindao",
            "rsshub.rssforever.com/javdb"
    );

    private final RerankerModel rerankerModel;

    private final ChatClient chatClient;


    public boolean isNSFW(String text) {
//        -3 是分水岭
        double score = rerankerModel.documentScore("这些内容是NSFW吗？只输出0或1即可", StringUtils.truncate(text, 500), null);
        String content = chatClient.prompt(text + "/n/n 这些内容是NSFW吗？ 只输出0或1即可").call().content();
//        log.info("score: {}, text: {}", , text);
        log.info("c:{} score: {}, text: {}", content, score, text);
//        return score > -2.5;
        return content.contains("1") || score < -9.7;
    }

    public boolean isNSFW(Collection<String> urls) {
        return urls != null && urls.stream().anyMatch(url -> {
            if (StringUtils.isBlank(url)) {
                return false;
            }
            try {
                URI uri = URI.create(url.trim());
                String host = uri.getHost();
                if (host == null) {
                    return false;
                }
                host = StringUtils.removeStart(host.toLowerCase(Locale.ROOT), "www.");
                String path = StringUtils.defaultString(uri.getPath()).toLowerCase(Locale.ROOT);
                String target = host + path;
                return NSFW_URL_PREFIXES.stream()
                        .anyMatch(prefix -> target.equals(prefix) || target.startsWith(prefix + "/"));
            } catch (IllegalArgumentException ignored) {
                return false;
            }
        });
    }


}
