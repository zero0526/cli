package com.fb.cli.domains.facebook.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fb.cli.dtos.facebook.FacebookWebContext;
import com.fb.cli.dtos.facebook.UploadImageResult;
import com.fb.cli.dtos.proxy.ProxyInfo;
import com.fb.cli.dtos.proxy.RandomSamplingCfg;
import com.fb.cli.entities.Bot;
import com.fb.cli.services.external.HttpRequestCall;
import com.fb.cli.services.external.SendRequest;
import com.fb.cli.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadImage {

    private static final String UPLOAD_ENDPOINT = "https://upload.facebook.com/ajax/react_composer/attachments/photo/upload";

    private final SendRequest sendRequest;
    private final FacebookWebContextService contextService;

    /**
     * Upload ảnh lên Facebook photo attachment endpoint và nhận về photoID
     */
    public UploadImageResult uploadPhoto(Bot bot, byte[] imageBytes, String fileName, ProxyInfo proxy, RandomSamplingCfg proxySampler) {
        if (bot == null) {
            return UploadImageResult.fail("BOT_NULL", "Bot không được để trống", "");
        }
        if (imageBytes == null || imageBytes.length == 0) {
            return UploadImageResult.fail("EMPTY_IMAGE", "Dữ liệu ảnh rỗng", "");
        }

        FacebookWebContext context = contextService.resolveContext(bot, proxy, proxySampler);
        if (context == null) {
            return UploadImageResult.fail("AUTH_FAILED", "Không thể lấy Facebook Web context (lsd/dtsg)", "");
        }

        String userId = context.getUserId();
        String safeFileName = (fileName != null && !fileName.isBlank()) ? fileName : "photo.png";
        String mediaType = detectMediaType(safeFileName);
        String uploadId = "jsc_c_" + UUID.randomUUID().toString().substring(0, 8);

        String uploadUrl = String.format(
                "%s?av=%s&__user=%s&__a=1&__req=29&dpr=1&__ccg=EXCELLENT&__rev=1047863799&__comet_req=15" +
                        "&fb_dtsg=%s&jazoest=%s&lsd=%s&__spin_b=trunk",
                UPLOAD_ENDPOINT,
                userId,
                userId,
                URLEncoder.encode(context.getDtsgToken() != null ? context.getDtsgToken() : "", StandardCharsets.UTF_8),
                URLEncoder.encode(context.getJazoest() != null ? context.getJazoest() : "2", StandardCharsets.UTF_8),
                URLEncoder.encode(context.getLsdToken() != null ? context.getLsdToken() : "", StandardCharsets.UTF_8)
        );

        try {
            HttpRequestCall call = sendRequest.post(uploadUrl)
                    .header("origin", "https://www.facebook.com")
                    .header("referer", "https://www.facebook.com/")
                    .header("accept", "*/*")
                    .header("sec-fetch-site", "same-site")
                    .header("sec-fetch-mode", "cors")
                    .header("sec-fetch-dest", "empty")
                    .withBot(bot)
                    .addFormDataPart("source", "8")
                    .addFormDataPart("profile_id", userId)
                    .addFormDataPart("waterfallxapp", "comet")
                    .addFormDataPart("upload_id", uploadId)
                    .addFormDataPart("farr", safeFileName, imageBytes, mediaType);

            if (proxy != null) {
                call.withProxy(proxy);
            } else if (proxySampler != null) {
                call.withProxy(proxySampler);
            }

            String responseText = call.execute();
            return parseUploadResponse(responseText);

        } catch (Exception e) {
            log.error("[UploadImage] Lỗi khi upload ảnh cho bot {}: {}", userId, e.getMessage(), e);
            return UploadImageResult.fail("EXCEPTION", e.getMessage(), "");
        }
    }

    /**
     * Tiện ích upload từ File
     */
    public UploadImageResult uploadPhoto(Bot bot, File file, ProxyInfo proxy, RandomSamplingCfg proxySampler) {
        if (file == null || !file.exists()) {
            return UploadImageResult.fail("FILE_NOT_FOUND", "File không tồn tại", "");
        }
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            return uploadPhoto(bot, bytes, file.getName(), proxy, proxySampler);
        } catch (Exception e) {
            return UploadImageResult.fail("FILE_READ_ERROR", "Lỗi đọc file: " + e.getMessage(), "");
        }
    }

    private UploadImageResult parseUploadResponse(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return UploadImageResult.fail("EMPTY_RESPONSE", "Phản hồi upload ảnh rỗng", "");
        }

        String json = rawResponse.trim();
        if (json.startsWith("for (;;);")) {
            json = json.substring(9).trim();
        }

        try {
            JsonNode root = JsonUtils.MAPPER.readTree(json);
            JsonNode payload = root.path("payload");
            if (!payload.isMissingNode() && !payload.isNull()) {
                String photoId = payload.path("photoID").asText(null);
                if (photoId == null || photoId.isBlank()) {
                    photoId = payload.path("fbid").asText(null);
                }

                if (photoId != null && !photoId.isBlank()) {
                    String imageSrc = payload.path("imageSrc").asText(null);
                    int width = payload.path("width").asInt(0);
                    int height = payload.path("height").asInt(0);
                    log.info("[UploadImage] Upload ảnh thành công! photoID={}", photoId);
                    return UploadImageResult.ok(photoId, imageSrc, width, height, rawResponse);
                }
            }

            String errorMsg = root.path("errorSummary").asText(root.path("error").asText("Upload thất bại"));
            return UploadImageResult.fail("UPLOAD_ERROR", errorMsg, rawResponse);

        } catch (Exception e) {
            log.error("[UploadImage] Lỗi parse JSON upload response: {}", e.getMessage());
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
