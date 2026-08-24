package com.melviavas.dailyartifacts.util;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TimeUtil {

    private static final Pattern DURATION = Pattern.compile("(\\d+)\\s*([smhd])", Pattern.CASE_INSENSITIVE);

    private TimeUtil() {}

    /** Парсит строки вида "1h", "7d", "30m", "45s" в миллисекунды. */
    public static long parseDuration(String value) {
        if (value == null) return 0L;
        Matcher m = DURATION.matcher(value.trim());
        if (!m.matches()) return 0L;
        long amount = Long.parseLong(m.group(1));
        return switch (m.group(2).toLowerCase()) {
            case "s" -> amount * 1000L;
            case "m" -> amount * 60_000L;
            case "h" -> amount * 3_600_000L;
            case "d" -> amount * 86_400_000L;
            default -> 0L;
        };
    }

    /** Форматирует оставшееся время (мс) согласно режиму, заданному в config.yml. */
    public static String formatRemaining(long remainingMillis, FileConfiguration config) {
        if (remainingMillis < 0) remainingMillis = 0;

        long totalSeconds = remainingMillis / 1000;
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;

        String mode = config.getString("time-format.mode", "format1");

        if (mode.equalsIgnoreCase("format2")) {
            if (totalSeconds < 60) {
                return ColorUtil.colorize(config.getString("time-format.format2.less-than-minute", "< минуты"));
            }
            String template;
            if (days > 0) {
                template = config.getString("time-format.format2.full", "{days}д {hours}ч {minutes}м");
            } else if (hours > 0) {
                template = config.getString("time-format.format2.hours-minutes", "{hours}ч {minutes}м");
            } else {
                template = config.getString("time-format.format2.minutes-only", "{minutes}м");
            }
            return ColorUtil.colorize(apply(template, days, hours, minutes));
        }

        // format1
        if (totalSeconds < 3600) {
            return ColorUtil.colorize(config.getString("time-format.format1.less-than-hour", "< часа"));
        }
        String template;
        if (days > 0 && hours > 0) {
            template = config.getString("time-format.format1.both", "{days}д {hours}ч");
        } else if (days > 0) {
            template = config.getString("time-format.format1.days-only", "{days}д");
        } else {
            template = config.getString("time-format.format1.hours-only", "{hours}ч");
        }
        return ColorUtil.colorize(apply(template, days, hours, minutes));
    }

    private static String apply(String template, long days, long hours, long minutes) {
        return template
                .replace("{days}", String.valueOf(days))
                .replace("{hours}", String.valueOf(hours))
                .replace("{minutes}", String.valueOf(minutes));
    }
}
