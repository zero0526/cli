package com.fb.cli.services.external.interceptor;

import com.fb.cli.dtos.bot.BotSamplingCfg;
import com.fb.cli.dtos.proxy.ProxyInfo;
import com.fb.cli.dtos.proxy.RandomSamplingCfg;
import com.fb.cli.entities.Bot;
import com.fb.cli.services.internal.BotSampleStrategy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import okhttp3.Request;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

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
     * Cấu hình Bot Sampler (dạng BotSamplingCfg) để kéo Bot từ DB
     */
    private BotSamplingCfg botSampler;

    /**
     * Strategy Sampler tùy biến để kéo Bot
     */
    private BotSampleStrategy botSampleStrategy;

    /**
     * Thực thể Bot sau khi được kéo lên hoặc truyền trực tiếp
     */
    private Bot bot;

    /**
     * Lambda function tự định nghĩa để ánh xạ linh hoạt các trường của Bot vào Request
     */
    private BiConsumer<Bot, Request.Builder> botMapper;

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

    public static RequestOptions fromBot(Bot bot, BiConsumer<Bot, Request.Builder> mapper) {
        return RequestOptions.builder()
                .bot(bot)
                .botMapper(mapper)
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
