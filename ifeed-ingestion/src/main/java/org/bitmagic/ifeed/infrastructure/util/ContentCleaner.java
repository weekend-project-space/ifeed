package org.bitmagic.ifeed.infrastructure.util;

import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

public class ContentCleaner {

    private static final FlexmarkHtmlConverter CONVERTER =
            FlexmarkHtmlConverter.builder().build();

    private static final Pattern HTML_TAG_PATTERN =
            Pattern.compile("<[^>]+>", Pattern.CASE_INSENSITIVE);

    public static Content clean(String html) {
        if (!StringUtils.hasText(html)) {
            return new Content("", "");
        }

        try {
            // 使用Safelist直接解析并清理
            Document document = Jsoup.parse(html);
            document = new Cleaner(Safelist.relaxed()).clean(document);

            // 提取纯文本
            String textContent = document.text();
//            二级嵌套
            if (StringUtils.hasText(textContent) && isHtml(textContent)) {
                document = Jsoup.parse(textContent);
                document = new Cleaner(Safelist.relaxed()).clean(document);
                textContent = document.text();
            }

            // 转换为Markdown(避免重复序列化)
            String mdContent = CONVERTER.convert(document.html());

            return new Content(mdContent, textContent);

        } catch (Exception e) {
            // 记录日志
            return new Content("", ""); // 或抛出自定义异常
        }
    }

    private static boolean isHtml(String str) {
        // 快速检查是否包含HTML标签
        return HTML_TAG_PATTERN.matcher(str).find();
    }


    public record Content(String mdContent, String textContent) {
    }

//    public static void main(String[] args) {
//        Content content = clean("<section><img data-src=\"https://mmbiz.qpic.cn/sz_mmbiz_png/KmXPKA19gWic1GuW68DykycvknmG9tyBvLRsVGY4rRKCGuKKSkOqnGrvGwXxqqDxHlia88ZCbqyicswl2HC89BcZA/640?wx_fmt=png\\&from=appmsg#imgIndex=0\" data-ratio=\"0.5703703703703704\" data-s=\"300,640\" data-type=\"png\" data-w=\"1080\" type=\"block\" data-imgfileid=\"503474619\" data-original-style=\"null\" data-index=\"2\" src=\"https://image.jiqizhixin.com/uploads/editor/958bdb89-0037-4f67-87f2-d3d12c8a2eea/640.png\" alt=\"图片\" data-report-img-idx=\"0\" data-fail=\"0\" class=\"fr-fic fr-dib\" style=\"width: 700%;\"></section><blockquote><p>在推荐系统迈向多模态的今天，如何兼顾数据隐私与个性化图文理解？悉尼科技大学龙国栋教授团队联合香港理工大学杨强教授、张成奇教授团队，提出全新框架 FedVLR。该工作解决了联邦环境下多模态融合的异质性难题，已被人工智能顶级会议 AAAI 2026 接收为 Oral Presentation。</p></blockquote><p>在当今的推荐系统中，利用图像和文本等多模态信息来辅助决策已是标配。然而，当这一需求遭遇<strong>联邦学习</strong> &mdash;&mdash; 这一要求「数据不出本地」的隐私保护计算范式时，情况变得极其复杂。</p><p>现有的联邦推荐往往面临两难：要么为了保护隐私而放弃繁重的多模态处理，仅使用 ID 特征；要么采用「一刀切」（One-size-fits-all）的粗暴融合策略，假设所有用户对图文的偏好一致。</p><p>但现实是残酷的：<strong>用户的「融合偏好」天生具有极大的异质性</strong>。 购买服装时，用户可能更依赖视觉冲击；而挑选数码产品时，详尽的参数文本可能才是关键。这种偏好的差异，在数据不可见的联邦环境下，极难被捕捉。</p><p>为了打破这一瓶颈，<strong>悉尼科技大学龙国栋教授团队，联合香港理工大学人工智能高等研究院杨强院长、香港理工大学深圳研究院张成奇院长推出了 FedVLR 框架</strong>。其核心洞见在于重构了多模态融合的决策流：<strong>将重计算的特征预处理留给服务器，而将决定「怎么看」的融合决策权，通过轻量级路由机制彻底下放给用户端侧。</strong></p><section><img data-src=\"https://mmbiz.qpic.cn/sz_mmbiz_png/KmXPKA19gW9jN9uVuF3gJ5iboibrxbQ6k8sEcXvzVtpKrNUm7bkAEXRDVswRBL3MeRo3EnXFMoff6UbFyHG15nFA/640?wx_fmt=png\\&from=appmsg#imgIndex=1\" data-ratio=\"0.2361111111111111\" data-s=\"300,640\" data-type=\"png\" data-w=\"1080\" type=\"block\" data-imgfileid=\"503519755\" data-original-style=\"null\" data-index=\"3\" src=\"https://image.jiqizhixin.com/uploads/editor/2a743270-f0da-43d8-8915-56ef1642cbca/640.png\" alt=\"图片\" data-report-img-idx=\"1\" data-fail=\"0\" class=\"fr-fic fr-dib\" style=\"width: 700%;\"></section><ul><li><p>论文链接： https://arxiv.org/abs/2410.08478\\</p></li><li><p>代码仓库： https://github.com/mtics/FedVLR\\</p></li></ul><p><strong>痛点：当「多模态」遇上「数据孤岛」</strong></p><p>在传统的中心化训练中，模型可以肆无忌惮地访问所有交互数据，轻松学习到图文融合的最佳权重。但在联邦学习中，服务器看不见用户的行为数据，也就无法得知：对于用户 A 来说，到底是图片重要还是文字重要？</p><p>这种<strong>「信息不对称」</strong>导致了现有方法的局限性：</p><ol><li><p><strong>计算瓶颈</strong>：&nbsp;端侧设备算力有限，难以运行庞大的视觉 - 语言模型（如 CLIP）。</p></li><li><p><strong>个性化缺失</strong>： 全局统一的融合规则无法满足用户千差万别的浏览习惯。</p></li></ol><p><strong>FedVLR 核心架构：服务器「备菜」，客户端「掌勺」</strong></p><section><img data-src=\"https://mmbiz.qpic.cn/sz_mmbiz_png/KmXPKA19gW9jN9uVuF3gJ5iboibrxbQ6k8VTobKiaWgo8UfGTYVdFDkh0ibt5sY9ZctH6g3bY6uLH7dhcR56y6Y1QQ/640?wx_fmt=png\\&from=appmsg#imgIndex=2\" data-ratio=\"0.724007561436673\" data-s=\"300,640\" data-type=\"png\" data-w=\"1058\" type=\"block\" data-imgfileid=\"503519797\" data-original-style=\"null\" data-index=\"4\" src=\"https://image.jiqizhixin.com/uploads/editor/8bbfbf53-de6e-4813-9c99-aba85a6b91ef/640.png\" alt=\"图片\" data-report-img-idx=\"2\" data-fail=\"0\" class=\"fr-fic fr-dib\" style=\"width: 70%;\"></section><p>FedVLR 创新性地提出了一种<strong>双层融合机制</strong>，巧妙地解耦了特征提取与偏好融合。</p><p><strong>第一层：服务器端的「多视图预融合」&mdash;&mdash; 解决算力焦虑，提供丰富素材</strong></p><p>FedVLR 将繁重的计算任务锁定在服务器端。利用强大的预训练视觉 - 语言模型，服务器不直接下发原始特征，而是通过多种预设的融合算子，将物品的图像、文本和 ID 信息加工成一组<strong>「候选融合视图集」</strong>。</p><p>可以把这理解为服务器预先准备了多种口味的「半成品」：</p><ul><li><p><strong>视图 A</strong>：侧重视觉表现</p></li><li><p><strong>视图 B</strong>：侧重文本描述</p></li><li><p><strong>视图 C</strong>：图文均衡</p></li><li><p><strong>...</strong></p></li></ul><p>这些视图包含了高质量的内容理解，却无需消耗客户端的算力来生成。</p><p><strong>第二层：客户端的「个性化精炼」&mdash;&mdash;MoE 路由机制，实现千人千面</strong></p><p>当这些「半成品」视图下发到用户设备（如手机）后，FedVLR 引入了一个极其轻量的<strong>本地混合专家模块</strong>。</p><p>这个路由器的作用至关重要：它利用<strong>本地私有的交互历史</strong>，动态计算出一组个性化权重。如果本地数据显示用户偏爱看图，路由器就会赋予「视觉侧重视图」更高的权重。</p><p><strong>这一过程完全在本地发生，确保了用户的偏好数据从未离开设备。</strong></p><p><strong>工程优势：即插即用的「增强包」</strong></p><section><img data-src=\"https://mmbiz.qpic.cn/sz_mmbiz_png/KmXPKA19gW9jN9uVuF3gJ5iboibrxbQ6k8ibkBHiaKQkxc4JnicwCVZvbibnUv0mkPekrXmccZnGwtzxzY0Dj7KpgI3Q/640?wx_fmt=png\\&from=appmsg#imgIndex=3\" data-ratio=\"1.3569682151589242\" data-s=\"300,640\" data-type=\"png\" data-w=\"818\" type=\"block\" data-imgfileid=\"503519798\" data-original-style=\"null\" data-index=\"5\" src=\"https://image.jiqizhixin.com/uploads/editor/55c95077-639d-4436-8542-2d2f0b457e80/640.png\" alt=\"图片\" data-report-img-idx=\"3\" data-fail=\"0\" class=\"fr-fic fr-dib\" style=\"width: 70%;\"></section><p>FedVLR 的设计哲学不仅仅是提出一个新模型，更是提供一种<strong>通用的增强方案</strong>。</p><p>它被设计为一个可插拔的层，具有极高的工程落地价值：</p><ol><li><p><strong>模型无关性</strong>：&nbsp;它可以无缝挂载到 FedAvg、FedNCF 等任何主流的基于 ID 的联邦推荐框架上。</p></li><li><p><strong>零通信增量</strong>：&nbsp;通信过程中传输的依然是梯度或小模型参数，并未增加额外的带宽负担。</p></li><li><p><strong>隐私无损</strong>：&nbsp;严格遵循联邦学习协议，个性化参数与原始数据均保留在本地。</p></li><li><p><strong>低端侧开销</strong>： 复杂的 CLIP 编码在云端完成，端侧仅需运行轻量级的 MLP 路由网络。</p></li></ol><p><strong>实验验证：稀疏数据下的「逆袭」</strong></p><section><img data-src=\"https://mmbiz.qpic.cn/sz_mmbiz_png/KmXPKA19gW9jN9uVuF3gJ5iboibrxbQ6k8Lvzic1FGCXYiag7eWJBc24Yref1tYh62eYsXnr0mJxDbRAF40J1usc0g/640?wx_fmt=png\\&from=appmsg#imgIndex=4\" data-ratio=\"0.587037037037037\" data-s=\"300,640\" data-type=\"png\" data-w=\"1080\" type=\"block\" data-imgfileid=\"503519799\" data-original-style=\"null\" data-index=\"6\" src=\"https://image.jiqizhixin.com/uploads/editor/1b1b0b53-9977-419c-843f-cd788d2c2777/640.png\" alt=\"图片\" data-report-img-idx=\"4\" data-fail=\"0\" class=\"fr-fic fr-dib\" style=\"width: 700%;\"></section><p>研究团队在电商、多媒体等多个领域的公开数据集上进行了严苛的测试。</p><p>实验结果表明：</p><ul><li><p><strong>全面提升</strong>：&nbsp;无论基线模型如何，挂载 FedVLR 后，NDCG 和 HR 等核心推荐指标均实现了显著且稳定的提升。</p></li><li><p><strong>冷启动友好</strong>： 一个令人兴奋的发现是，在<strong>数据稀疏</strong>的场景下，FedVLR 的性能提升尤为惊人。这证明了通过个性化融合策略，模型能更有效地利用有限的本地数据来理解物品内容，甚至在部分指标上逼近了中心化训练的效果。</p></li></ul><p><strong>总结</strong></p><p>FedVLR 的价值不仅限于推荐系统本身，它更为联邦基础模型的落地提供了一种极具启发性的范式。</p><p>在端侧算力受限、而云端大模型能力日益增强的背景下，如何在不传输原始数据的前提下，让边缘设备低成本地享受到大模型的通用知识，是业界亟待解决的难题。</p><p>FedVLR 实际上展示了一种<strong>「云端大模型编码 + 端侧微调适配」</strong>的高效协同路径。它证明了我们无需在每个终端都部署庞大的多模态模型，只需通过精巧的架构设计，将云端的通用内容理解能力与端侧的私有偏好解耦。</p><p>这种思路极大地降低了联邦学习的通信与计算门槛，为未来将更复杂的视觉 - 语言模型甚至生成式 AI 引入隐私敏感场景铺平了道路，是构建下一代「既懂内容、又懂用户、且严守隐私边界」的智能系统的关键一步。</p><p><strong>目前，该论文代码已开源，欢迎社区关注与试用。</strong></p><p><strong>作者介绍</strong></p><p><strong>李志伟，悉尼科技大学博士生</strong>，研究方向为联邦推荐系统。</p><p><strong>龙国栋、江静，悉尼科技大学副教授</strong>，专注于联邦学习。</p><p><strong>张成奇，香港理工大学深圳研究院院长</strong>，在数据挖掘、人工智能理论与应用方面具有广泛影响力。</p><p><strong>杨强，香港理工大学人工智能高等研究院院长、国际人工智能领域领军人物</strong>，提出迁移学习与联邦学习多项奠基性成果。</p>");
//        System.out.println(content.textContent);
//    }
}
