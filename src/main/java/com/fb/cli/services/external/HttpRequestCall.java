package com.fb.cli.services.external;

import com.fb.cli.dtos.bot.BotSamplingCfg;
import com.fb.cli.dtos.proxy.ProxyInfo;
import com.fb.cli.dtos.proxy.RandomSamplingCfg;
import com.fb.cli.entities.Bot;
import com.fb.cli.services.external.interceptor.BotInterceptor;
import com.fb.cli.services.external.interceptor.ProxyInterceptor;
import com.fb.cli.services.external.interceptor.RequestInterceptor;
import com.fb.cli.services.external.interceptor.RequestOptions;
import com.fb.cli.services.internal.BotSampleStrategy;
import com.fb.cli.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;

@Slf4j
public class HttpRequestCall {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient okHttpClient;
    private final ProxyInterceptor proxyInterceptor;
    private final BotInterceptor botInterceptor;

    private String url;
    private String method = "GET";
    private String body;
    private final Map<String, String> headers = new HashMap<>();
    private final RequestOptions options = new RequestOptions();
    private final List<RequestInterceptor> customInterceptors = new ArrayList<>();

    public HttpRequestCall(
            OkHttpClient okHttpClient,
            ProxyInterceptor proxyInterceptor,
            BotInterceptor botInterceptor,
            String method,
            String url
    ) {
        this.okHttpClient = okHttpClient;
        this.proxyInterceptor = proxyInterceptor;
        this.botInterceptor = botInterceptor;
        this.method = method;
        this.url = url;
    }

    public HttpRequestCall(OkHttpClient okHttpClient, ProxyInterceptor proxyInterceptor, String method, String url) {
        this(okHttpClient, proxyInterceptor, null, method, url);
    }

    public HttpRequestCall url(String url) {
        this.url = url;
        return this;
    }

    public HttpRequestCall method(String method) {
        this.method = method != null ? method.toUpperCase() : "GET";
        return this;
    }

    public HttpRequestCall body(String body) {
        this.body = body;
        return this;
    }

    public HttpRequestCall jsonBody(Object bodyObj) {
        this.body = JsonUtils.marshal(bodyObj);
        return this;
    }

    public HttpRequestCall header(String name, String value) {
        this.headers.put(name, value);
        return this;
    }

    public HttpRequestCall headers(Map<String, String> headers) {
        if (headers != null) {
            this.headers.putAll(headers);
        }
        return this;
    }

    // ==========================================
    // PROXY FLUENT CHAIN
    // ==========================================

    /**
     * Gắn sampler để ProxyManager tự động kéo proxy thích hợp từ database
     */
    public HttpRequestCall withProxy(RandomSamplingCfg sampler) {
        this.options.setSampler(sampler);
        return this;
    }

    /**
     * Gắn trực tiếp ProxyInfo nếu đã có sẵn
     */
    public HttpRequestCall withProxy(ProxyInfo proxyInfo) {
        this.options.setProxy(proxyInfo);
        return this;
    }

    // ==========================================
    // BOT FLUENT CHAIN (WITH CUSTOM LAMBDA MAPPER)
    // ==========================================

    /**
     * Tự động kéo Bot từ DB theo sampler và thực thi lambda ánh xạ các trường của Bot vào Request
     *
     * @param sampler Cấu hình BotSamplingCfg (sampleSize, seed, platform, status)
     * @param mapper  Lambda function tự định nghĩa cách map các trường (token, cookie, proxy,...) của Bot vào Request.Builder
     */
    public HttpRequestCall withBot(BotSamplingCfg sampler, BiConsumer<Bot, Request.Builder> mapper) {
        this.options.setBotSampler(sampler);
        this.options.setBotMapper(mapper);
        return this;
    }

    /**
     * Tự động kéo Bot theo sampler và sử dụng ánh xạ mặc định (token -> Authorization, cookies -> Cookie, userAgent -> User-Agent)
     */
    public HttpRequestCall withBot(BotSamplingCfg sampler) {
        return withBot(sampler, null);
    }

    /**
     * Tự động kéo Bot theo BotSampleStrategy tùy biến và thực thi lambda ánh xạ
     */
    public HttpRequestCall withBot(BotSampleStrategy sampler, BiConsumer<Bot, Request.Builder> mapper) {
        this.options.setBotSampleStrategy(sampler);
        this.options.setBotMapper(mapper);
        return this;
    }

    public HttpRequestCall withBot(BotSampleStrategy sampler) {
        return withBot(sampler, null);
    }

    /**
     * Gắn trực tiếp một thực thể Bot đã có sẵn và thực thi lambda ánh xạ vào Request
     */
    public HttpRequestCall withBot(Bot bot, BiConsumer<Bot, Request.Builder> mapper) {
        this.options.setBot(bot);
        this.options.setBotMapper(mapper);
        return this;
    }

    public HttpRequestCall withBot(Bot bot) {
        return withBot(bot, null);
    }

    // ==========================================
    // CUSTOM INTERCEPTORS & EXECUTION
    // ==========================================

    /**
     * Thêm interceptor tùy biến vào chuỗi xử lý
     */
    public HttpRequestCall addInterceptor(RequestInterceptor interceptor) {
        if (interceptor != null) {
            this.customInterceptors.add(interceptor);
        }
        return this;
    }

    /**
     * Lấy options hiện tại của request
     */
    public RequestOptions getOptions() {
        return this.options;
    }

    /**
     * Thực thi toàn bộ chuỗi interceptor (Proxy, Bot, Custom), cấu hình proxy và gửi HTTP request
     *
     * @return Response body dạng chuỗi
     */
    public String execute() {
        // 1. Tự động resolve Proxy nếu đã truyền sampler
        if (this.proxyInterceptor != null && this.options.getSampler() != null && this.options.getProxy() == null) {
            this.proxyInterceptor.resolveProxy(this.options);
        }

        // 2. Tự động resolve Bot nếu đã truyền bot sampler
        if (this.botInterceptor != null && this.options.getBot() == null) {
            this.botInterceptor.resolveBot(this.options);
        }

        // 3. Khởi tạo OkHttp Request Builder
        Request.Builder requestBuilder = new Request.Builder().url(this.url);

        // Cấu hình Body theo Method
        RequestBody requestBody = null;
        if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method)) {
            requestBody = RequestBody.create(this.body != null ? this.body : "{}", JSON_MEDIA_TYPE);
            requestBuilder.addHeader("Content-Type", "application/json");
        }
        requestBuilder.method(this.method, requestBody);
        requestBuilder.addHeader("Accept", "application/json");

        // Gắn các headers từ request
        this.headers.forEach(requestBuilder::addHeader);

        // Gắn các headers từ options nếu có
        if (this.options.getHeaders() != null) {
            this.options.getHeaders().forEach(requestBuilder::addHeader);
        }

        // 4. Tập hợp và sắp xếp chuỗi Interceptors
        List<RequestInterceptor> allInterceptors = new ArrayList<>(this.customInterceptors);
        if (this.botInterceptor != null) {
            allInterceptors.add(this.botInterceptor);
        }
        allInterceptors.sort(Comparator.comparingInt(RequestInterceptor::getOrder));

        for (RequestInterceptor interceptor : allInterceptors) {
            requestBuilder = interceptor.intercept(requestBuilder, this.options);
        }

        Request request = requestBuilder.build();

        // 5. Áp dụng Proxy vào OkHttpClient nếu đã có ProxyInfo
        OkHttpClient clientToUse = this.okHttpClient;
        if (this.proxyInterceptor != null && this.options.getProxy() != null) {
            clientToUse = this.proxyInterceptor.applyProxy(this.okHttpClient, this.options.getProxy());
        }

        // 6. Gửi request qua mạng
        try (Response response = clientToUse.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IllegalStateException("HTTP request failed with code " + response.code() + " for URL: " + url);
            }
            return response.body() != null ? response.body().string() : "";
        } catch (IOException e) {
            log.error("Network error when calling URL {}: {}", url, e.getMessage(), e);
            throw new RuntimeException("Network error requesting URL " + url + ": " + e.getMessage(), e);
        }
    }

    /**
     * Thực thi request và parse kết quả JSON trực tiếp thành đối tượng Java
     */
    public <T> T execute(Class<T> clazz) {
        String json = execute();
        return JsonUtils.unmarshal(json, clazz);
    }
}
