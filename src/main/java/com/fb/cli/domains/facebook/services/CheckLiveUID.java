package com.fb.cli.domains.facebook.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fb.cli.dtos.proxy.ProxyInfo;
import com.fb.cli.dtos.proxy.RandomSamplingCfg;
import com.fb.cli.enums.BotStatus;
import com.fb.cli.services.external.HttpRequestCall;
import com.fb.cli.services.external.SendRequest;
import com.fb.cli.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckLiveUID {

    private static final String GRAPH_PICTURE_URL = "https://graph.facebook.com/%s/picture?redirect=false";
    private static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private final SendRequest sendRequest;

    /**
     * Kiểm tra trạng thái Live/Dead của UID Facebook không dùng proxy
     */
    public BotStatus checkLive(String uId) {
        return checkLive(uId, (ProxyInfo) null, null);
    }

    /**
     * Kiểm tra trạng thái Live/Dead của UID Facebook sử dụng ProxyInfo
     */
    public BotStatus checkLive(String uId, ProxyInfo proxy) {
        return checkLive(uId, proxy, null);
    }

    /**
     * Kiểm tra trạng thái Live/Dead của UID Facebook sử dụng Proxy Sampler
     */
    public BotStatus checkLive(String uId, RandomSamplingCfg proxySampler) {
        return checkLive(uId, null, proxySampler);
    }

    /**
     * Phương thức cốt lõi kiểm tra trạng thái Live/Dead của UID Facebook
     */
    public BotStatus checkLive(String uId, ProxyInfo proxy, RandomSamplingCfg proxySampler) {
        if (uId == null || uId.isBlank()) {
            log.warn("[checkLive] UID không hợp lệ (null hoặc rỗng)");
            return BotStatus.ERROR;
        }

        String cleanUId = uId.trim();
        String url = GRAPH_PICTURE_URL.formatted(cleanUId);

        try {
            HttpRequestCall call = sendRequest.get(url)
                    .header("User-Agent", DEFAULT_USER_AGENT)
                    .header("Accept", "application/json,text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");

            if (proxy != null) {
                call.withProxy(proxy);
            } else if (proxySampler != null) {
                call.withProxy(proxySampler);
            }

            String resp = call.execute();
            if (resp == null || resp.isBlank()) {
                log.warn("[checkLive] UID {} nhận phản hồi rỗng từ Facebook Graph API", cleanUId);
                return BotStatus.ERROR;
            }

            JsonNode root = JsonUtils.MAPPER.readTree(resp);

            // Khi UID live, Graph API trả về: {"data": {"height": ..., "width": ..., "url": "..."}}
            if (root.has("data") && (root.path("data").has("url") || root.path("data").has("height"))) {
                log.debug("[checkLive] UID {} is LIVE", cleanUId);
                return BotStatus.LIVE;
            }

            // Khi UID bị vô hiệu hóa/không tồn tại, Graph API trả về: {"error": {"message": "...", "code": 803, ...}}
            if (root.has("error")) {
                log.debug("[checkLive] UID {} is DEAD (Graph error: {})", cleanUId, root.path("error").path("message").asText());
                return BotStatus.DEAD;
            }

            // Fallback phòng trường hợp format khác thường
            if (resp.contains("\"height\"") && resp.contains("\"width\"")) {
                return BotStatus.LIVE;
            }

            return BotStatus.DEAD;

        } catch (Exception e) {
            log.error("[checkLive] Lỗi khi kiểm tra UID {}: {}", cleanUId, e.getMessage(), e);
            return BotStatus.ERROR;
        }
    }
}
