package com.fb.cli.services.external.proxy;

import com.fb.cli.dtos.proxy.EndpointConfig;
import com.fb.cli.dtos.proxy.EndpointConstant;
import com.fb.cli.dtos.proxy.ProviderConfig;
import com.fb.cli.dtos.proxy.ProxyInfo;
import com.fb.cli.entities.ProxyProvider;
import com.fb.cli.enums.ProviderStrategy;
import com.fb.cli.repositories.ProxyProviderRepository;
import com.fb.cli.services.external.SendRequest;
import com.fb.cli.utils.JsonUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
public class TMProvider implements ProxyProviderStrategy {

    private static final String DEFAULT_BASE_URL = "https://tmproxy.com/api/proxy";

    private final ProxyProviderRepository proxyProviderRepository;
    private final SendRequest sendRequest;
    private final ProxyResponseMapper proxyResponseMapper;

    @Getter
    @Setter
    private ProxyProvider proxyProvider;

    @Autowired
    public TMProvider(
            ProxyProviderRepository proxyProviderRepository,
            SendRequest sendRequest,
            ProxyResponseMapper proxyResponseMapper
    ) {
        this.proxyProviderRepository = proxyProviderRepository;
        this.sendRequest = sendRequest;
        this.proxyResponseMapper = proxyResponseMapper;
    }

    /**
     * Constructor dùng cho testing hoặc khởi tạo trực tiếp với ProxyProvider entity
     */
    public TMProvider(
            ProxyProvider proxyProvider,
            SendRequest sendRequest,
            ProxyResponseMapper proxyResponseMapper
    ) {
        this.proxyProviderRepository = null;
        this.proxyProvider = proxyProvider;
        this.sendRequest = sendRequest;
        this.proxyResponseMapper = proxyResponseMapper;
    }

    /**
     * Lấy thực thể ProxyProvider hiện tại (ưu tiên provider được gán trực tiếp, hoặc truy vấn từ DB theo strategy)
     */
    public ProxyProvider getProvider() {
        if (this.proxyProvider != null) {
            return this.proxyProvider;
        }
        if (this.proxyProviderRepository != null) {
            return this.proxyProviderRepository.findByStrategy(ProviderStrategy.TM_Strategy)
                    .orElseThrow(() -> new IllegalStateException("ProxyProvider not found in database for strategy: " + ProviderStrategy.TM_Strategy));
        }
        throw new IllegalStateException("ProxyProvider is not configured or initialized");
    }

    @Override
    public ProxyInfo getNewProxy(String apiKey) {
        return getNewProxy(apiKey, 0, 0, true);
    }

    public ProxyInfo getNewProxy(String apiKey, int idLocation, int idIsp, boolean fallbackCurrent) {
        ProxyProvider provider = getProvider();
        EndpointConfig endpoint = resolveEndpoint(provider, EndpointConstant.GET_NEW_PROXY, "/get-new-proxy", "POST");

        Map<String, Object> payload = Map.of(
                "api_key", apiKey,
                "id_location", idLocation,
                "id_isp", idIsp
        );

        String response = executeRequest(provider, endpoint, "/get-new-proxy", "POST", payload);
        int code = JsonUtils.getCode(response);

        // Kiểm tra mã cooldown động theo cấu hình trong entity provider
        if (provider.isCooldown(code) && fallbackCurrent) {
            String message = JsonUtils.getMessage(response);
            log.warn("Proxy provider returned cooldown code {}: '{}'. Falling back to getCurrentProxy.", code, message);
            return getCurrentProxy(apiKey);
        }

        // Kiểm tra mã thành công động theo cấu hình trong entity provider
        if (!provider.isSuccess(code)) {
            String message = JsonUtils.getMessage(response);
            throw new IllegalStateException("Failed to get new proxy (code=" + code + "): " + message);
        }

        return parseProxyInfo(endpoint, response);
    }

    @Override
    public ProxyInfo getCurrentProxy(String apiKey) {
        ProxyProvider provider = getProvider();
        EndpointConfig endpoint = resolveEndpoint(provider, EndpointConstant.GET_CURRENT_PROXY, "/get-current-proxy", "POST");

        Map<String, Object> payload = Map.of("api_key", apiKey);
        String response = executeRequest(provider, endpoint, "/get-current-proxy", "POST", payload);
        int code = JsonUtils.getCode(response);

        if (!provider.isSuccess(code)) {
            String message = JsonUtils.getMessage(response);
            throw new IllegalStateException("Failed to get current proxy (code=" + code + "): " + message);
        }

        return parseProxyInfo(endpoint, response);
    }

    @Override
    public LocalDateTime keyExpiredTime(String apiKey) {
        ProxyProvider provider = getProvider();
        EndpointConfig endpoint = resolveEndpoint(provider, EndpointConstant.KEY_EXPIRED_TIME, "/stats", "POST");
        if (endpoint == null || endpoint.path() == null) {
            endpoint = resolveEndpoint(provider, EndpointConstant.STATS, "/stats", "POST");
        }

        Map<String, Object> payload = Map.of("api_key", apiKey);
        String response = executeRequest(provider, endpoint, "/stats", "POST", payload);
        int code = JsonUtils.getCode(response);

        if (!provider.isSuccess(code)) {
            String message = JsonUtils.getMessage(response);
            throw new IllegalStateException("Failed to get key expired time (code=" + code + "): " + message);
        }

        // Parse ngày giờ hết hạn theo response mapping nếu được cấu hình
        if (endpoint != null && endpoint.responseMapping() != null && endpoint.responseMapping().expireAt() != null) {
            String expirePath = endpoint.responseMapping().expireAt();
            if (endpoint.responseMapping().rootPath() != null && !endpoint.responseMapping().rootPath().isBlank()) {
                expirePath = endpoint.responseMapping().rootPath() + "." + expirePath;
            }
            String expireStr = JsonUtils.getProperty(response, expirePath);
            if (expireStr != null && !expireStr.isBlank()) {
                return JsonUtils.parseDateTime(expireStr);
            }
        }

        return JsonUtils.parseExpiredTime(response);
    }

    private EndpointConfig resolveEndpoint(ProxyProvider provider, String endpointKey, String defaultPath, String defaultMethod) {
        ProviderConfig config = provider.getParsedConfig();
        if (config != null && config.endpoints() != null && config.endpoints().containsKey(endpointKey)) {
            return config.endpoints().get(endpointKey);
        }
        return EndpointConfig.builder()
                .endpointName(endpointKey)
                .method(defaultMethod)
                .path(defaultPath)
                .build();
    }

    private String buildUrl(ProxyProvider provider, EndpointConfig endpoint, String defaultPath) {
        String baseUrl = provider.getBaseUrl() != null && !provider.getBaseUrl().isBlank()
                ? provider.getBaseUrl()
                : DEFAULT_BASE_URL;

        String path = (endpoint != null && endpoint.path() != null && !endpoint.path().isBlank())
                ? endpoint.path()
                : defaultPath;

        if (baseUrl.endsWith("/") && path.startsWith("/")) {
            return baseUrl + path.substring(1);
        } else if (!baseUrl.endsWith("/") && !path.startsWith("/")) {
            return baseUrl + "/" + path;
        }
        return baseUrl + path;
    }

    private String executeRequest(
            ProxyProvider provider,
            EndpointConfig endpoint,
            String defaultPath,
            String defaultMethod,
            Map<String, Object> payload
    ) {
        String url = buildUrl(provider, endpoint, defaultPath);
        String method = (endpoint != null && endpoint.method() != null)
                ? endpoint.method().toUpperCase()
                : defaultMethod;
        Map<String, String> headers = endpoint != null ? endpoint.headers() : null;

        String jsonBody;
        if (endpoint != null && endpoint.bodyTemplate() != null && !endpoint.bodyTemplate().isBlank()) {
            jsonBody = substituteTemplate(endpoint.bodyTemplate(), payload);
        } else {
            jsonBody = JsonUtils.marshal(payload);
        }

        if ("GET".equalsIgnoreCase(method)) {
            return sendRequest.get(url, headers);
        } else {
            return sendRequest.postJson(url, jsonBody, headers);
        }
    }

    private String substituteTemplate(String template, Map<String, Object> payload) {
        String result = template;
        if (payload != null) {
            for (Map.Entry<String, Object> entry : payload.entrySet()) {
                String placeholder = "{{" + entry.getKey() + "}}";
                result = result.replace(placeholder, String.valueOf(entry.getValue()));
            }
        }
        return result;
    }

    private ProxyInfo parseProxyInfo(EndpointConfig endpoint, String response) {
        if (endpoint != null && endpoint.responseMapping() != null) {
            return proxyResponseMapper.mapToProxyInfo(response, endpoint.responseMapping());
        }
        return JsonUtils.parseProxyInfo(response);
    }

    @Override
    public ProviderStrategy getStrategy() {
        return ProviderStrategy.TM_Strategy;
    }
}
