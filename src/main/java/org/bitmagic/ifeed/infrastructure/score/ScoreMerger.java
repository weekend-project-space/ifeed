package org.bitmagic.ifeed.infrastructure.score;

import java.util.HashMap;
import java.util.Map;

/**
 * @author yangrd
 * @date 2025/12/15
 **/
public class ScoreMerger {

    // 评级对应的分数
    private static final Map<String, Double> GRADE_SCORES = new HashMap<>();
    static {
        GRADE_SCORES.put("A", 1.0);
        GRADE_SCORES.put("B", 0.75);
        GRADE_SCORES.put("C", 0.5);
        GRADE_SCORES.put("D", 0.25);
        GRADE_SCORES.put("E", 0.0);
    }

    /**
     * 综合两个评级（默认权重 60:40）
     *
     * @param grade1 第一个评级（权重60%）
     * @param grade2 第二个评级（权重40%）
     * @return 综合后的评级
     */
    public static String merge(String grade1, String grade2) {
        return merge(grade1, grade2, 0.6, 0.4);
    }

    /**
     * 综合两个评级（自定义权重）
     *
     * @param grade1 第一个评级
     * @param grade2 第二个评级
     * @param weight1 第一个评级的权重
     * @param weight2 第二个评级的权重
     * @return 综合后的评级
     */
    public static String merge(String grade1, String grade2, double weight1, double weight2) {
        // 参数校验
        if (grade1 == null || grade2 == null) {
            return grade1 != null ? grade1 : (grade2 != null ? grade2 : "E");
        }

        // 转换为大写
        grade1 = grade1.trim().toUpperCase();
        grade2 = grade2.trim().toUpperCase();

        // 获取分数
        Double score1 = GRADE_SCORES.get(grade1);
        Double score2 = GRADE_SCORES.get(grade2);

        if (score1 == null || score2 == null) {
            throw new IllegalArgumentException("Invalid grade: " + grade1 + " or " + grade2);
        }

        // 加权计算
        double finalScore = score1 * weight1 + score2 * weight2;

        // 转换回评级
        return scoreToGrade(finalScore);
    }

    /**
     * 分数转评级
     */
    private static String scoreToGrade(double score) {
        if (score >= 0.85) return "A";
        if (score >= 0.625) return "B";  // (0.75 + 0.5) / 2
        if (score >= 0.375) return "C";  // (0.5 + 0.25) / 2
        if (score >= 0.125) return "D";  // (0.25 + 0.0) / 2
        return "E";
    }

    /**
     * 评级转分数（用于其他计算）
     */
    public static double gradeToScore(String grade) {
        if (grade == null) return 0.0;
        return GRADE_SCORES.getOrDefault(grade.trim().toUpperCase(), 0.0);
    }

    /**
     * 综合多个评级（支持更多维度）
     *
     * @param grades 评级数组
     * @param weights 对应的权重数组
     * @return 综合评级
     */
    public static String mergeMultiple(String[] grades, double[] weights) {
        if (grades == null || weights == null || grades.length != weights.length) {
            throw new IllegalArgumentException("Grades and weights must have same length");
        }

        double totalScore = 0.0;
        double totalWeight = 0.0;

        for (int i = 0; i < grades.length; i++) {
            if (grades[i] != null) {
                Double score = GRADE_SCORES.get(grades[i].trim().toUpperCase());
                if (score != null) {
                    totalScore += score * weights[i];
                    totalWeight += weights[i];
                }
            }
        }

        if (totalWeight == 0) return "E";

        double finalScore = totalScore / totalWeight;
        return scoreToGrade(finalScore);
    }

    /**
     * 解析评级字符串（例如 "A C" -> ["A", "C"]）
     */
    public static String[] parseGrades(String gradeString) {
        if (gradeString == null || gradeString.trim().isEmpty()) {
            return new String[]{"E", "E"};
        }

        String[] parts = gradeString.trim().split("\\s+");
        if (parts.length >= 2) {
            return new String[]{parts[0], parts[1]};
        } else if (parts.length == 1) {
            return new String[]{parts[0], "C"}; // 默认第二个为C
        }

        return new String[]{"E", "E"};
    }

}
