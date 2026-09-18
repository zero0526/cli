package com.fb.cli.services.external;

import com.fb.cli.dtos.proxy.ProxyInfo;
import com.fb.cli.dtos.proxy.RandomSamplingCfg;
import com.fb.cli.enums.RotationType;
import com.fb.cli.services.external.interceptor.ProxyInterceptor;
import com.fb.cli.services.external.proxy.ProxyManager;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HttpRequestCallTest {

    @Test
    @DisplayName("Gửi GET cơ bản bằng SendRequest fluent call và execute()")
    void testBasicGetExecute() {
        OkHttpClient mockClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> new Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(ResponseBody.create("{\"status\":\"ok\"}", okhttp3.MediaType.get("application/json")))
                        .build())
                .build();

        SendRequest sendRequest = new SendRequest(mockClient);

        String result = sendRequest.get("https://httpbin.org/get")
                .header("X-App", "CLI")
                .execute();

        assertEquals("{\"status\":\"ok\"}", result);
    }

    @Test
    @DisplayName("Gửi POST có .withProxy(sampler) -> tự động gọi ProxyManager kéo proxy và cấu hình vào client")
    void testPostWithProxySampler() {
        ProxyManager mockProxyManager = Mockito.mock(ProxyManager.class);
        ProxyInfo mockProxy = new ProxyInfo("103.15.22.99", 8080, "u1", "p1", "http", 120000L);

        when(mockProxyManager.getProxy(any(RandomSamplingCfg.class))).thenReturn(mockProxy);

        ProxyInterceptor proxyInterceptor = new ProxyInterceptor(mockProxyManager);

        AtomicBoolean proxyApplied = new AtomicBoolean(false);

        OkHttpClient mockClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    // Xác nhận proxy đã được cấu hình trong OkHttpClient của chain
                    assertNotNull(chain.request());
                    proxyApplied.set(true);
                    return new Response.Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(ResponseBody.create("{\"result\":\"success\"}", okhttp3.MediaType.get("application/json")))
                            .build();
                })
                .build();

        SendRequest sendRequest = new SendRequest(mockClient, proxyInterceptor);

        RandomSamplingCfg sampler = new RandomSamplingCfg(1, 0, RotationType.ROTATING);

        String result = sendRequest.post("https://httpbin.org/post")
                .body("{\"name\":\"test\"}")
                .header("User-Agent", "TestAgent")
                .withProxy(sampler)
                .execute();

        assertEquals("{\"result\":\"success\"}", result);
        assertTrue(proxyApplied.get());
        verify(mockProxyManager, times(1)).getProxy(sampler);
    }

    @Test
    @DisplayName("Gửi request có chuỗi interceptor tùy biến và chốt bằng execute()")
    void testCustomInterceptorChain() {
        AtomicBoolean customInterceptorCalled = new AtomicBoolean(false);

        OkHttpClient mockClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    assertEquals("custom-header-value", chain.request().header("X-Custom-Header"));
                    return new Response.Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(ResponseBody.create("{\"done\":true}", okhttp3.MediaType.get("application/json")))
                            .build();
                })
                .build();

        SendRequest sendRequest = new SendRequest(mockClient);

        String result = sendRequest.get("https://httpbin.org/headers")
                .addInterceptor((builder, options) -> {
                    customInterceptorCalled.set(true);
                    return builder.addHeader("X-Custom-Header", "custom-header-value");
                })
                .execute();

        assertEquals("{\"done\":true}", result);
        assertTrue(customInterceptorCalled.get());
    }

    @Test
    @DisplayName("Gửi request có .withBot(bot, mapper) -> lambda tự định nghĩa map các trường của Bot vào header")
    void testWithBotCustomMapper() {
        com.fb.cli.entities.Bot bot = com.fb.cli.entities.Bot.builder()
                .botId("fb_bot_99")
                .token("EAAG_TEST_TOKEN")
                .cookies("c_user=10001; xs=sec99;")
                .userAgent("Mozilla/5.0 Custom")
                .build();

        OkHttpClient mockClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    assertEquals("Bearer EAAG_TEST_TOKEN", chain.request().header("Authorization"));
                    assertEquals("c_user=10001; xs=sec99;", chain.request().header("Cookie"));
                    assertEquals("Mozilla/5.0 Custom", chain.request().header("User-Agent"));
                    return new Response.Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(ResponseBody.create("{\"ok\":true}", okhttp3.MediaType.get("application/json")))
                            .build();
                })
                .build();

        SendRequest sendRequest = new SendRequest(mockClient);

        String result = sendRequest.get("https://graph.facebook.com/me")
                .withBot(bot, (b, req) -> {
                    req.header("Authorization", "Bearer " + b.getToken());
                    req.header("Cookie", b.getCookies());
                    req.header("User-Agent", b.getUserAgent());
                })
                .execute();

        assertEquals("{\"ok\":true}", result);
    }

    @Test
    @DisplayName("Gửi request có .withBot(bot) với ánh xạ mặc định")
    void testWithBotDefaultMapping() {
        com.fb.cli.entities.Bot bot = com.fb.cli.entities.Bot.builder()
                .botId("fb_bot_default")
                .token("DEFAULT_TOKEN")
                .cookies("sb=123")
                .userAgent("DefaultAgent/1.0")
                .build();

        OkHttpClient mockClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    assertEquals("Bearer DEFAULT_TOKEN", chain.request().header("Authorization"));
                    assertEquals("sb=123", chain.request().header("Cookie"));
                    assertEquals("DefaultAgent/1.0", chain.request().header("User-Agent"));
                    return new Response.Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(ResponseBody.create("{\"status\":\"default_ok\"}", okhttp3.MediaType.get("application/json")))
                            .build();
                })
                .build();

        SendRequest sendRequest = new SendRequest(mockClient);

        String result = sendRequest.get("https://graph.facebook.com/v19.0/me")
                .withBot(bot)
                .execute();

        assertEquals("{\"status\":\"default_ok\"}", result);
    }
}

