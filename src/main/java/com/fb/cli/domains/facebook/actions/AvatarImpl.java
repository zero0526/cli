package com.fb.cli.domains.facebook.actions;

import com.fb.cli.domains.facebook.services.ActionConfigService;
import com.fb.cli.domains.facebook.services.UploadPhoto;
import com.fb.cli.domains.facebook.utils.ActionTemplateResolver;
import com.fb.cli.domains.facebook.utils.GenericResponseParser;
import com.fb.cli.dtos.facebook.UploadImageResult;
import com.fb.cli.dtos.proxy.ProxyInfo;
import com.fb.cli.entities.ActionConfig;
import com.fb.cli.entities.Bot;
import com.fb.cli.services.external.HttpRequestCall;
import com.fb.cli.services.external.SendRequest;
import com.fb.cli.services.internal.BotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service("facebookAppAvatar")
public class AvatarImpl implements Avatar {

    private static final String PLATFORM = "FACEBOOK";
    private static final String ACTION_PROVIDER = "APP_GRAPHQL";
    private static final String ACTION_SET_PROFILE_PICTURE = "SET_PROFILE_PICTURE";

    private final SendRequest sendRequest;
    private final BotService botService;
    private final ActionConfigService actionConfigService;
    private final UploadPhoto uploadPhotoService;

    public AvatarImpl(SendRequest sendRequest,
                      BotService botService,
                      ActionConfigService actionConfigService,
                      @Qualifier("facebookAppUploadPhoto") UploadPhoto uploadPhotoService) {
        this.sendRequest = sendRequest;
        this.botService = botService;
        this.actionConfigService = actionConfigService;
        this.uploadPhotoService = uploadPhotoService;
    }

    @Override
    public boolean setAvatar(String imgPath) {
        Bot bot = botService.getBot(PLATFORM);
        if (bot == null) {
            log.error("[AvatarImpl] Không tìm thấy bot Facebook khả dụng");
            return false;
        }
        return setAvatar(imgPath, bot, null);
    }

    @Override
    public boolean setAvatar(String imgPath, Bot bot, ProxyInfo proxy) {
        if (bot == null) {
            log.error("[AvatarImpl] Bot không được để trống");
            return false;
        }
        if (imgPath == null || imgPath.isBlank()) {
            log.error("[AvatarImpl] Đường dẫn ảnh không được để trống");
            return false;
        }

        File file = new File(imgPath);
        if (!file.exists() || !file.isFile()) {
            log.error("[AvatarImpl] File ảnh không tồn tại: {}", imgPath);
            return false;
        }

        try {
            // Bước 1: Upload photo qua UploadPhoto service đa hình
            UploadImageResult uploadResult = uploadPhotoService.uploadPhoto(bot, file, proxy);
            if (uploadResult == null || !uploadResult.isSuccess() || uploadResult.getPhotoId() == null) {
                log.error("[AvatarImpl] Upload ảnh thất bại: {}", uploadResult != null ? uploadResult.getErrorMessage() : "null");
                return false;
            }

            String photoId = uploadResult.getPhotoId();

            // Bước 2: Gọi ProfilePictureSetMutation đặt ảnh làm avatar
            return executeSetProfilePicture(photoId, bot, proxy);

        } catch (Exception e) {
            log.error("[AvatarImpl] Lỗi khi đổi avatar: {}", e.getMessage(), e);
            return false;
        }
    }

    private boolean executeSetProfilePicture(String photoId, Bot bot, ProxyInfo proxy) {
        ActionConfig config = actionConfigService.getActiveConfig(PLATFORM, ACTION_SET_PROFILE_PICTURE, ACTION_PROVIDER);
        if (config == null) {
            log.error("[AvatarImpl] Không tìm thấy action config cho {}/{}", PLATFORM, ACTION_SET_PROFILE_PICTURE);
            return false;
        }

        String sessionId = UUID.randomUUID().toString();
        Map<String, String> contextMap = new HashMap<>();
        contextMap.put("accessToken", bot.getToken());
        contextMap.put("userId", bot.getBotId());
        contextMap.put("photoId", photoId);
        contextMap.put("sessionId", sessionId);
        contextMap.put("clientTraceId", UUID.randomUUID().toString());
        contextMap.put("composerSessionId", UUID.randomUUID().toString());
        contextMap.put("clientMutationId", UUID.randomUUID().toString());
        contextMap.put("deviceId", "d0db7ed7-f05d-4ab4-a34b-27ea5eb599f1");

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
        GenericResponseParser.ParseResult parseResult = GenericResponseParser.parse(response, config.getExtractorRules());
        if (parseResult.isSuccess()) {
            log.info("[AvatarImpl] Đặt avatar thành công cho bot {}: photoId={}", bot.getBotId(), photoId);
            return true;
        } else {
            log.warn("[AvatarImpl] Đặt avatar thất bại: code={}, msg={}", parseResult.getErrorCode(), parseResult.getErrorMessage());
            return false;
        }
    }
}
