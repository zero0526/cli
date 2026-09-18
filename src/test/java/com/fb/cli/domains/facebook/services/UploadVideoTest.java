package com.fb.cli.domains.facebook.services;

import com.fb.cli.dtos.facebook.FacebookWebContext;
import com.fb.cli.dtos.facebook.UploadVideoResult;
import com.fb.cli.entities.Bot;
import com.fb.cli.services.external.SendRequest;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class UploadVideoTest {

    private FacebookWebContextService mockContextService;
    private Bot sampleBot;
    private FacebookWebContext sampleContext;

    @BeforeEach
    void setUp() {
        mockContextService = Mockito.mock(FacebookWebContextService.class);

        sampleBot = Bot.builder()
                .botId("61586296264033")
                .cookies("c_user=61586296264033; xs=sec88;")
                .userAgent("Mozilla/5.0")
                .build();

        sampleContext = FacebookWebContext.builder()
                .userId("61586296264033")
                .jazoest("25387")
                .lsdToken("LSD_123")
                .dtsgToken("DTSG_ABC")
                .build();
    }

    @Test
    @DisplayName("Upload video thất bại khi Bot null")
    void testUploadVideo_NullBot_Fails() {
        SendRequest sendRequest = new SendRequest(new OkHttpClient());
        UploadVideo uploadVideo = new UploadVideo(sendRequest, mockContextService);

        UploadVideoResult result = uploadVideo.uploadVideo(null, new byte[]{1, 2, 3}, "test.mp4", null, null);

        assertFalse(result.isSuccess());
        assertEquals("BOT_NULL", result.getErrorCode());
    }

    @Test
    @DisplayName("Upload video thất bại khi byte rỗng")
    void testUploadVideo_EmptyBytes_Fails() {
        SendRequest sendRequest = new SendRequest(new OkHttpClient());
        UploadVideo uploadVideo = new UploadVideo(sendRequest, mockContextService);

        UploadVideoResult result = uploadVideo.uploadVideo(sampleBot, new byte[0], "test.mp4", null, null);

        assertFalse(result.isSuccess());
        assertEquals("EMPTY_VIDEO", result.getErrorCode());
    }

    @Test
    @DisplayName("Upload video thất bại khi file không tồn tại")
    void testUploadVideo_FileNotFound_Fails() {
        SendRequest sendRequest = new SendRequest(new OkHttpClient());
        UploadVideo uploadVideo = new UploadVideo(sendRequest, mockContextService);

        UploadVideoResult result = uploadVideo.uploadVideo(sampleBot, new File("non_existent_video.mp4"), null, null);

        assertFalse(result.isSuccess());
        assertEquals("FILE_NOT_FOUND", result.getErrorCode());
    }

    @Test
    @DisplayName("Upload video thất bại khi không lấy được web context")
    void testUploadVideo_AuthFailed_Fails() {
        when(mockContextService.resolveContext(eq(sampleBot), any(), any())).thenReturn(null);

        SendRequest sendRequest = new SendRequest(new OkHttpClient());
        UploadVideo uploadVideo = new UploadVideo(sendRequest, mockContextService);

        UploadVideoResult result = uploadVideo.uploadVideo(sampleBot, new byte[]{1, 2, 3}, "test.mp4", null, null);

        assertFalse(result.isSuccess());
        assertEquals("AUTH_FAILED", result.getErrorCode());
    }

    @Test
    @DisplayName("Upload video thành công trường hợp deduplication (skip_upload=true)")
    void testUploadVideo_SkipUploadDedup_Success() {
        when(mockContextService.resolveContext(eq(sampleBot), any(), any())).thenReturn(sampleContext);

        String configResponse = "{\"data\":{\"media_upload_config\":{\"network_start\":{\"uri\":\"https://www.facebook.com/ajax/video/upload/requests/start/\"},\"network_receive\":{\"uri\":\"https://www.facebook.com/ajax/video/upload/requests/receive/\"},\"network_upload_service\":{\"default\":{\"service_name\":\"rupload\",\"service_domain\":\"facebook.com\"}}}}}";
        String startResponse = "{\"__ar\":1,\"payload\":{\"video_id\":\"1629089142153499\",\"upload_session_id\":\"1629089148820165\",\"skip_upload\":true}}";

        OkHttpClient mockClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    String url = chain.request().url().toString();
                    String body;
                    if (url.contains("graphql")) {
                        body = configResponse;
                    } else if (url.contains("requests/start")) {
                        body = startResponse;
                    } else {
                        body = "{}";
                    }
                    return new Response.Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(ResponseBody.create(body, okhttp3.MediaType.get("application/json")))
                            .build();
                })
                .build();

        SendRequest sendRequest = new SendRequest(mockClient);
        UploadVideo uploadVideo = new UploadVideo(sendRequest, mockContextService);

        UploadVideoResult result = uploadVideo.uploadVideo(sampleBot, new byte[]{1, 2, 3, 4, 5}, "my_video.mp4", null, null);

        assertTrue(result.isSuccess());
        assertEquals("1629089142153499", result.getVideoId());
        assertEquals("1629089148820165", result.getUploadSessionId());
        assertEquals(5L, result.getFileSize());
    }

    @Test
    @DisplayName("Upload video thành công đầy đủ quy trình (config -> start -> probe -> transfer -> receive)")
    void testUploadVideo_FullFlow_Success() {
        when(mockContextService.resolveContext(eq(sampleBot), any(), any())).thenReturn(sampleContext);

        String configResponse = "{\"data\":{\"media_upload_config\":{\"network_start\":{\"uri\":\"https://vupload2.facebook.com/ajax/video/upload/requests/start/\"},\"network_receive\":{\"uri\":\"https://vupload2.facebook.com/ajax/video/upload/requests/receive/\"},\"network_upload_service\":{\"targeted\":{\"service_name\":\"rupload-hkg4-2.up\",\"service_domain\":\"facebook.com\"}}}}}";
        String startResponse = "for (;;);{\"__ar\":1,\"payload\":{\"video_id\":\"1629089142153499\",\"upload_session_id\":\"1629089148820165\",\"start_offset\":0,\"end_offset\":1048576,\"skip_upload\":false}}";
        String probeResponse = "{\"dc\":\"eag6c03\",\"offset\":0}";
        String transferResponse = "1:c25hcHRpay52bl83NTA0OTE5NDY3Mjc3Nzk4NjcyLm1wNA==:video/mp4:GLPtByOfq7Qt_lwIAEwEYkNNoBAtbugbAAAP:e:1790063199:ARY-oeIC2OG4ReUxF3k";
        String receiveResponse = "for (;;);{\"__ar\":1,\"payload\":{\"start_offset\":15043287,\"end_offset\":15043287}}";

        OkHttpClient mockClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    String url = chain.request().url().toString();
                    String method = chain.request().method();
                    String body = "{}";
                    String contentType = "application/json";

                    if (url.contains("graphql")) {
                        body = configResponse;
                    } else if (url.contains("requests/start")) {
                        body = startResponse;
                    } else if (url.contains("rupload") && method.equals("GET")) {
                        body = probeResponse;
                    } else if (url.contains("rupload") && method.equals("POST")) {
                        body = transferResponse;
                        contentType = "text/plain";
                    } else if (url.contains("requests/receive")) {
                        body = receiveResponse;
                    }

                    return new Response.Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(ResponseBody.create(body, okhttp3.MediaType.get(contentType)))
                            .build();
                })
                .build();

        SendRequest sendRequest = new SendRequest(mockClient);
        UploadVideo uploadVideo = new UploadVideo(sendRequest, mockContextService);

        byte[] videoData = new byte[100];
        UploadVideoResult result = uploadVideo.uploadVideo(sampleBot, videoData, "snaptik.vn_7504919467277798672.mp4", null, null);

        assertTrue(result.isSuccess());
        assertEquals("1629089142153499", result.getVideoId());
        assertEquals("1629089148820165", result.getUploadSessionId());
        assertEquals(100L, result.getFileSize());
    }
}
