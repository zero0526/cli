package com.fb.cli.utils;

import com.ethlo.time.DateTime;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import com.fb.cli.dtos.proxy.ProxyInfo;
import lombok.extern.slf4j.Slf4j;

import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
public class JsonUtils {

    public static ObjectMapper MAPPER;

    public static JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
    public static String dirPath= "logs/";
    static {
        MAPPER = new ObjectMapper();
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    }

    public static String marshal(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("error marshal object: ", e);
            return "";
        }
    }
    public static String getProperty(String jsonObj, String key) {
        try {
            JsonNode node = MAPPER.readTree(jsonObj);
            JsonNode valueNode = node.get(key);

            if (valueNode != null && !valueNode.isNull()) {
                return valueNode.asText();
            }
        } catch (JsonProcessingException e) {
            log.error("[getProperty] error marshal object: ", e);
        }
        return "";
    }
    public static boolean hasProperty(String jsonObj, String key) {
        try {
            JsonNode node = MAPPER.readTree(jsonObj);
            return node.has(key);
        } catch (JsonProcessingException e) {
            return false;
        }
    }
    public static Map<String, Object> toMap(String jsonStr) {
        try {
            if (jsonStr == null || jsonStr.isEmpty()) {
                return new HashMap<>();
            }
            return MAPPER.readValue(jsonStr, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            System.err.println("Lỗi parse JSON: " + e.getMessage());
            return new HashMap<>();
        }
    }
    public static String marshalPretty(Object obj) throws JsonProcessingException {
        return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
    }

    public static <T> T unmarshal(Object object, Class<T> clazz) {
        return MAPPER.convertValue(object, clazz);
    }

    public static <T> T unmarshal(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("error unmarshal object: ", e);
            return null;
        }
    }

    public static boolean isValidJson(String json) {
        try {
            MAPPER.readTree(json);
            return true;
        } catch (Exception e) {
            log.error("[isValidJson] - error parse json: ", e);
        }
        return false;
    }

    public static List<String> validateJsonSchema(String jsonSchema, String json) {
        JsonSchema schema;
        JsonNode jsonNode;
        try {
            schema = jsonSchemaFactory.getSchema(jsonSchema);
            jsonNode = JsonUtils.MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            log.error("[validateJsonSchema] - error parse json: ", e);
            return List.of("json is not valid");
        }
        List<String> errors = new ArrayList<>();
        Set<ValidationMessage> validate = schema.validate(jsonNode);
        for (ValidationMessage v : validate) {
            errors.add(v.getMessage());
        }
        return errors;
    }

    public static <T> T unmarshal(String json, TypeReference<T> typeRef) {
        try {
            return MAPPER.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            log.error("error unmarshal object: ", e);
            return null;
        }
    }
    public static String getString(JsonObject obj, String key, String defaultVal) {
        if (obj != null && obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return defaultVal;
    }

    public static Long getLong(JsonObject obj, String key, Long defaultVal) {
        if (obj != null && obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsLong();
        }
        return defaultVal;
    }

    public static JsonObject getObject(JsonObject obj, String key) {
        if (obj != null && obj.has(key) && obj.get(key).isJsonObject()) {
            return obj.getAsJsonObject(key);
        }
        return null;
    }
    public static void writeToFile(List<JsonObject> objects, String fileName) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMdd_HHmmss"));

        String filePath = new StringBuilder()
                .append(dirPath)
                .append(timestamp)
                .append("_")
                .append(fileName)
                .toString();

        try (FileWriter writer = new FileWriter(filePath)) {
            gson.toJson(objects, writer);

            log.info("Ghi file thành công: " + filePath);
        } catch (IOException e) {
            log.error("Lỗi khi ghi file: " + e.getMessage());
        }
    }

    /**
     * Lấy giá trị code số từ response JSON (default = -1 nếu không có hoặc lỗi)
     */
    public static int getCode(String jsonStr) {
        try {
            JsonNode node = MAPPER.readTree(jsonStr);
            return node.path("code").asInt(-1);
        } catch (Exception e) {
            log.error("[getCode] error parse json: ", e);
            return -1;
        }
    }

    /**
     * Lấy message từ response JSON
     */
    public static String getMessage(String jsonStr) {
        try {
            JsonNode node = MAPPER.readTree(jsonStr);
            return node.path("message").asText("");
        } catch (Exception e) {
            log.error("[getMessage] error parse json: ", e);
            return "";
        }
    }

    /**
     * Trích xuất node data từ response JSON
     */
    public static JsonNode getDataNode(String jsonStr) {
        try {
            JsonNode node = MAPPER.readTree(jsonStr);
            return node.path("data");
        } catch (Exception e) {
            log.error("[getDataNode] error parse json: ", e);
            return null;
        }
    }

    /**
     * Parse chuỗi JSON response của provider thành ProxyInfo chuẩn
     */
    public static ProxyInfo parseProxyInfo(String jsonStr) {
        try {
            JsonNode root = MAPPER.readTree(jsonStr);
            JsonNode dataNode = root.has("data") ? root.get("data") : root;

            if (dataNode == null || dataNode.isMissingNode() || dataNode.isNull()) {
                throw new IllegalStateException("Proxy response data is empty");
            }

            String proxyStr = null;
            String protocol = "http";

            if (dataNode.hasNonNull("https") && !dataNode.get("https").asText().isBlank()) {
                proxyStr = dataNode.get("https").asText();
                protocol = "https";
            } else if (dataNode.hasNonNull("socks5") && !dataNode.get("socks5").asText().isBlank()) {
                proxyStr = dataNode.get("socks5").asText();
                protocol = "socks5";
            } else if (dataNode.hasNonNull("proxy") && !dataNode.get("proxy").asText().isBlank()) {
                proxyStr = dataNode.get("proxy").asText();
            }

            if (proxyStr == null || proxyStr.isBlank()) {
                throw new IllegalStateException("No proxy host/port found in response: " + jsonStr);
            }

            String host = null;
            Integer port = null;
            String username = null;
            String password = null;

            if (proxyStr.contains("@")) {
                String[] parts = proxyStr.split("@", 2);
                String[] auth = parts[0].split(":", 2);
                username = auth[0];
                if (auth.length > 1) password = auth[1];

                String[] hostPort = parts[1].split(":", 2);
                host = hostPort[0];
                if (hostPort.length > 1) port = Integer.parseInt(hostPort[1].trim());
            } else {
                String[] parts = proxyStr.split(":");
                if (parts.length >= 2) {
                    host = parts[0].trim();
                    port = Integer.parseInt(parts[1].trim());
                }
                if (parts.length >= 4) {
                    username = parts[2].trim();
                    password = parts[3].trim();
                }
            }

            Long expireAt = null;
            if (dataNode.hasNonNull("timeout")) {
                long timeoutSec = dataNode.get("timeout").asLong();
                if (timeoutSec > 0) {
                    expireAt = System.currentTimeMillis() + (timeoutSec * 1000L);
                }
            }

            return new ProxyInfo(host, port, username, password, protocol, expireAt);
        } catch (Exception e) {
            log.error("[parseProxyInfo] error parsing proxy info from json: {}", jsonStr, e);
            if (e instanceof RuntimeException re) throw re;
            throw new RuntimeException("Failed to parse ProxyInfo: " + e.getMessage(), e);
        }
    }

    /**
     * Parse thời gian hết hạn từ response stats của provider (trường expired_at)
     */
    public static LocalDateTime parseExpiredTime(String jsonStr) {
        try {
            JsonNode root = MAPPER.readTree(jsonStr);
            JsonNode dataNode = root.has("data") ? root.get("data") : root;

            if (dataNode == null || !dataNode.hasNonNull("expired_at")) {
                throw new IllegalStateException("Response data does not contain 'expired_at'");
            }

            String expiredAtStr = dataNode.get("expired_at").asText();
            return parseDateTime(expiredAtStr);
        } catch (Exception e) {
            log.error("[parseExpiredTime] error parsing expired time: {}", jsonStr, e);
            if (e instanceof RuntimeException re) throw re;
            throw new RuntimeException("Failed to parse expired_at: " + e.getMessage(), e);
        }
    }

    /**
     * Parse chuỗi ngày giờ linh hoạt thành LocalDateTime
     */
    public static LocalDateTime parseDateTime(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        // 1. Format ISO-8601 (vd: 2025-01-15T07:35:03.473Z)
        try {
            return OffsetDateTime.parse(text).toLocalDateTime();
        } catch (Exception ignored) {}

        try {
            return Instant.parse(text).atZone(ZoneId.systemDefault()).toLocalDateTime();
        } catch (Exception ignored) {}

        // 2. Format ISO Local Date Time
        try {
            return LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception ignored) {}

        // 3. Các pattern ngày giờ thông dụng
        String[] patterns = {"yyyy-MM-dd HH:mm:ss", "dd/MM/yyyy HH:mm:ss", "yyyy/MM/dd HH:mm:ss"};
        for (String pattern : patterns) {
            try {
                return LocalDateTime.parse(text, DateTimeFormatter.ofPattern(pattern));
            } catch (Exception ignored) {}
        }

        // 4. Unix epoch millis / seconds
        try {
            long epoch = Long.parseLong(text);
            if (epoch > 1_000_000_000_000L) {
                return Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault()).toLocalDateTime();
            } else {
                return Instant.ofEpochSecond(epoch).atZone(ZoneId.systemDefault()).toLocalDateTime();
            }
        } catch (Exception ignored) {}

        throw new IllegalArgumentException("Unable to parse date string: " + text);
    }
}
