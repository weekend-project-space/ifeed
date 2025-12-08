package org.bitmagic.ifeed.domain.model.value;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FeedCategory {
    ALL("all", "全部", "📚", "所有分类的订阅源"),
    TECH("tech", "科技", "💻", "科技、编程、互联网相关内容"),
    NEWS("news", "新闻", "📰", "时事新闻、国际资讯"),
    DESIGN("design", "设计", "🎨", "UI/UX设计、平面设计、创意"),
    BUSINESS("business", "商业", "💼", "商业、创业、经济"),
    LIFESTYLE("lifestyle", "生活", "🌟", "生活方式、健康、美食"),
    ENTERTAINMENT("entertainment", "娱乐", "🎬", "影视、音乐、游戏"),
    SPORTS("sports", "体育", "⚽", "体育赛事、运动健身");

    private final String code;
    private final String name;
    private final String icon;
    private final String description;

    public static FeedCategory fromCode(String code) {
        if (code == null || code.isBlank()) {
            return TECH;
        }
        for (FeedCategory category : values()) {
            if (category.code.equalsIgnoreCase(code.trim())) {
                return category;
            }
        }
        return TECH;
    }

    public static boolean isValidCode(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        for (FeedCategory category : values()) {
            if (category.code.equalsIgnoreCase(code.trim())) {
                return true;
            }
        }
        return false;
    }
}
