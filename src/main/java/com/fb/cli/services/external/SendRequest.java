package com.fb.cli.services.external;

import com.fb.cli.services.external.interceptor.ProxyInterceptor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Component
public class SendRequest {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient okHttpClient;
    private final ObjectProvider<ProxyInterceptor> proxyInterceptorProvider;
    private ProxyInterceptor directProxyInterceptor;

    @Autowired
    public SendRequest(
            OkHttpClient okHttpClient,
            @Lazy ObjectProvider<ProxyInterceptor> proxyInterceptorProvider
    ) {
        this.okHttpClient = okHttpClient;
        this.proxyInterceptorProvider = proxyInterceptorProvider;
    }

    /**
     * Constructor dùng cho testing hoặc khởi tạo thủ công
     */
    public SendRequest(OkHttpClient okHttpClient, ProxyInterceptor proxyInterceptor) {
        this.okHttpClient = okHttpClient;
        this.proxyInterceptorProvider = null;
        this.directProxyInterceptor = proxyInterceptor;
    }

    public SendRequest(OkHttpClient okHttpClient) {
        this(okHttpClient, (ProxyInterceptor) null);
    }

    private ProxyInterceptor getProxyInterceptor() {
        if (this.directProxyInterceptor != null) {
            return this.directProxyInterceptor;
        }
        if (this.proxyInterceptorProvider != null) {
            return this.proxyInterceptorProvider.getIfAvailable();
        }
        return null;
    }

    // ==========================================
    // FLUENT CALL BUILDER API (.withProxy().withBot().execute())
    // ==========================================

    /**
     * Khởi tạo một HTTP GET request dạng Fluent call
     */
    public HttpRequestCall get(String url) {
        return new HttpRequestCall(okHttpClient, getProxyInterceptor(), "GET", url);
    }

    /**
     * Khởi tạo một HTTP POST request dạng Fluent call
     */
    public HttpRequestCall post(String url) {
        return new HttpRequestCall(okHttpClient, getProxyInterceptor(), "POST", url);
    }

    public HttpRequestCall post(String url, String jsonBody) {
        return post(url).body(jsonBody);
    }

    /**
     * Khởi tạo một HTTP request tùy biến method
     */
    public HttpRequestCall request(String method, String url) {
        return new HttpRequestCall(okHttpClient, getProxyInterceptor(), method, url);
    }

    // ==========================================
    // CONVENIENCE METHODS
    // ==========================================

    /**
     * Gửi HTTP POST request với body là chuỗi JSON
     */
    public String postJson(String url, String jsonBody, Map<String, String> headers) {
        return post(url, jsonBody).headers(headers).execute();
    }

    public String postJson(String url, String jsonBody) {
        return postJson(url, jsonBody, Collections.emptyMap());
    }

    /**
     * Gửi HTTP GET request
     */
    public String get(String url, Map<String, String> headers) {
        return get(url).headers(headers).execute();
    }
}

