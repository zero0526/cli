package com.fb.cli.services.external.proxy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fb.cli.dtos.proxy.ProxyInfo;
import com.fb.cli.dtos.proxy.ResponseMappingConfig;
import com.fb.cli.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProxyResponseMapper {

    private final ObjectMapper objectMapper;

    /**
     * Map raw JSON response string to standardized ProxyInfo according to ResponseMappingConfig.
     */
    public ProxyInfo mapToProxyInfo(String rawJsonResponse, ResponseMappingConfig config) {
        try {
            JsonNode rootNode = objectMapper.readTree(rawJsonResponse);
            return mapToProxyInfo(rootNode, config);
        } catch (Exception e) {
            log.error("Failed to parse raw JSON response into ProxyInfo: {}", rawJsonResponse, e);
            throw new IllegalArgumentException("Cannot parse provider response to JSON", e);
        }
    }

    /**
     * Map JsonNode to standardized ProxyInfo according to ResponseMappingConfig.
     */
    public ProxyInfo mapToProxyInfo(JsonNode rootNode, ResponseMappingConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("ResponseMappingConfig must not be null");
        }

        // 1. Resolve root node if rootPath is specified (e.g. "data" or "data.proxy_info")
        JsonNode targetNode = rootNode;
        if (config.rootPath() != null && !config.rootPath().isBlank()) {
            targetNode = resolvePath(rootNode, config.rootPath());
            if (targetNode == null || targetNode.isMissingNode() || targetNode.isNull()) {
                throw new IllegalArgumentException("Root path '" + config.rootPath() + "' not found in response");
            }
        }

        // 2. Handle raw_proxy string (e.g. "1.2.3.4:8080:user:pass") if configured
        if (config.rawProxy() != null && !config.rawProxy().isBlank()) {
            JsonNode rawProxyNode = resolvePath(targetNode, config.rawProxy());
            if (rawProxyNode != null && !rawProxyNode.isMissingNode() && !rawProxyNode.isNull()) {
                String rawProxy = rawProxyNode.asText();
                Long expireAt = extractLong(targetNode, config.expireAt());
                return parseRawProxyString(rawProxy, config.defaultProtocol(), expireAt);
            }
        }

        // 3. Field-by-field mapping
        String host = extractString(targetNode, config.host());
        Integer port = extractInteger(targetNode, config.port());
        String username = extractString(targetNode, config.username());
        String password = extractString(targetNode, config.password());
        String protocol = extractString(targetNode, config.protocol());
        if (protocol == null || protocol.isBlank()) {
            protocol = config.defaultProtocol() != null ? config.defaultProtocol() : "http";
        }
        Long expireAt = extractLong(targetNode, config.expireAt());

        return new ProxyInfo(host, port, username, password, protocol, expireAt);
    }

    /**
     * Parses format like: "host:port", "host:port:user:pass", or "user:pass@host:port"
     */
    private ProxyInfo parseRawProxyString(String rawProxy, String defaultProtocol, Long expireAt) {
        String protocol = defaultProtocol != null ? defaultProtocol : "http";
        String host = null;
        Integer port = null;
        String username = null;
        String password = null;

        // Check if rawProxy contains '@' -> user:pass@host:port
        if (rawProxy.contains("@")) {
            String[] parts = rawProxy.split("@", 2);
            String[] auth = parts[0].split(":", 2);
            username = auth[0];
            if (auth.length > 1) password = auth[1];

            String[] hostPort = parts[1].split(":", 2);
            host = hostPort[0];
            if (hostPort.length > 1) port = Integer.parseInt(hostPort[1]);
        } else {
            // Split by ":" -> host:port or host:port:user:pass
            String[] parts = rawProxy.split(":");
            if (parts.length >= 2) {
                host = parts[0];
                port = Integer.parseInt(parts[1]);
            }
            if (parts.length >= 4) {
                username = parts[2];
                password = parts[3];
            }
        }

        return new ProxyInfo(host, port, username, password, protocol, expireAt);
    }

    private JsonNode resolvePath(JsonNode node, String path) {
        if (node == null || path == null || path.isBlank()) {
            return node;
        }
        String[] tokens = path.split("\\.");
        JsonNode current = node;
        for (String token : tokens) {
            if (current == null || !current.has(token)) {
                return null;
            }
            current = current.get(token);
        }
        return current;
    }

    private String extractString(JsonNode node, String path) {
        if (node == null || path == null || path.isBlank()) return null;
        JsonNode target = resolvePath(node, path);
        return (target != null && !target.isNull()) ? target.asText() : null;
    }

    private Integer extractInteger(JsonNode node, String path) {
        if (node == null || path == null || path.isBlank()) return null;
        JsonNode target = resolvePath(node, path);
        return (target != null && !target.isNull()) ? target.asInt() : null;
    }

    private Long extractLong(JsonNode node, String path) {
        if (node == null || path == null || path.isBlank()) return null;
        JsonNode target = resolvePath(node, path);
        if (target == null || target.isNull()) return null;
        if (target.isNumber()) {
            return target.asLong();
        }
        try {
            return Long.parseLong(target.asText().trim());
        } catch (NumberFormatException e) {
            try {
                LocalDateTime ldt = JsonUtils.parseDateTime(target.asText().trim());
                if (ldt != null) {
                    return ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                }
            } catch (Exception ignored) {}
        }
        return null;
    }
}
