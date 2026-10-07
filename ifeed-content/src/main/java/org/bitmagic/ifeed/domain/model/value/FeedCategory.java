package org.bitmagic.ifeed.domain.model.value;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum FeedCategory {

    // 特殊分类
    ALL("all", "全部", "📚", "所有分类的订阅源", 0),

    // 基础类别
    TECH("tech", "科技", "💻", "技术资讯、编程开发、产品评测", 1),
    NEWS("news", "时事", "📰", "时政、社会、突发事件", 2),
    BUSINESS("business", "商业", "💼", "商业分析、创业、企业管理", 3),
    FINANCE("finance", "财经", "💰", "投资理财、股市、经济政策", 4),
    ENTERTAINMENT("entertainment", "娱乐", "🎬", "影视、音乐、明星动态", 5),
    SPORTS("sports", "体育", "⚽", "体育赛事、运动健身", 6),
    LIFESTYLE("lifestyle", "生活", "🌟", "生活技巧、品质生活", 7),

    // 高频类别
    EDUCATION("education", "教育", "📖", "学习方法、在线课程、教育资讯", 8),
    HEALTH("health", "健康", "🏥", "医疗资讯、养生保健、心理健康", 9),
    FOOD("food", "美食", "🍽️", "菜谱、餐厅推荐、美食文化", 10),
    TRAVEL("travel", "旅游", "✈️", "旅行攻略、目的地推荐", 11),
    CULTURE("culture", "文化", "🎭", "文学、艺术、历史、人文", 12),
    SCIENCE("science", "科学", "🔬", "科普知识、科研成果、自然探索", 13),
    DESIGN("design", "设计", "🎨", "视觉设计、UI/UX、创意思路", 14),

    // 补充类别
    AUTO("auto", "汽车", "🚗", "汽车评测、行业动态、用车知识", 15),
    FASHION("fashion", "时尚", "👗", "服装穿搭、潮流趋势、美妆护肤", 16),
    CAREER("career", "职场", "💼", "求职技巧、职业规划、职场经验", 17),
    GAMING("gaming", "游戏", "🎮", "游戏攻略、电竞资讯、游戏评测", 18),
    OPINION("opinion", "观点", "💭", "深度评论、时事分析、专栏文章", 19),

    // 兜底类别
    OTHER("other", "其他", "📦", "其他优质内容", 99);

    private final String code;
    private final String name;
    private final String icon;
    private final String description;
    private final int order;

    // 静态缓存 Map，提高查询性能
    private static final Map<String, FeedCategory> CODE_MAP = Arrays.stream(values())
            .collect(Collectors.toMap(
                    category -> category.code.toLowerCase(),
                    category -> category
            ));

    /**
     * 根据 code 获取分类
     *
     * @param code 分类代码
     * @return 对应的分类，如果不存在返回 OTHER
     */
    public static FeedCategory fromCode(String code) {
        if (code == null || code.isBlank()) {
            return OTHER;
        }
        return CODE_MAP.getOrDefault(code.trim().toLowerCase(), OTHER);
    }


    /**
     * 验证 code 是否有效
     *
     * @param code 分类代码
     * @return 是否为有效的分类代码
     */
    public static boolean isValidCode(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        return CODE_MAP.containsKey(code.trim().toLowerCase());
    }


    @Override
    public String toString() {
        return String.format("%s(%s)", name, code);
    }
}
