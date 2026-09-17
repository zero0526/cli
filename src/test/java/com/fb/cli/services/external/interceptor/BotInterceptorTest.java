package com.fb.cli.services.external.interceptor;

import com.fb.cli.dtos.bot.BotSamplingCfg;
import com.fb.cli.entities.Bot;
import com.fb.cli.enums.BotStatus;
import com.fb.cli.services.external.SendRequest;
import com.fb.cli.services.internal.BotManager;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BotInterceptorTest {

    @Test
    @DisplayName("Gửi request có .withBot(sampler, lambda) -> tự động kéo Bot và thực thi lambda function ánh xạ vào Request")
    void testWithBotAndCustomLambdaMapper() {
        BotManager mockBotManager = Mockito.mock(BotManager.class);
        Bot sampleBot = Bot.builder()
                .id(UUID.randomUUID())
                .botId("bot_fb_999")
                .platform("facebook")
                .token("EAAB_test_token_123")
                .cookies("c_user=1000999; xs=sample_xs;")
                .userAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 16_0)")
                .status(BotStatus.ACTIVE)
                .build();

        when(mockBotManager.getBot(any(BotSamplingCfg.class))).thenReturn(sampleBot);

        BotInterceptor botInterceptor = new BotInterceptor(mockBotManager);
        AtomicBoolean lambdaExecuted = new AtomicBoolean(false);

        OkHttpClient mockClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    // Kiểm tra các header do lambda ánh xạ vào
                    assertEquals("Bearer EAAB_test_token_123", chain.request().header("Authorization"));
                    assertEquals("c_user=1000999; xs=sample_xs;", chain.request().header("Cookie"));
                    assertEquals("Mozilla/5.0 (iPhone; CPU iPhone OS 16_0)", chain.request().header("User-Agent"));
                    assertEquals("bot_fb_999", chain.request().header("X-FB-Bot-ID"));
                    lambdaExecuted.set(true);

                    return new Response.Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(ResponseBody.create("{\"status\":\"success\"}", okhttp3.MediaType.get("application/json")))
                            .build();
                })
                .build();

        SendRequest sendRequest = new SendRequest(mockClient, null, botInterceptor);

        BotSamplingCfg botSampler = new BotSamplingCfg(1, 42L, "facebook");

        String result = sendRequest.post("https://graph.facebook.com/me/feed")
                .body("{\"message\":\"Hello world\"}")
                // Truyền sampler và lambda function tự định nghĩa cách map các trường của bot vào request
                .withBot(botSampler, (bot, builder) -> {
                    builder.header("Authorization", "Bearer " + bot.getToken());
                    builder.header("Cookie", bot.getCookies());
                    builder.header("User-Agent", bot.getUserAgent());
                    builder.header("X-FB-Bot-ID", bot.getBotId());
                })
                .execute();

        assertEquals("{\"status\":\"success\"}", result);
        assertTrue(lambdaExecuted.get());
        verify(mockBotManager, times(1)).getBot(botSampler);
    }

    @Test
    @DisplayName("Gửi request có .withBot(sampler) không truyền lambda -> dùng default mapping")
    void testWithBotDefaultMapping() {
        BotManager mockBotManager = Mockito.mock(BotManager.class);
        Bot sampleBot = Bot.builder()
                .id(UUID.randomUUID())
                .botId("bot_default_1")
                .token("sample_token")
                .cookies("session=abc")
                .userAgent("DefaultAgent")
                .status(BotStatus.ACTIVE)
                .build();

        when(mockBotManager.getBot(any(BotSamplingCfg.class))).thenReturn(sampleBot);

        BotInterceptor botInterceptor = new BotInterceptor(mockBotManager);

        OkHttpClient mockClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    assertEquals("Bearer sample_token", chain.request().header("Authorization"));
                    assertEquals("session=abc", chain.request().header("Cookie"));
                    assertEquals("DefaultAgent", chain.request().header("User-Agent"));
                    return new Response.Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(ResponseBody.create("{\"ok\":true}", okhttp3.MediaType.get("application/json")))
                            .build();
                })
                .build();

        SendRequest sendRequest = new SendRequest(mockClient, null, botInterceptor);

        String result = sendRequest.get("https://api.example.com/status")
                .withBot(new BotSamplingCfg(1, 0L))
                .execute();

        assertEquals("{\"ok\":true}", result);
    }

    @Test
    @DisplayName("Gửi request kết hợp cả withProxy và withBot cùng lúc")
    void testCombineProxyAndBot() {
        Bot sampleBot = Bot.builder()
                .botId("bot_combo")
                .token("tok_combo")
                .build();

        BotInterceptor botInterceptor = new BotInterceptor((BotManager) null);

        OkHttpClient mockClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    assertEquals("tok_combo", chain.request().header("X-Auth-Token"));
                    return new Response.Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(ResponseBody.create("{\"combo\":true}", okhttp3.MediaType.get("application/json")))
                            .build();
                })
                .build();

        SendRequest sendRequest = new SendRequest(mockClient, null, botInterceptor);

        String result = sendRequest.get("https://api.example.com/combo")
                .withBot(sampleBot, (bot, req) -> req.header("X-Auth-Token", bot.getToken()))
                .execute();

        assertEquals("{\"combo\":true}", result);
    }
}
