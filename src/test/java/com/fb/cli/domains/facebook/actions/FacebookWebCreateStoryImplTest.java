package com.fb.cli.domains.facebook.actions;

import com.fb.cli.domains.facebook.services.ActionConfigService;
import com.fb.cli.domains.facebook.services.FacebookWebContextService;
import com.fb.cli.domains.facebook.services.UploadImage;
import com.fb.cli.domains.facebook.services.UploadVideo;
import com.fb.cli.dtos.bot.BotDetermineCfg;
import com.fb.cli.dtos.bot.BotSamplingConfig;
import com.fb.cli.dtos.facebook.*;
import com.fb.cli.entities.ActionConfig;
import com.fb.cli.entities.Bot;
import com.fb.cli.services.external.SendRequest;
import com.fb.cli.services.internal.BotService;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class FacebookWebCreateStoryImplTest {

    private ActionConfigService mockActionConfigService;
    private UploadImage mockUploadImage;
    private UploadVideo mockUploadVideo;
    private ActionConfig sampleStoryConfig;
    private BotService mockBotService;
    private FacebookWebContextService mockContextService;
    private Bot bot;
    private FacebookWebContext context;

    private static final String SUCCESS_RESPONSE = "{\"data\":{\"story_create\":{\"story\":{\"id\":\"UzpfSTYxNTg2Mjk2MjY0MDMzOjEyMjE0MjAzODU3NzIwOTg3NQ==\",\"legacy_story_hideable_id\":\"122142038577209875\",\"url\":\"https://www.facebook.com/permalink.php?story_fbid=pfbid0kXaKLnVg29yBAMX6fCRAgqBDxmN4DJL5743uXoCuGTzeUBBebWEXaMe6LoSn6JDZl&id=61586296264033\"}}}}";

    @BeforeEach
    void setUp() {
        mockActionConfigService = Mockito.mock(ActionConfigService.class);
        mockUploadImage = Mockito.mock(UploadImage.class);
        mockUploadVideo = Mockito.mock(UploadVideo.class);
        mockBotService = Mockito.mock(BotService.class);
        mockContextService = Mockito.mock(FacebookWebContextService.class);

        sampleStoryConfig = ActionConfig.builder()
                .platform("FACEBOOK")
                .actionType("CREATE_STORY")
                .actionProvider("WEB_GRAPHQL")
                .endpointUrl("https://www.facebook.com/api/graphql/")
                .httpMethod("POST")
                .headers("{\"x-fb-friendly-name\":\"ComposerStoryCreateMutation\",\"x-fb-lsd\":\"{{lsdToken}}\"}")
                .formParams("{\"av\":\"{{userId}}\",\"doc_id\":\"28778531428503134\",\"variables\":\"{{PAYLOAD_VARIABLES}}\"}")
                .payloadTemplate("{\"input\":{\"actor_id\":\"{{userId}}\",\"attachments\":\"{{attachments}}\",\"message\":{\"text\":\"{{content}}\"}}}")
                .metadata("{\"attachment_templates\":{\"photo\":{\"photo\":{\"id\":\"{{photoId}}\"}},\"video\":{\"video\":{\"id\":\"{{videoId}}\"}}}}")
                .extractorRules("{\"success_path\":\"data.story_create.story.id\",\"error_path\":\"errors[0].description\",\"error_code_path\":\"errors[0].code\"}")
                .build();

        when(mockActionConfigService.getActiveConfig("FACEBOOK", "CREATE_STORY", "WEB_GRAPHQL"))
                .thenReturn(sampleStoryConfig);

        bot = Bot.builder()
                .botId("61586296264033")
                .cookies("c_user=61586296264033; xs=sec88;")
                .userAgent("Mozilla/5.0")
                .build();
        when(mockBotService.getBot(any(BotSamplingConfig.class))).thenReturn(bot);

        context = FacebookWebContext.builder()
                .userId("61586296264033")
                .jazoest("25387")
                .lsdToken("LSD_123")
                .dtsgToken("DTSG_ABC")
                .build();
        when(mockContextService.resolveContext(eq(bot), any(), any())).thenReturn(context);
    }

    private FacebookWebCreateStoryImpl buildAction(AtomicReference<String> capturedBody) {
        OkHttpClient mockClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    if (capturedBody != null && chain.request().body() != null) {
                        okio.Buffer buffer = new okio.Buffer();
                        chain.request().body().writeTo(buffer);
                        capturedBody.set(buffer.readUtf8());
                    }
                    return new Response.Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(ResponseBody.create(SUCCESS_RESPONSE, okhttp3.MediaType.get("application/json")))
                            .build();
                })
                .build();

        SendRequest sendRequest = new SendRequest(mockClient);
        return new FacebookWebCreateStoryImpl(
                sendRequest, mockBotService, mockContextService, mockActionConfigService, mockUploadImage, mockUploadVideo
        );
    }

    @Test
    @DisplayName("Tạo story kèm ảnh tự động upload thành công")
    void testCreateStoryWithImageBytes() {
        UploadImageResult uploadOk = UploadImageResult.ok("122142038481209875", "https://fb.com/image.jpg", 1280, 886, "{}");
        when(mockUploadImage.uploadPhoto(eq(bot), any(byte[].class), anyString(), any(), any())).thenReturn(uploadOk);

        AtomicReference<String> capturedBody = new AtomicReference<>();
        FacebookWebCreateStoryImpl createStoryAction = buildAction(capturedBody);

        CreateStoryRequest request = CreateStoryRequest.builder()
                .content("hi!!")
                .imageBytes(new byte[]{1, 2, 3})
                .imageName("photo.png")
                .botConfig(new BotDetermineCfg("facebook", "61586296264033"))
                .build();

        CreateStoryResult response = createStoryAction.createStory(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("122142038481209875", response.getPhotoId());
        assertEquals("122142038577209875", response.getPostId());
        assertEquals("UzpfSTYxNTg2Mjk2MjY0MDMzOjEyMjE0MjAzODU3NzIwOTg3NQ==", response.getStoryId());

        assertNotNull(capturedBody.get());
        assertTrue(capturedBody.get().contains("122142038481209875"));
    }

    @Test
    @DisplayName("Tạo story kèm video tự động upload thành công")
    void testCreateStoryWithVideoBytes() {
        UploadVideoResult videoOk = UploadVideoResult.ok("8899001122", "session_123", 1024L, "{}");
        when(mockUploadVideo.uploadVideo(eq(bot), any(byte[].class), anyString(), any(), any())).thenReturn(videoOk);

        AtomicReference<String> capturedBody = new AtomicReference<>();
        FacebookWebCreateStoryImpl createStoryAction = buildAction(capturedBody);

        CreateStoryRequest request = CreateStoryRequest.builder()
                .content("video caption")
                .videoBytes(new byte[]{4, 5, 6})
                .videoName("sample.mp4")
                .botConfig(new BotDetermineCfg("facebook", "61586296264033"))
                .build();

        CreateStoryResult response = createStoryAction.createStory(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("8899001122", response.getVideoId());
        assertEquals("UzpfSTYxNTg2Mjk2MjY0MDMzOjEyMjE0MjAzODU3NzIwOTg3NQ==", response.getStoryId());

        assertNotNull(capturedBody.get());
        assertTrue(capturedBody.get().contains("8899001122"));
    }

    @Test
    @DisplayName("Tạo bài viết linh hoạt với nhiều ảnh và nhiều video cùng lúc")
    void testCreateStoryWithMultiplePhotosAndVideos() {
        UploadImageResult img1 = UploadImageResult.ok("photo_111", "https://fb.com/1.jpg", 100, 100, "{}");
        UploadImageResult img2 = UploadImageResult.ok("photo_222", "https://fb.com/2.jpg", 100, 100, "{}");
        when(mockUploadImage.uploadPhoto(eq(bot), any(byte[].class), anyString(), any(), any()))
                .thenReturn(img1)
                .thenReturn(img2);

        UploadVideoResult vid1 = UploadVideoResult.ok("video_333", "sess_1", 2048L, "{}");
        when(mockUploadVideo.uploadVideo(eq(bot), any(byte[].class), anyString(), any(), any()))
                .thenReturn(vid1);

        AtomicReference<String> capturedBody = new AtomicReference<>();
        FacebookWebCreateStoryImpl createStoryAction = buildAction(capturedBody);

        // Gửi 2 ảnh (1 byte array, 1 photoId có sẵn) và 2 video (1 byte array, 1 videoId có sẵn)
        CreateStoryRequest request = CreateStoryRequest.builder()
                .content("Post with multiple photos and videos!")
                .imageBytes(new byte[]{1, 1})
                .photoId("photo_pre_uploaded")
                .videoBytes(new byte[]{2, 2})
                .videoId("video_pre_uploaded")
                .botConfig(new BotDetermineCfg("facebook", "61586296264033"))
                .build();

        CreateStoryResult response = createStoryAction.createStory(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(2, response.getPhotoIds().size());
        assertTrue(response.getPhotoIds().contains("photo_111"));
        assertTrue(response.getPhotoIds().contains("photo_pre_uploaded"));

        assertEquals(2, response.getVideoIds().size());
        assertTrue(response.getVideoIds().contains("video_333"));
        assertTrue(response.getVideoIds().contains("video_pre_uploaded"));

        assertNotNull(capturedBody.get());
        // Body phải chứa đủ các attachment IDs
        assertTrue(capturedBody.get().contains("photo_111"));
        assertTrue(capturedBody.get().contains("photo_pre_uploaded"));
        assertTrue(capturedBody.get().contains("video_333"));
        assertTrue(capturedBody.get().contains("video_pre_uploaded"));
    }

    @Test
    @DisplayName("Tạo bài viết dạng status chữ thuần (không đính kèm media -> attachments = [])")
    void testCreateStoryTextOnly() {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        FacebookWebCreateStoryImpl createStoryAction = buildAction(capturedBody);

        CreateStoryRequest request = CreateStoryRequest.builder()
                .content("Hello World Text Only!")
                .botConfig(new BotDetermineCfg("facebook", "61586296264033"))
                .build();

        CreateStoryResult response = createStoryAction.createStory(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertTrue(response.getPhotoIds().isEmpty());
        assertTrue(response.getVideoIds().isEmpty());

        assertNotNull(capturedBody.get());
        // Form body được URL encoded, "attachments":[] mã hóa thành %22attachments%22%3A%5B%5D
        assertTrue(capturedBody.get().contains("attachments%22%3A%5B%5D") || capturedBody.get().contains("\"attachments\":[]"));
    }
}
