package com.fb.cli.services.external;

import com.fb.cli.dtos.proxy.ProxyInfo;
import com.fb.cli.dtos.proxy.RandomSamplingCfg;
import com.fb.cli.entities.Bot;
import com.fb.cli.services.external.interceptor.ProxyInterceptor;
import com.fb.cli.services.external.interceptor.RequestInterceptor;
import com.fb.cli.services.external.interceptor.RequestOptions;
import com.fb.cli.services.internal.BotService;
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

    private String url;
    private String method = "GET";
    private String body;
    private byte[] rawBody;
    private String rawBodyContentType;
    private final Map<String, String> formData = new LinkedHashMap<>();
    private final Map<String, String> headers = new HashMap<>();
    private final RequestOptions options = new RequestOptions();
    private final List<RequestInterceptor> customInterceptors = new ArrayList<>();
    private MultipartBody.Builder multipartBuilder;

    public HttpRequestCall(OkHttpClient okHttpClient, ProxyInterceptor proxyInterceptor, String method, String url) {
        this.okHttpClient = okHttpClient;
        this.proxyInterceptor = proxyInterceptor;
        this.method = method;
        this.url = url;
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

    public HttpRequestCall rawBody(byte[] bytes, String contentType) {
        this.rawBody = bytes;
        this.rawBodyContentType = contentType;
        return this;
    }

    public HttpRequestCall rawBody(byte[] bytes) {
        return rawBody(bytes, null);
    }

    public HttpRequestCall formData(String key, String value) {
        if (key != null && value != null) {
            this.formData.put(key, value);
        }
        return this;
    }

    public HttpRequestCall formBody(Map<String, String> data) {
        if (data != null) {
            this.formData.putAll(data);
        }
        return this;
    }

    public HttpRequestCall multipart() {
        if (this.multipartBuilder == null) {
            this.multipartBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        }
        return this;
    }

    public HttpRequestCall addFormDataPart(String name, String value) {
        multipart();
        if (name != null && value != null) {
            this.multipartBuilder.addFormDataPart(name, value);
        }
        return this;
    }

    public HttpRequestCall addFormDataPart(String name, String filename, byte[] bytes, String mediaType) {
        multipart();
        MediaType mt = mediaType != null ? MediaType.parse(mediaType) : MediaType.parse("application/octet-stream");
        RequestBody fileBody = RequestBody.create(bytes != null ? bytes : new byte[0], mt);
        this.multipartBuilder.addFormDataPart(name, filename != null ? filename : "file", fileBody);
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
     * Gắn trực tiếp thực thể Bot và thực thi lambda ánh xạ các trường vào Request.Builder
     *
     * @param bot    Thực thể Bot
     * @param mapper Lambda function tự định nghĩa cách map các trường (token, cookies, headers,...) vào Request.Builder
     */
    public HttpRequestCall withBot(Bot bot, BiConsumer<Bot, Request.Builder> mapper) {
        if (bot != null) {
            this.options.setBot(bot);
            if (mapper != null) {
                addInterceptor((builder, opts) -> {
                    mapper.accept(bot, builder);
                    return builder;
                });
            } else {
                addInterceptor((builder, opts) -> {
                    applyDefaultBotMapping(bot, builder);
                    return builder;
                });
            }
        }
        return this;
    }

    /**
     * Gắn trực tiếp thực thể Bot với ánh xạ mặc định (token -> Authorization, cookies -> Cookie, userAgent -> User-Agent)
     */
    public HttpRequestCall withBot(Bot bot) {
        return withBot(bot, null);
    }

    /**
     * Lấy Bot từ BotSerivce theo platform và thực thi lambda ánh xạ
     */
    public HttpRequestCall withBot(BotService sampler, String platform, BiConsumer<Bot, Request.Builder> mapper) {
        if (sampler != null) {
            Bot bot = sampler.getBot(platform);
            return withBot(bot, mapper);
        }
        return this;
    }

    /**
     * Lấy Bot từ BotSerivce theo platform với ánh xạ mặc định
     */
    public HttpRequestCall withBot(BotService sampler, String platform) {
        return withBot(sampler, platform, null);
    }

    private void applyDefaultBotMapping(Bot bot, Request.Builder builder) {
        if (bot.getToken() != null && !bot.getToken().isBlank()) {
            builder.addHeader("Authorization", "Bearer " + bot.getToken());
        }
        if (bot.getCookies() != null && !bot.getCookies().isBlank()) {
            builder.addHeader("Cookie", bot.getCookies());
        }
        if (bot.getUserAgent() != null && !bot.getUserAgent().isBlank()) {
            builder.header("User-Agent", bot.getUserAgent());
        } else {
            builder.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36");
        }
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
     * Thực thi request và trả về response dạng String
     */
    public String execute() {
        // 1. Tự động resolve Proxy nếu đã truyền sampler
        if (this.proxyInterceptor != null && this.options.getSampler() != null && this.options.getProxy() == null) {
            this.proxyInterceptor.resolveProxy(this.options);
        }

        // 2. Build Request
        Request.Builder requestBuilder = new Request.Builder().url(this.url);
        RequestBody requestBody = null;

        if (this.multipartBuilder != null) {
            requestBody = this.multipartBuilder.build();
        } else if (!this.formData.isEmpty()) {
            FormBody.Builder formBuilder = new FormBody.Builder();
            this.formData.forEach(formBuilder::add);
            requestBody = formBuilder.build();
        } else if (this.rawBody != null) {
            MediaType mt = this.rawBodyContentType != null ? MediaType.parse(this.rawBodyContentType) : MediaType.parse("application/octet-stream");
            requestBody = RequestBody.create(this.rawBody, mt);
            if (this.rawBodyContentType != null) {
                requestBuilder.header("Content-Type", this.rawBodyContentType);
            }
        } else if (this.body != null) {
            requestBody = RequestBody.create(this.body, JSON_MEDIA_TYPE);
            requestBuilder.addHeader("Content-Type", "application/json");
        }

        requestBuilder.method(this.method, requestBody);
        if (formData.isEmpty() && this.multipartBuilder == null && this.rawBody == null) {
            requestBuilder.addHeader("Accept", "application/json");
        }

        // Gắn các headers từ request
        this.headers.forEach(requestBuilder::addHeader);

        // Gắn các headers từ options nếu có
        if (this.options.getHeaders() != null) {
            this.options.getHeaders().forEach(requestBuilder::addHeader);
        }

        // 3. Tập hợp và sắp xếp chuỗi Interceptors
        List<RequestInterceptor> allInterceptors = new ArrayList<>(this.customInterceptors);
        allInterceptors.sort(Comparator.comparingInt(RequestInterceptor::getOrder));

        for (RequestInterceptor interceptor : allInterceptors) {
            requestBuilder = interceptor.intercept(requestBuilder, this.options);
        }

        Request request = requestBuilder.build();

        // 4. Áp dụng Proxy vào OkHttpClient nếu đã có ProxyInfo
        OkHttpClient clientToUse = this.okHttpClient;
        if (this.proxyInterceptor != null && this.options.getProxy() != null) {
            clientToUse = this.proxyInterceptor.applyProxy(this.okHttpClient, this.options.getProxy());
        }

        // 5. Gửi request qua mạng
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

