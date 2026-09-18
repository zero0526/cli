package com.fb.cli.domains.facebook.services;

import com.fb.cli.dtos.facebook.FacebookWebContext;
import com.fb.cli.entities.Bot;
import com.fb.cli.persistences.redis.RedisService;
import com.fb.cli.services.external.HttpRequestCall;
import com.fb.cli.services.external.SendRequest;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FacebookWebContextServiceTest {

    @Test
    @DisplayName("resolveContext lấy dữ liệu từ Redis cache khi đã có")
    void testResolveContextFromCache() {
        RedisService mockRedis = Mockito.mock(RedisService.class);
        SendRequest mockSendRequest = Mockito.mock(SendRequest.class);

        FacebookWebContext cachedCtx = FacebookWebContext.builder()
                .userId("100012345")
                .jazoest("2958")
                .lsdToken("LSD_CACHED")
                .dtsgToken("DTSG_CACHED")
                .build();

        when(mockRedis.get("fb:web_context:100012345", FacebookWebContext.class)).thenReturn(cachedCtx);

        FacebookWebContextService service = new FacebookWebContextService(mockSendRequest, mockRedis);

        Bot bot = Bot.builder()
                .botId("100012345")
                .cookies("c_user=100012345; xs=test;")
                .build();

        FacebookWebContext result = service.resolveContext(bot, null, null);

        assertNotNull(result);
        assertEquals("100012345", result.getUserId());
        assertEquals("LSD_CACHED", result.getLsdToken());
        assertEquals("DTSG_CACHED", result.getDtsgToken());
        verify(mockSendRequest, never()).get(anyString());
    }

    @Test
    @DisplayName("resolveContext gọi facebook.com bóc tách tokens khi cache miss và lưu vào Redis")
    void testResolveContextCacheMissAndExtract() {
        RedisService mockRedis = Mockito.mock(RedisService.class);
        when(mockRedis.get(anyString(), eq(FacebookWebContext.class))).thenReturn(null);

        String sampleHtml = "<html><head>" +
                "<script>something jazoest=21094 extra</script>" +
                "<script>[\"LSD\",[],{\"token\":\"LSD_MOCK_XYZ\"},323]</script>" +
                "<script>\"DTSGInitialData\",[],{\"token\":\"NAcA_MOCK_DTSG\"}</script>" +
                "</head></html>";

        OkHttpClient mockClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> new Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(ResponseBody.create(sampleHtml, okhttp3.MediaType.get("text/html")))
                        .build())
                .build();

        SendRequest sendRequest = new SendRequest(mockClient);
        FacebookWebContextService service = new FacebookWebContextService(sendRequest, mockRedis);

        Bot bot = Bot.builder()
                .botId("bot_1")
                .cookies("sb=1; c_user=100098765; xs=2;")
                .userAgent("Mozilla/5.0")
                .build();

        FacebookWebContext result = service.resolveContext(bot, null, null);

        assertNotNull(result);
        assertEquals("100098765", result.getUserId());
        assertEquals("21094", result.getJazoest());
        assertEquals("LSD_MOCK_XYZ", result.getLsdToken());
        assertEquals("NAcA_MOCK_DTSG", result.getDtsgToken());

        verify(mockRedis, times(1)).set(eq("fb:web_context:100098765"), any(FacebookWebContext.class), eq(1L), eq(TimeUnit.HOURS));
    }

    @Test
    @DisplayName("extractUserId trả về null nếu không có c_user và botId không phải là số")
    void testExtractUserIdInvalid() {
        RedisService mockRedis = Mockito.mock(RedisService.class);
        SendRequest mockSendRequest = Mockito.mock(SendRequest.class);

        FacebookWebContextService service = new FacebookWebContextService(mockSendRequest, mockRedis);

        Bot bot = Bot.builder()
                .botId("not_a_number_id")
                .cookies("sb=1; xs=2;")
                .build();

        assertNull(service.extractUserId(bot));
        assertNull(service.resolveContext(bot, null, null));
    }
}
