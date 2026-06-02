package com.company.payrouter.common.logging;

import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SensitiveLogMasker {
    private static final String MASK = "****";
    private static final String SENSITIVE_KEYS = "sign|authCode|apiKey|secret|apiSecret|signKey|privateKey|publicKey|certPassword";
    private static final Pattern JSON_FIELD = Pattern.compile("(?i)(\"(?:" + SENSITIVE_KEYS + ")\"\\s*:\\s*\")([^\"]*)(\")");
    private static final Pattern KEY_VALUE_FIELD = Pattern.compile("(?i)(^|[&?\\s,{])(" + SENSITIVE_KEYS + ")=([^&\\s,}]+)");

    private SensitiveLogMasker() {
    }

    public static String mask(String text) {
        if (!StringUtils.hasText(text)) {
            return text;
        }
        String masked = replaceJsonFields(text);
        return replaceKeyValueFields(masked);
    }

    private static String replaceJsonFields(String text) {
        Matcher matcher = JSON_FIELD.matcher(text);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(builder, Matcher.quoteReplacement(matcher.group(1) + MASK + matcher.group(3)));
        }
        matcher.appendTail(builder);
        return builder.toString();
    }

    private static String replaceKeyValueFields(String text) {
        Matcher matcher = KEY_VALUE_FIELD.matcher(text);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(builder, Matcher.quoteReplacement(matcher.group(1) + matcher.group(2) + "=" + MASK));
        }
        matcher.appendTail(builder);
        return builder.toString();
    }
}
