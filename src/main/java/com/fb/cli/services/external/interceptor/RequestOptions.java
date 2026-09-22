package com.fb.cli.services.external.interceptor;

import com.fb.cli.dtos.proxy.ProxyInfo;
import com.fb.cli.dtos.proxy.RandomSamplingCfg;
import com.fb.cli.entities.Bot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestOptions {

    /**
     * Cấu hình Sampler để ProxyManager tự động kéo Proxy phù hợp từ DB
     */
    private RandomSamplingCfg sampler;

    /**
     * Thông tin Proxy sau khi được resolve hoặc gán thủ công
     */
    private ProxyInfo proxy;

    /**
     * Thực thể Bot sau khi được gán vào request
     */
    private Bot bot;

    /**
     * Các header tùy chỉnh bổ sung
     */
    @Builder.Default
    private Map<String, String> headers = new HashMap<>();

    /**
     * Timeout tùy chỉnh riêng cho request này (ms)
     */
    private Long timeoutMs;

    public static RequestOptions fromSampler(RandomSamplingCfg sampler) {
        return RequestOptions.builder()
                .sampler(sampler)
                .build();
    }

    public static RequestOptions fromProxy(ProxyInfo proxy) {
        return RequestOptions.builder()
                .proxy(proxy)
                .build();
    }

    public static RequestOptions fromBot(Bot bot) {
        return RequestOptions.builder()
                .bot(bot)
                .build();
    }

    public RequestOptions addHeader(String key, String value) {
        if (this.headers == null) {
            this.headers = new HashMap<>();
        }
        this.headers.put(key, value);
        return this;
    }
}

