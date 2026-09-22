package com.fb.cli.domains.facebook.actions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fb.cli.domains.facebook.services.ActionConfigService;
import com.fb.cli.domains.facebook.services.UploadPhoto;
import com.fb.cli.domains.facebook.utils.ActionTemplateResolver;
import com.fb.cli.dtos.facebook.UploadImageResult;
import com.fb.cli.dtos.proxy.ProxyInfo;
import com.fb.cli.entities.ActionConfig;
import com.fb.cli.entities.Bot;
import com.fb.cli.services.external.HttpRequestCall;
import com.fb.cli.services.external.SendRequest;
import com.fb.cli.services.internal.BotService;
import com.fb.cli.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service("facebookAppCover")
public class CoverImpl implements Cover {

    private static final String PLATFORM = "FACEBOOK";
    private static final String ACTION_PROVIDER = "APP_GRAPHQL";
    private static final String ACTION_SET_COVER_PHOTO = "SET_COVER_PHOTO";

    private final SendRequest sendRequest;
    private final BotService botService;
    private final ActionConfigService actionConfigService;
    private final UploadPhoto uploadPhotoService;

    public CoverImpl(SendRequest sendRequest,
                     BotService botService,
                     ActionConfigService actionConfigService,
                     @Qualifier("facebookAppUploadPhoto") UploadPhoto uploadPhotoService) {
        this.sendRequest = sendRequest;
        this.botService = botService;
        this.actionConfigService = actionConfigService;
        this.uploadPhotoService = uploadPhotoService;
    }

    @Override
    public boolean setCover(String imgPath) {
        Bot bot = botService.getBot(PLATFORM);
        if (bot == null) {
            log.error("[CoverImpl] Không tìm thấy bot Facebook khả dụng");
            return false;
        }
        return setCover(imgPath, bot, null);
    }

    @Override
    public boolean setCover(String imgPath, Bot bot, ProxyInfo proxy) {
        if (bot == null) {
            log.error("[CoverImpl] Bot không được để trống");
            return false;
        }
        if (imgPath == null || imgPath.isBlank()) {
            log.error("[CoverImpl] Đường dẫn ảnh không được để trống");
            return false;
        }

        File file = new File(imgPath);
        if (!file.exists() || !file.isFile()) {
            log.error("[CoverImpl] File ảnh không tồn tại: {}", imgPath);
            return false;
        }

        try {
            // Bước 1: Upload photo qua UploadPhoto service đa hình
            UploadImageResult uploadResult = uploadPhotoService.uploadPhoto(bot, file, proxy);
            if (uploadResult == null || !uploadResult.isSuccess() || uploadResult.getPhotoId() == null) {
                log.error("[CoverImpl] Upload ảnh bìa thất bại: {}", uploadResult != null ? uploadResult.getErrorMessage() : "null");
                return false;
            }

            String photoId = uploadResult.getPhotoId();

            // Bước 2: Gọi publish photo đặt làm ảnh bìa
            return executeSetCoverPhoto(photoId, bot, proxy);

        } catch (Exception e) {
            log.error("[CoverImpl] Lỗi khi đổi ảnh bìa: {}", e.getMessage(), e);
            return false;
        }
    }

    private boolean executeSetCoverPhoto(String photoId, Bot bot, ProxyInfo proxy) {
        ActionConfig config = actionConfigService.getActiveConfig(PLATFORM, ACTION_SET_COVER_PHOTO, ACTION_PROVIDER);
        if (config == null) {
            log.error("[CoverImpl] Không tìm thấy action config cho {}/{}", PLATFORM, ACTION_SET_COVER_PHOTO);
            return false;
        }

        String sessionId = UUID.randomUUID().toString();
        String connUuid = UUID.randomUUID().toString().replace("-", "");

        Map<String, String> contextMap = new HashMap<>();
        contextMap.put("accessToken", bot.getToken());
        contextMap.put("userId", bot.getBotId());
        contextMap.put("photoId", photoId);
        contextMap.put("sessionId", sessionId);
        contextMap.put("connUuid", connUuid);

        String resolvedPayload = ActionTemplateResolver.resolveTemplate(config.getPayloadTemplate(), contextMap);
        contextMap.put("PAYLOAD_VARIABLES", resolvedPayload);

        Map<String, String> headers = ActionTemplateResolver.resolveMapTemplate(config.getHeaders(), contextMap);
        Map<String, String> formParams = ActionTemplateResolver.resolveMapTemplate(config.getFormParams(), contextMap);

        HttpRequestCall call = sendRequest.request(config.getHttpMethod(), config.getEndpointUrl())
                .headers(headers)
                .withBot(bot)
                .formBody(formParams);

        if (proxy != null) {
            call.withProxy(proxy);
        }

        String response = call.execute();
        boolean success = parseBatchResponse(response);
        if (success) {
            log.info("[CoverImpl] Đặt ảnh bìa thành công cho bot {}: photoId={}", bot.getBotId(), photoId);
            return true;
        } else {
            log.warn("[CoverImpl] Đặt ảnh bìa thất bại: {}", response);
            return false;
        }
    }

    private boolean parseBatchResponse(String response) {
        if (response == null || response.isBlank()) {
            return false;
        }
        try {
            JsonNode root = JsonUtils.MAPPER.readTree(response);
            if (root.isArray() && root.size() > 0) {
                JsonNode firstElement = root.get(0);
                if (firstElement.isArray() && firstElement.size() >= 2) {
                    int code = firstElement.get(0).path("code").asInt(0);
                    JsonNode bodyNode = firstElement.get(1).path("body");
                    if (code == 200 && (bodyNode.has("id") || bodyNode.asText().contains("\"id\""))) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {}

        // Fallback kiểm tra text
        return response.contains("\"code\":200") && response.contains("\"id\"");
    }
}
