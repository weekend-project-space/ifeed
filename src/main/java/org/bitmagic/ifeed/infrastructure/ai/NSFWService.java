package org.bitmagic.ifeed.infrastructure.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bitmagic.ifeed.infrastructure.ai.rerank.RerankerModel;
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


    public boolean isNSFW(String text) {
//        -3 是分水岭
        double score = rerankerModel.documentScore("这些内容适合给未成年人观看吗？", StringUtils.truncate(text, 500), null);
        log.info("score: {}, text: {}", score, text);
        return score > -3.5;
    }


}
