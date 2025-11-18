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
                "Parallels Desktop 26.2.0-57329 是一款强大的Mac虚拟机解决方案，允许用户同时运行Windows和macOS应用程序，实现文件拖放、手势共享和云端共享。它提供无缝对接、安全便捷的新建向导，并支持7.1环绕立体声和加速3D图形，能够流畅运行高要求的Windows程序。该版本支持Windows 10和OS X El Capitan，简化了Windows打印流程。更新日志显示性能大幅提升，启动和关机速度提升50%，任务处理速度提升20%。同时改进了文件关联，并引入了OS X El Capitan的新功能，如拆分视图、Share On服务、OS X手势在Windows中的应用，以及实时定位功能。安装方面，Mojave系统下可能需要参考特定方法进行安装。",
                "Gemini 3 还没影子，GPT 5.1 已经在路上。7 号深夜，OpenRouter 平台上线了一个全新的隐名模型。已经有眼尖动作快的网友尝鲜体验，并且认为这就是披着马甲的 GPT 5.1，暂名：Polaris Alpha。\n" +
                        "\n" +
                        "\n" +
                        "\n" +
                        "目前提供 API 调用（包括 OpenRouter），知识库截止时间为 2024 年 10 月，不支持推理模式。最大 context 容量 256K，单次最大输出 128K。\n" +
                        "\n" +
                        "开发商信息保密，但是在网友的不懈努力下，成功「越狱」，让 Polaris Alpha 自曝了家门。\n",
                "我们来试一试吧",
                "我们一起学猫叫",
                "我和Faker五五开",
                "明天预计下雨，不能出去玩了"
        );
        rerankerModel.rerankDocumentsResult("ai 相关文章", docs, RerankOptions.create()
                .withTopN(3)
                .withMinScore(-5.0)).forEach(System.out::println);
        log.info("{} ms", System.currentTimeMillis() - start);
    }
}