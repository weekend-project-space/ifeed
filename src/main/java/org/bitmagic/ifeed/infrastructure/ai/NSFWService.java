package org.bitmagic.ifeed.infrastructure.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bitmagic.ifeed.infrastructure.ai.rerank.RerankerModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * @author yangrd
 * @date 2026/2/28
 **/
@Slf4j
@Component
@RequiredArgsConstructor
public class NSFWService {

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


}
