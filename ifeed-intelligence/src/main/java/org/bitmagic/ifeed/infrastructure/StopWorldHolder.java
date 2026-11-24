package org.bitmagic.ifeed.infrastructure;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author yangrd
 * @date 2025/12/24
 **/
public class StopWorldHolder {

    // ==================== 停用词表 ====================
    public static final Set<String> STOP_WORDS = buildStopWords();

    private static Set<String> buildStopWords() {
        Set<String> words = new HashSet<>();

        // === 英文通用停用词 ===
        words.addAll(List.of(
                "the", "a", "an", "and", "or", "but", "yet", "so", "because", "since", "although", "though",
                "whereas", "while", "if", "unless", "until", "before", "after", "this", "that", "it", "you",
                "i", "we", "they", "he", "she", "me", "us", "him", "her", "them", "is", "are", "was", "were",
                "be", "been", "being", "have", "has", "had", "do", "does", "did", "will", "would", "should",
                "could", "may", "might", "must", "can", "in", "on", "at", "to", "for", "of", "with", "from",
                "by", "as", "about", "into", "through", "during", "above", "below", "under", "between",
                "among", "across", "around", "over", "without", "within", "that", "which", "who", "what",
                "when", "where", "why", "how", "all", "each", "every", "both", "either", "neither", "any",
                "some", "many", "much", "more", "most", "less", "least", "such", "so", "very", "really",
                "quite", "rather", "fairly", "pretty", "just", "only", "even", "also", "too", "as well",
                "new", "old", "same", "different", "similar", "other", "another", "first", "second", "last",
                "next", "seems", "appears", "looks", "feels", "sounds", "becomes", "turned", "proved",
                "according", "say", "said", "says", "report", "reports", "reported"
        ));

        // === 中文通用停用词 ===
        words.addAll(List.of(
                "的", "了", "和", "是", "在", "有", "就", "不", "人", "都", "一", "个", "上", "也", "很",
                "到", "说", "要", "去", "好", "为", "中", "来", "我", "对", "从", "以", "其", "还", "并",
                "等", "而", "后", "将", "被", "于", "及", "与", "更", "已", "通过", "可以", "但", "谁",
                "什么", "哪个", "哪里", "哪些", "如何", "为什么", "怎样", "怎么", "多少", "多久", "几个",
                "某个", "某些", "彼此", "今年", "去年", "明年", "今天", "明天", "当时", "近日", "最近",
                "一直", "总是", "经常", "常常", "有时", "偶尔", "称", "表示", "据", "指出", "宣布", "透露",
                "认为", "觉得", "似乎", "看起来", "显然", "看来", "比如", "例如", "呢", "吗", "吧", "啦",
                "哈", "嘛", "哪", "呀", "啊", "哦", "而已", "罢了", "似的", "样", "般", "因此", "所以",
                "既然", "否则", "另外", "反而", "总之", "总的来说", "一般来说", "通常", "往往", "好像",
                "大概", "需要", "能够", "可能", "应该", "方面", "根据", "报道", "进行", "开展", "实现",
                "推进", "以及", "其中", "包括", "相关", "主要", "已经",
                "-", ",", "."
        ));

        return Collections.unmodifiableSet(words);
    }


}
