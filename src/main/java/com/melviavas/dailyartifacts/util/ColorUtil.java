package com.melviavas.dailyartifacts.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatColor;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Поддерживает одновременно два формата цвета:
 *  - legacy HEX:  &#RRGGBB (+ обычные & коды)
 *  - MiniMessage: <gradient:#..:..>, <bold>, <red> и т.д.
 *
 * Строка сначала прогоняется через MiniMessage (если в ней есть теги <...>),
 * затем через legacy-HEX парсер для оставшихся &#RRGGBB / & кодов.
 */
public final class ColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private ColorUtil() {}

    /** Возвращает готовую цветную строку (legacy-секции), пригодную для ItemMeta/чата. */
    public static String colorize(String input) {
        if (input == null) return "";

        String text = input;

        // MiniMessage теги -> сериализуем обратно в legacy, чтобы дальше можно было
        // единообразно работать со строкой (title/lore API Paper принимает String с & кодами
        // либо Component; здесь используем String-путь для простоты и совместимости).
        if (text.contains("<") && text.contains(">")) {
            try {
                Component component = MINI_MESSAGE.deserialize(text);
                text = LEGACY.serialize(component);
            } catch (Exception ignored) {
                // если парсинг не удался — считаем что это просто текст со скобками, не трогаем
            }
        }

        // legacy &#RRGGBB -> настоящий HEX-цвет Minecraft (через ChatColor.of)
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(1);
            matcher.appendReplacement(buffer, ChatColor.of("#" + hex).toString());
        }
        matcher.appendTail(buffer);
        text = buffer.toString();

        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static List<String> colorize(List<String> lines) {
        return lines.stream().map(ColorUtil::colorize).collect(Collectors.toList());
    }

    public static String strip(String input) {
        return ChatColor.stripColor(colorize(input));
    }
}
