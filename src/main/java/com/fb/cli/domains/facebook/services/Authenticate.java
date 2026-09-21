package com.fb.cli.domains.facebook.services;

import com.fb.cli.dtos.proxy.ProxyInfo;
import com.fb.cli.dtos.proxy.RandomSamplingCfg;
import com.fb.cli.services.external.HttpRequestCall;
import com.fb.cli.services.external.SendRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class Authenticate {

    private static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36";
    private static final Pattern EAAG_TOKEN_PATTERN = Pattern.compile("EAAG[a-zA-Z0-9]+");

    private static final List<String> BUSINESS_URLS = List.of(
            "https://business.facebook.com/content_management",
            "https://business.facebook.com/business_locations"
    );

    private final SendRequest sendRequest;

    /**
     * Chuyển đổi cookie sang token Business Manager (EAAG...) không dùng proxy
     */
    public String convertCookieToBMToken(String cookies) {
        return convertCookieToBMToken(cookies, (ProxyInfo) null, null);
    }

    /**
     * Chuyển đổi cookie sang token Business Manager (EAAG...) với ProxyInfo
     */
    public String convertCookieToBMToken(String cookies, ProxyInfo proxy) {
        return convertCookieToBMToken(cookies, proxy, null);
    }

    /**
     * Chuyển đổi cookie sang token Business Manager (EAAG...) với Proxy Sampler
     */
    public String convertCookieToBMToken(String cookies, RandomSamplingCfg proxySampler) {
        return convertCookieToBMToken(cookies, null, proxySampler);
    }

    /**
     * Gửi request đến business.facebook.com kèm Cookie để trích xuất token EAAG
     */
    public String convertCookieToBMToken(String cookies, ProxyInfo proxy, RandomSamplingCfg proxySampler) {
        if (cookies == null || cookies.isBlank()) {
            log.warn("[convertCookieToBMToken] Cookies không được để trống");
            return null;
        }

        String cleanCookies = cookies.trim();

        for (String url : BUSINESS_URLS) {
            try {
                HttpRequestCall call = sendRequest.get(url)
                        .header("User-Agent", DEFAULT_USER_AGENT)
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                        .header("Accept-Language", "en-US,en;q=0.9")
                        .header("sec-fetch-dest", "document")
                        .header("sec-fetch-mode", "navigate")
                        .header("sec-fetch-site", "none")
                        .header("sec-fetch-user", "?1")
                        .header("upgrade-insecure-requests", "1")
                        .header("Cookie", cleanCookies);

                if (proxy != null) {
                    call.withProxy(proxy);
                } else if (proxySampler != null) {
                    call.withProxy(proxySampler);
                }

                String html = call.execute();
                if (html != null && !html.isBlank()) {
                    String token = extractTokenBM(html);
                    if (token != null && !token.isBlank()) {
                        log.info("[convertCookieToBMToken] Trích xuất thành công token BM từ URL: {}", url);
                        return token;
                    }
                }
            } catch (Exception e) {
                log.warn("[convertCookieToBMToken] Thử URL {} không thành công: {}", url, e.getMessage());
            }
        }

        log.error("[convertCookieToBMToken] Không tìm thấy EAAG token từ cookie cung cấp");
        return null;
    }

    /**
     * Trích xuất token EAAG từ chuỗi HTML
     */
    public String extractTokenBM(String html) {
        if (html == null || html.isBlank()) {
            return null;
        }

        // 1. Áp dụng logic theo code JS: tìm 'EAAG' và điểm dừng '","'
        int start = html.indexOf("EAAG");
        if (start > -1) {
            int end = html.indexOf("\",\"", start);
            if (end > start) {
                String token = html.substring(start, end).trim();
                if (token.length() >= 25 && token.matches("EAAG[a-zA-Z0-9]+")) {
                    return token;
                }
            }
        }

        // 2. Regex fallback nếu định dạng kết thúc khác (ví dụ: dấu ngoặc kép thông thường '"' hoặc JSON escaped)
        Matcher matcher = EAAG_TOKEN_PATTERN.matcher(html);
        while (matcher.find()) {
            String token = matcher.group();
            if (token.length() >= 25) {
                return token;
            }
        }

        return null;
    }
}
