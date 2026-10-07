package org.bitmagic.ifeed.infrastructure.util;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * @author yangrd
 * @date 2025/11/8
 **/
public class DateUtils {


    private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/M/d");

    /**
     * 格式化相对时间（类似：刚刚、5分钟前、3小时前、2天前、2025/11/8）
     *
     * @param instant  等
     * @return 相对时间字符串
     */
    public static String formatRelativeTime(Instant instant) {
        if (instant == null) {
            return "未知时间";
        }

        Instant now = Instant.now();
        long diffMinutes = Duration.between(instant, now).toMinutes();

        if (diffMinutes < 1) {
            return "刚刚";
        }
        if (diffMinutes < 60) {
            return diffMinutes + " 分钟前";
        }

        long diffHours = diffMinutes / 60;
        if (diffHours < 24) {
            return diffHours + " 小时前";
        }

        long diffDays = diffHours / 24;
        if (diffDays < 7) {
            return diffDays + " 天前";
        }

        // 转为本地日期显示
        return LocalDate.ofInstant(instant, DEFAULT_ZONE).format(DATE_FORMATTER);
    }

}
