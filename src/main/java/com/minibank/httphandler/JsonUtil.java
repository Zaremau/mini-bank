package com.minibank.httphandler;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonUtil {

    private static final Pattern JSON_PAIR_PATTERN =
            Pattern.compile("\"([^\"]+)\"\\s*:\\s*(?:\"([^\"]*)\"|([^,\\s}]+))");

    public static Map<String, String> parseFlatJson(String json) {
        Map<String, String> result = new HashMap<>();
        if (json == null || json.isBlank()) {
            return result;
        }
        Matcher matcher = JSON_PAIR_PATTERN.matcher(json);
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2) != null ? matcher.group(2) : matcher.group(3);
            result.put(key, value);
        }
        return result;
    }

    public static String escape(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
