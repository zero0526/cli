package com.fb.cli.domains.facebook.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fb.cli.utils.JsonUtils;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class GenericResponseParser {

    @Data
    @Builder
    public static class ParseResult {
        private boolean success;
        private String extractedId;
        private String notice;
        private String errorCode;
        private String errorMessage;
    }

    public static ParseResult parse(String responseText, String extractorRulesJson) {
        if (responseText == null || responseText.isBlank()) {
            return ParseResult.builder()
                    .success(false)
                    .errorCode("EMPTY_RESPONSE")
                    .errorMessage("Phản hồi rỗng từ Facebook")
                    .build();
        }

        try {
            JsonNode root = JsonUtils.MAPPER.readTree(responseText);
            JsonNode rules = (extractorRulesJson != null && !extractorRulesJson.isBlank())
                    ? JsonUtils.MAPPER.readTree(extractorRulesJson)
                    : null;

            // 1. Kiểm tra success_path
            String successPath = rules != null ? rules.path("success_path").asText(null) : null;
            if (successPath != null) {
                JsonNode successNode = getNodeByPath(root, successPath);
                if (successNode != null && !successNode.isMissingNode() && !successNode.isNull()) {
                    String extractedId = successNode.asText();

                    // Bóc tách qua id_extractor nếu có cấu hình
                    JsonNode idExtractor = rules.path("id_extractor");
                    if (!idExtractor.isMissingNode()) {
                        String decodeType = idExtractor.path("decode").asText("");
                        String regex = idExtractor.path("regex").asText("");
                        if ("BASE64".equalsIgnoreCase(decodeType)) {
                            extractedId = decodeBase64WithRegex(extractedId, regex);
                        }
                    }

                    return ParseResult.builder()
                            .success(true)
                            .extractedId(extractedId)
                            .build();
                }
            }

            // 2. Kiểm tra notice_path (nếu có)
            String noticePath = rules != null ? rules.path("notice_path").asText(null) : null;
            if (noticePath != null) {
                JsonNode noticeNode = getNodeByPath(root, noticePath);
                if (noticeNode != null && !noticeNode.isMissingNode() && !noticeNode.isNull()) {
                    return ParseResult.builder()
                            .success(true)
                            .notice(noticeNode.asText())
                            .build();
                }
            }

            // 3. Xử lý lỗi
            String errorCode = "ACTION_FAILED";
            String errorMessage = "Thao tác thất bại";

            JsonNode errors = root.path("errors");
            if (errors.isArray() && errors.size() > 0) {
                JsonNode firstErr = errors.get(0);
                if (firstErr.has("description")) {
                    errorMessage = firstErr.get("description").asText();
                } else if (firstErr.has("message")) {
                    errorMessage = firstErr.get("message").asText();
                }
                if (firstErr.has("code")) {
                    errorCode = String.valueOf(firstErr.get("code").asText());
                }
            }

            return ParseResult.builder()
                    .success(false)
                    .errorCode(errorCode)
                    .errorMessage(errorMessage)
                    .build();

        } catch (Exception e) {
            log.error("[GenericResponseParser] Lỗi parse JSON response: {}", e.getMessage());
            return ParseResult.builder()
                    .success(false)
                    .errorCode("PARSE_ERROR")
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    private static JsonNode getNodeByPath(JsonNode root, String dotPath) {
        if (root == null || dotPath == null || dotPath.isBlank()) {
            return null;
        }
        String[] parts = dotPath.split("\\.");
        JsonNode current = root;
        for (String part : parts) {
            if (current == null || current.isMissingNode()) {
                return null;
            }
            current = current.path(part);
        }
        return current;
    }

    private static String decodeBase64WithRegex(String encoded, String regex) {
        try {
            String decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
            if (regex != null && !regex.isBlank()) {
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(decoded);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
            return decoded;
        } catch (Exception e) {
            return encoded;
        }
    }
}
