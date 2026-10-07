package org.bitmagic.ifeed.infrastructure.ai.rerank.bge;

import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.infrastructure.ai.rerank.RerankerModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
class BgeRerankerModelTest {

    @Autowired
    RerankerModel rerankerModel;

    @Test
    void doRerank() {
        long start = System.currentTimeMillis();
        rerankerModel.rerankDocumentsResult(
                """
                      用户特征：AI技术深度探索者+人文思辨爱好者。
                      主线（60%）： 优先推荐AI/LLM技术原理、prompt工程、模型应用实践。深度>广度，要有技术含金量
                      调剂（30%）： 穿插历史政治深度分析、社会学理论、哲学思辨长文。避免浅层资讯
                      探索（10%）： 科技前沿、独立开发、HackerNews热点

                      节奏控制： 连续3篇技术内容后插入1篇人文，避免审美疲劳。单篇阅读时长>8分钟优先。
                      禁忌： 避免娱乐八卦、标题党、碎片化短资讯。
                      根据上述画像，从候选池中选出最匹配的内容并排序。
                        预测每篇文章的点击率
                        """,
                List.of("年赚10亿的AI单品，喂不饱大厂","飞书钉钉企微的AI价值与局限及延伸","AI早报 | “灵光”App下载量突破100万；AMD CEO苏姿丰：不担心AI泡沫 投资不够反而比较危险","谷歌：公司必须每半年翻倍一次 AI 算力才能满足需求","谷歌和阿里，都靠AI实现了逆袭 | 财经峰评","融资3200万美元，18个月闷声干到400万ARR，这个”电话AI”凭什么？"),
                null

        ).forEach(rerankResult -> {
            log.info("{}", rerankResult);
        });
        long end = System.currentTimeMillis();
        log.info("{}ms", end - start);
    }
}