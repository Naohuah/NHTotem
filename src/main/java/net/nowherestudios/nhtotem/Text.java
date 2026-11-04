package net.nowherestudios.nhtotem;

import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文本处理工具：把配置里的 &amp; 颜色代码、&amp;#RRGGBB 十六进制颜色转换成 Minecraft 的颜色代码。
 */
@SuppressWarnings("deprecation")
public final class Text {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([0-9A-Fa-f]{6})");

    private Text() {
    }

    /**
     * 把配置文本转换为带颜色代码的字符串。支持 &amp; 代码和 &amp;#RRGGBB。
     */
    public static String color(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        String translated = replaceHex(input);
        return ChatColor.translateAlternateColorCodes('&', translated);
    }

    /**
     * 去掉颜色代码，得到纯文本。
     */
    public static String strip(String input) {
        return ChatColor.stripColor(color(input));
    }

    private static String replaceHex(String input) {
        Matcher matcher = HEX_PATTERN.matcher(input);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("\u00a7x");
            for (int i = 0; i < hex.length(); i++) {
                replacement.append('\u00a7').append(hex.charAt(i));
            }
            matcher.appendReplacement(builder, Matcher.quoteReplacement(replacement.toString()));
        }
        matcher.appendTail(builder);
        return builder.toString();
    }
}
