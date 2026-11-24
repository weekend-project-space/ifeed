package org.bitmagic.ifeed.infrastructure.ai;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.bitmagic.ifeed.infrastructure.ai.systemone.SystemOneModel;
import org.bitmagic.ifeed.infrastructure.ai.systemone.SystemOneQuestion;
import org.bitmagic.ifeed.infrastructure.ai.systemone.SystemOneRequest;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.*;

/**
 * @author yangrd
 * @date 2026/2/28
 **/
@Component
@RequiredArgsConstructor
public class NSFWService {

    private static final String NSFW_QUESTION = "nsfw";
    private static final double NSFW_THRESHOLD = 0.5;

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

    private final SystemOneModel systemOneModel;

    public boolean isNSFW(String text) {
        if (StringUtils.isBlank(text)) return false;
        var request = new SystemOneRequest(StringUtils.truncate(text.trim(), 500), Map.of(
                NSFW_QUESTION, SystemOneQuestion.noul(
                        "内容是否包含色情、露骨性描写或成人性服务推广，属于不适合工作场合展示的 NSFW 内容？"
                                + "正常的医学、健康、教育或新闻讨论不算。")));
        return systemOneModel.call(request).noul(NSFW_QUESTION).noul() >= NSFW_THRESHOLD;
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
