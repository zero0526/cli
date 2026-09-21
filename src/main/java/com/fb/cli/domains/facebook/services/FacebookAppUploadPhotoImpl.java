package com.fb.cli.domains.facebook.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fb.cli.domains.facebook.utils.ActionTemplateResolver;
import com.fb.cli.dtos.facebook.UploadImageResult;
import com.fb.cli.dtos.proxy.ProxyInfo;
import com.fb.cli.dtos.proxy.RandomSamplingCfg;
import com.fb.cli.entities.ActionConfig;
import com.fb.cli.entities.Bot;
import com.fb.cli.services.external.HttpRequestCall;
import com.fb.cli.services.external.SendRequest;
import com.fb.cli.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service("facebookAppUploadPhoto")
@RequiredArgsConstructor
public class FacebookAppUploadPhotoImpl implements UploadPhoto {

    private static final String PLATFORM = "FACEBOOK";
    private static final String ACTION_PROVIDER = "APP_GRAPHQL";
    private static final String ACTION_UPLOAD_PHOTO = "UPLOAD_PHOTO";

    private final SendRequest sendRequest;
    private final ActionConfigService actionConfigService;

    @Override
    public UploadImageResult uploadPhoto(Bot bot, byte[] imageBytes, String fileName, ProxyInfo proxy, RandomSamplingCfg proxySampler) {
        if (bot == null) {
            return UploadImageResult.fail("BOT_NULL", "Bot không được để trống", "");
        }
        if (imageBytes == null || imageBytes.length == 0) {
            return UploadImageResult.fail("EMPTY_IMAGE", "Dữ liệu ảnh rỗng", "");
        }

        ActionConfig config = actionConfigService.getActiveConfig(PLATFORM, ACTION_UPLOAD_PHOTO, ACTION_PROVIDER);
        if (config == null) {
            log.error("[FacebookAppUploadPhotoImpl] Không tìm thấy action config cho {}/{}/{}", PLATFORM, ACTION_UPLOAD_PHOTO, ACTION_PROVIDER);
            return UploadImageResult.fail("CONFIG_NOT_FOUND", "Không tìm thấy cấu hình UPLOAD_PHOTO cho APP_GRAPHQL", "");
        }

        String safeFileName = (fileName != null && !fileName.isBlank()) ? fileName : "photo.jpg";
        String mediaType = detectMediaType(safeFileName);
        String sessionId = UUID.randomUUID().toString();

        Map<String, String> contextMap = new HashMap<>();
        contextMap.put("accessToken", bot.getToken());
        contextMap.put("sessionId", sessionId);
        contextMap.put("userId", bot.getBotId());

        try {
            Map<String, String> headers = ActionTemplateResolver.resolveMapTemplate(config.getHeaders(), contextMap);
            Map<String, String> formParams = ActionTemplateResolver.resolveMapTemplate(config.getFormParams(), contextMap);

            HttpRequestCall call = sendRequest.post(config.getEndpointUrl())
                    .headers(headers)
                    .withBot(bot);

            for (Map.Entry<String, String> entry : formParams.entrySet()) {
                call.addFormDataPart(entry.getKey(), entry.getValue());
            }
            call.addFormDataPart("source", safeFileName, imageBytes, mediaType);

            if (proxy != null) {
                call.withProxy(proxy);
            } else if (proxySampler != null) {
                call.withProxy(proxySampler);
            }

            String response = call.execute();
            return parseUploadResponse(response);

        } catch (Exception e) {
            log.error("[FacebookAppUploadPhotoImpl] Lỗi khi upload ảnh cho bot {}: {}", bot.getBotId(), e.getMessage(), e);
            return UploadImageResult.fail("EXCEPTION", e.getMessage(), "");
        }
    }

    @Override
    public UploadImageResult uploadPhoto(Bot bot, File file, ProxyInfo proxy, RandomSamplingCfg proxySampler) {
        if (file == null || !file.exists()) {
            return UploadImageResult.fail("FILE_NOT_FOUND", "File không tồn tại", "");
        }
        try {
            byte[] imageBytes = Files.readAllBytes(file.toPath());
            return uploadPhoto(bot, imageBytes, file.getName(), proxy, proxySampler);
        } catch (Exception e) {
            log.error("[FacebookAppUploadPhotoImpl] Lỗi đọc file ảnh {}: {}", file.getAbsolutePath(), e.getMessage(), e);
            return UploadImageResult.fail("FILE_READ_ERROR", "Lỗi đọc file: " + e.getMessage(), "");
        }
    }

    private UploadImageResult parseUploadResponse(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return UploadImageResult.fail("EMPTY_RESPONSE", "Phản hồi upload ảnh rỗng", "");
        }
        try {
            JsonNode root = JsonUtils.MAPPER.readTree(rawResponse);
            if (root.has("id")) {
                String photoId = root.path("id").asText();
                log.info("[FacebookAppUploadPhotoImpl] Upload ảnh thành công, photoId={}", photoId);
                return UploadImageResult.ok(photoId, null, null, null, rawResponse);
            }

            String errorMsg = "Upload ảnh thất bại";
            if (root.has("error")) {
                errorMsg = root.path("error").path("message").asText(errorMsg);
            }
            log.warn("[FacebookAppUploadPhotoImpl] Upload ảnh thất bại: {}", rawResponse);
            return UploadImageResult.fail("UPLOAD_ERROR", errorMsg, rawResponse);

        } catch (Exception e) {
            log.error("[FacebookAppUploadPhotoImpl] Lỗi parse JSON upload response: {}", e.getMessage());
            return UploadImageResult.fail("PARSE_ERROR", "Lỗi parse JSON: " + e.getMessage(), rawResponse);
        }
    }

    private String detectMediaType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lower.endsWith(".gif")) {
            return "image/gif";
        } else if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/png";
    }
}
