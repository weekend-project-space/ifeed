package org.bitmagic.ifeed.infrastructure.ai.rerank;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@Slf4j
@SpringBootTest
class RerankerModelTest {

    @Autowired
    RerankerModel rerankerModel;

    @Test
    void call(){
        long start = System.currentTimeMillis();
        List<String> docs = List.of(
                "在我上一篇文章《我妈妈被电信诈骗95万元的全过程》发布后，很多网友留言对于如此大金额的银行转账为什么没有触发银行的风险控制感到疑惑，我一开始也百思不得其解，我于是在另一台手机上安装并登录了中国银行的手机银行APP，通过对手机银行日志的分析，我才终于明白——骗子并不是“暴力盗钱”，而是在几天的时间里，精确地绕过了银行的风险控制机制。 一、时间线回顾 7月底，骗子冒充警察打我妈妈的电话，声称她的身份证被人冒用，涉嫌一宗300万元的诈骗大案，要求她配合“资金核查”，并套取了支付宝密码、银行卡号和密码。 7月30日，骗子以“配合公安调查、进行视频签到”为由，让我妈妈购买了一部新手机——华为畅享80S。..."
        );
        rerankerModel.rerankDocumentsResult("给这篇文章的信息密度打分", docs, RerankOptions.create()
                .withTopN(3)
                .withMinScore(-5.0)).forEach(System.out::println);
        log.info("{} ms", System.currentTimeMillis() - start);
    }
}