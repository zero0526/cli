package com.fb.cli.domains.facebook.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fb.cli.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class ActionTemplateResolver {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{(\\w+)}}");

    /**
     * Thay thế tất cả placeholder {{key}} trong chuỗi template bằng giá trị từ context.
     * Nếu placeholder được bao bởi dấu nháy kép "{{key}}" và giá trị thay thế là một JSON array [...] hoặc JSON object {...},
     * dấu nháy kép sẽ được bóc bỏ để giữ nguyên cấu trúc JSON hợp lệ.
     */
    public static String resolveTemplate(String template, Map<String, String> context) {
        if (template == null || template.isBlank() || context == null || context.isEmpty()) {
            return template;
        }

        String result = template;
        // Bóc tách trước các placeholder dạng "{{key}}" có value là JSON array/object
        for (Map.Entry<String, String> entry : context.entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue();
            if (val != null) {
                String trimmed = val.trim();
                if ((trimmed.startsWith("[") && trimmed.endsWith("]")) || (trimmed.startsWith("{") && trimmed.endsWith("}"))) {
                    String quotedPlaceholder = "\"\\{\\{" + key + "\\}\\}\"";
                    result = result.replaceAll(quotedPlaceholder, Matcher.quoteReplacement(trimmed));
                }
            }
        }

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(result);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = context.getOrDefault(key, "");
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Parse chuỗi JSON template Map<String, String> và resolve từng value bằng context
     */
    public static Map<String, String> resolveMapTemplate(String jsonMapTemplate, Map<String, String> context) {
        if (jsonMapTemplate == null || jsonMapTemplate.isBlank()) {
            return Collections.emptyMap();
        }

        try {
            Map<String, String> rawMap = JsonUtils.MAPPER.readValue(
                    jsonMapTemplate,
                    new TypeReference<LinkedHashMap<String, String>>() {}
            );

            Map<String, String> result = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : rawMap.entrySet()) {
                String resolvedValue = resolveTemplate(entry.getValue(), context);
                result.put(entry.getKey(), resolvedValue);
            }
            return result;
        } catch (Exception e) {
            log.error("[ActionTemplateResolver] Lỗi parse JSON map template: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
}
