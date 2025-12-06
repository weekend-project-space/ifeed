package org.bitmagic.ifeed.infrastructure;

import com.huaban.analysis.jieba.JiebaSegmenter;
import com.huaban.analysis.jieba.SegToken;
import com.rometools.utils.Strings;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author yangrd
 * @date 2025/11/19
 **/
@Slf4j
public class TermUtils {
    static JiebaSegmenter segmenter = new JiebaSegmenter();

    // 你可以放到 resources/idf.txt （可选）
    private static final Map<String, Double> idfDict = new HashMap<>();
    private static final double idfAverage = 8.0; // 默认平均 IDF

    static {
        // 可从文件加载
        // idfDict.put("北京", 9.7);
        // 或动态添加常用权重
        idfDict.put("中国", 6.0);
        idfDict.put("新闻", 5.0);
        idfDict.put("文章", 4.0);
    }


    public static List<String> keywords(String content, int topK) {
        return extract(content, topK);
    }


    public static String segmentStr(String seq) {
        if (Strings.isBlank(seq)) {
            return "";
        }
        return String.join(" ", segment(seq));
    }

    /**
     * 执行分词
     *
     * @param text 待分词文本
     */
    private static List<String> segment(String text) {
//        List<SegToken> tokens = ;
        return segmenter.sentenceProcess(text); //tokens.stream().map(segToken -> segToken.word).toList();
    }


    private static List<String> extract(String text, int topK) {

        // 1. 分词
        List<String> words = segmenter.process(text, JiebaSegmenter.SegMode.SEARCH)
                .stream()
                .map(t -> t.word)
                .filter(w -> w.length() > 1) // 去掉无意义词
                .toList();

        // 2. TF
        Map<String, Long> tf = words.stream()
                .collect(Collectors.groupingBy(w -> w, Collectors.counting()));

        // 3. TF-IDF 排序
        Map<String, Double> scores = new HashMap<>();
        for (String w : tf.keySet()) {
            double tfScore = tf.get(w);
            double idfScore = idfDict.getOrDefault(w, idfAverage);
            scores.put(w, tfScore * idfScore);
        }

        // 4. 排序取 topK
        return scores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(topK)
                .map(Map.Entry::getKey)
                .toList();
    }

}
