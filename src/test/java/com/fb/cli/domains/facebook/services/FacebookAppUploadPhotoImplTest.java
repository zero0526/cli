package com.fb.cli.domains.facebook.services;

import com.fb.cli.dtos.facebook.UploadImageResult;
import com.fb.cli.entities.ActionConfig;
import com.fb.cli.entities.Bot;
import com.fb.cli.services.external.HttpRequestCall;
import com.fb.cli.services.external.SendRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class FacebookAppUploadPhotoImplTest {

    private SendRequest sendRequest;
    private ActionConfigService actionConfigService;
    private FacebookAppUploadPhotoImpl appUploadPhoto;

    @BeforeEach
    void setUp() {
        sendRequest = Mockito.mock(SendRequest.class);
        actionConfigService = Mockito.mock(ActionConfigService.class);
        appUploadPhoto = new FacebookAppUploadPhotoImpl(sendRequest, actionConfigService);
    }

    @Test
    @DisplayName("Upload photo thất bại khi bot null")
    void testUploadPhoto_BotNull() {
        UploadImageResult result = appUploadPhoto.uploadPhoto(null, new byte[]{1, 2, 3}, "test.jpg", null, null);
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals("BOT_NULL", result.getErrorCode());
    }

    @Test
    @DisplayName("Upload photo thất bại khi dữ liệu ảnh rỗng")
    void testUploadPhoto_EmptyBytes() {
        Bot bot = Bot.builder().botId("123456").token("EAAG...").build();
        UploadImageResult result = appUploadPhoto.uploadPhoto(bot, new byte[0], "test.jpg", null, null);
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals("EMPTY_IMAGE", result.getErrorCode());
    }

    @Test
    @DisplayName("Upload photo thất bại khi không có ActionConfig")
    void testUploadPhoto_ConfigNotFound() {
        Bot bot = Bot.builder().botId("123456").token("EAAG...").build();
        when(actionConfigService.getActiveConfig("FACEBOOK", "UPLOAD_PHOTO", "APP_GRAPHQL")).thenReturn(null);

        UploadImageResult result = appUploadPhoto.uploadPhoto(bot, new byte[]{1, 2, 3}, "test.jpg", null, null);
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals("CONFIG_NOT_FOUND", result.getErrorCode());
    }

    @Test
    @DisplayName("Upload photo thành công khi API trả về photo id")
    void testUploadPhoto_Success() throws Exception {
        Bot bot = Bot.builder().botId("123456").token("EAAG...").build();

        ActionConfig config = ActionConfig.builder()
                .endpointUrl("https://graph.facebook.com/v23.0/me/photos")
                .httpMethod("POST")
                .headers("{\"Authorization\": \"OAuth {{accessToken}}\"}")
                .formParams("{\"published\": \"false\"}")
                .build();

        when(actionConfigService.getActiveConfig("FACEBOOK", "UPLOAD_PHOTO", "APP_GRAPHQL")).thenReturn(config);

        HttpRequestCall mockCall = Mockito.mock(HttpRequestCall.class, Mockito.RETURNS_SELF);
        when(sendRequest.post(anyString())).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn("{\"id\":\"9876543210\"}");

        UploadImageResult result = appUploadPhoto.uploadPhoto(bot, new byte[]{1, 2, 3}, "avatar.jpg", null, null);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("9876543210", result.getPhotoId());
    }
}
