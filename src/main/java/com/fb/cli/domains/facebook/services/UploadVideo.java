package com.fb.cli.domains.facebook.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fb.cli.dtos.facebook.FacebookWebContext;
import com.fb.cli.dtos.facebook.UploadVideoResult;
import com.fb.cli.dtos.proxy.ProxyInfo;
import com.fb.cli.dtos.proxy.RandomSamplingCfg;
import com.fb.cli.entities.Bot;
import com.fb.cli.services.external.HttpRequestCall;
import com.fb.cli.services.external.SendRequest;
import com.fb.cli.utils.JsonUtils;
import lombok.Builder;
import lombok.Data;
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
public class UploadVideo {

    private static final String DEFAULT_START_URL = "https://www.facebook.com/ajax/video/upload/requests/start/";
    private static final String DEFAULT_RECEIVE_URL = "https://www.facebook.com/ajax/video/upload/requests/receive/";
    private static final String DEFAULT_RUPLOAD_HOST = "rupload.facebook.com";
    private static final String GRAPHQL_URL = "https://www.facebook.com/api/graphql/";
    private static final String CONFIG_DOC_ID = "26396735533340887";

    private final SendRequest sendRequest;
    private final FacebookWebContextService contextService;

    @Data
    @Builder
    private static class VideoUploadConfig {
        private String startUrl;
        private String receiveUrl;
        private String ruploadHost;
    }

    /**
     * Upload video từ mảng byte
     */
    public UploadVideoResult uploadVideo(Bot bot, byte[] videoBytes, String fileName, ProxyInfo proxy, RandomSamplingCfg proxySampler) {
        if (bot == null) {
            return UploadVideoResult.fail("BOT_NULL", "Bot không được để trống", "");
        }
        if (videoBytes == null || videoBytes.length == 0) {
            return UploadVideoResult.fail("EMPTY_VIDEO", "Dữ liệu video rỗng", "");
        }

        FacebookWebContext context = contextService.resolveContext(bot, proxy, proxySampler);
        if (context == null) {
            return UploadVideoResult.fail("AUTH_FAILED", "Không thể lấy Facebook Web context (lsd/dtsg)", "");
        }

        String userId = context.getUserId();
        String safeFileName = (fileName != null && !fileName.isBlank()) ? fileName : "video.mp4";
        String fileExtension = extractExtension(safeFileName);
        String mediaType = detectMediaType(safeFileName);
        long fileSize = videoBytes.length;
        String waterfallId = UUID.randomUUID().toString().replace("-", "");

        log.info("[UploadVideo] Bắt đầu quy trình upload video '{}' ({} bytes) cho user {}", safeFileName, fileSize, userId);

        // 1. Lấy cấu hình upload (RUpload server, endpoints)
        VideoUploadConfig config = fetchUploadConfig(bot, context, proxy, proxySampler);

        // 2. Khởi tạo phiên upload (requests/start)
        UploadStartResult startResult = startUploadSession(bot, context, config, waterfallId, fileSize, fileExtension, proxy, proxySampler);
        if (!startResult.isSuccess()) {
            return UploadVideoResult.fail(startResult.getErrorCode(), startResult.getErrorMessage(), startResult.getRawResponse());
        }

        // Nếu Facebook deduplicate (skip_upload = true), video_id đã sẵn sàng
        if (startResult.isSkipUpload()) {
            log.info("[UploadVideo] Video đã tồn tại trên Facebook (dedup), bỏ qua upload bytes. videoId={}", startResult.getVideoId());
            return UploadVideoResult.ok(startResult.getVideoId(), startResult.getUploadSessionId(), fileSize, startResult.getRawResponse());
        }

        String videoId = startResult.getVideoId();
        String uploadSessionId = startResult.getUploadSessionId();
        long startOffset = startResult.getStartOffset();
        long endOffset = startResult.getEndOffset() > 0 ? startResult.getEndOffset() : fileSize;
        String streamId = UUID.randomUUID().toString().replace("-", "");

        // 3. Thăm dò / offset probe trên rupload server
        checkRuploadOffset(bot, context, config.getRuploadHost(), streamId, startOffset, endOffset, proxy, proxySampler);

        // 4. Bơm dữ liệu nhị phân lên RUpload
        String chunkToken = transferVideoBytes(bot, context, config.getRuploadHost(), streamId, startOffset, endOffset,
                uploadSessionId, videoId, waterfallId, fileSize, safeFileName, mediaType, videoBytes, proxy, proxySampler);

        if (chunkToken == null || chunkToken.isBlank()) {
            return UploadVideoResult.fail("TRANSFER_FAILED", "Không nhận được chunk token từ máy chủ RUpload", "");
        }

        // 5. Chốt phiên upload (requests/receive)
        UploadVideoResult receiveResult = finishUploadSession(bot, context, config.getReceiveUrl(), waterfallId,
                videoId, fileSize, chunkToken, uploadSessionId, proxy, proxySampler);

        if (receiveResult.isSuccess()) {
            log.info("[UploadVideo] Upload video thành công! videoId={}", videoId);
        }

        return receiveResult;
    }

    /**
     * Tiện ích upload từ File
     */
    public UploadVideoResult uploadVideo(Bot bot, File file, ProxyInfo proxy, RandomSamplingCfg proxySampler) {
        if (file == null || !file.exists()) {
            return UploadVideoResult.fail("FILE_NOT_FOUND", "File không tồn tại", "");
        }
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            return uploadVideo(bot, bytes, file.getName(), proxy, proxySampler);
        } catch (Exception e) {
            return UploadVideoResult.fail("FILE_READ_ERROR", "Lỗi đọc file: " + e.getMessage(), "");
        }
    }

    /**
     * Bước 1: Lấy cấu hình upload từ MediaUploadFBDefaultServerConfigurationRetrieverQuery
     */
    private VideoUploadConfig fetchUploadConfig(Bot bot, FacebookWebContext context, ProxyInfo proxy, RandomSamplingCfg proxySampler) {
        String startUrl = DEFAULT_START_URL;
        String receiveUrl = DEFAULT_RECEIVE_URL;
        String ruploadHost = DEFAULT_RUPLOAD_HOST;

        try {
            HttpRequestCall call = sendRequest.post(GRAPHQL_URL)
                    .header("x-fb-friendly-name", "MediaUploadFBDefaultServerConfigurationRetrieverQuery")
                    .header("x-fb-lsd", context.getLsdToken() != null ? context.getLsdToken() : "")
                    .header("content-type", "application/x-www-form-urlencoded")
                    .header("origin", "https://www.facebook.com")
                    .header("referer", "https://www.facebook.com/")
                    .withBot(bot)
                    .formData("av", context.getUserId())
                    .formData("__user", context.getUserId())
                    .formData("__a", "1")
                    .formData("__req", "2m")
                    .formData("fb_dtsg", context.getDtsgToken() != null ? context.getDtsgToken() : "")
                    .formData("jazoest", context.getJazoest() != null ? context.getJazoest() : "2")
                    .formData("lsd", context.getLsdToken() != null ? context.getLsdToken() : "")
                    .formData("fb_api_caller_class", "RelayModern")
                    .formData("fb_api_req_friendly_name", "MediaUploadFBDefaultServerConfigurationRetrieverQuery")
                    .formData("variables", "{\"source_type\":\"composer\"}")
                    .formData("server_timestamps", "true")
                    .formData("doc_id", CONFIG_DOC_ID);

            if (proxy != null) {
                call.withProxy(proxy);
            } else if (proxySampler != null) {
                call.withProxy(proxySampler);
            }

            String response = call.execute();
            if (response != null && !response.isBlank()) {
                JsonNode root = JsonUtils.MAPPER.readTree(response);
                JsonNode configNode = root.path("data").path("media_upload_config");
                if (!configNode.isMissingNode()) {
                    String fetchedStart = configNode.path("network_start").path("uri").asText(null);
                    String fetchedReceive = configNode.path("network_receive").path("uri").asText(null);
                    if (fetchedStart != null && !fetchedStart.isBlank()) {
                        startUrl = fetchedStart;
                    }
                    if (fetchedReceive != null && !fetchedReceive.isBlank()) {
                        receiveUrl = fetchedReceive;
                    }

                    JsonNode uploadService = configNode.path("network_upload_service");
                    JsonNode targeted = uploadService.path("targeted");
                    if (!targeted.isMissingNode() && !targeted.path("service_name").isMissingNode()) {
                        ruploadHost = targeted.path("service_name").asText() + "." + targeted.path("service_domain").asText("facebook.com");
                    } else {
                        JsonNode def = uploadService.path("default");
                        if (!def.isMissingNode() && !def.path("service_name").isMissingNode()) {
                            ruploadHost = def.path("service_name").asText() + "." + def.path("service_domain").asText("facebook.com");
                        }
                    }
                    log.debug("[UploadVideo] Cấu hình upload: start={}, receive={}, ruploadHost={}", startUrl, receiveUrl, ruploadHost);
                }
            }
        } catch (Exception e) {
            log.warn("[UploadVideo] Không thể lấy cấu hình GraphQL, dùng cấu hình mặc định: {}", e.getMessage());
        }

        return VideoUploadConfig.builder()
                .startUrl(startUrl)
                .receiveUrl(receiveUrl)
                .ruploadHost(ruploadHost)
                .build();
    }

    /**
     * Bước 2: Bắt đầu phiên upload
     */
    private UploadStartResult startUploadSession(Bot bot, FacebookWebContext context, VideoUploadConfig config,
                                                 String waterfallId, long fileSize, String fileExtension,
                                                 ProxyInfo proxy, RandomSamplingCfg proxySampler) {
        String url = config.getStartUrl();
        url = url.contains("?") ? (url + "&__a=1") : (url + "?__a=1");

        try {
            HttpRequestCall call = sendRequest.post(url)
                    .header("x_fb_video_waterfall_id", waterfallId)
                    .header("origin", "https://www.facebook.com")
                    .header("referer", "https://www.facebook.com/")
                    .withBot(bot)
                    .formData("waterfall_id", waterfallId)
                    .formData("target_id", context.getUserId())
                    .formData("source", "composer")
                    .formData("composer_entry_point_ref", "timeline")
                    .formData("supports_chunking", "true")
                    .formData("supports_file_api", "true")
                    .formData("file_size", String.valueOf(fileSize))
                    .formData("file_extension", fileExtension)
                    .formData("partition_start_offset", "0")
                    .formData("partition_end_offset", String.valueOf(fileSize))
                    .formData("composer_dialog_version", "V2")
                    .formData("video_publisher_action_source", "")
                    .formData("av", context.getUserId())
                    .formData("__user", context.getUserId())
                    .formData("__a", "1")
                    .formData("__req", "2n")
                    .formData("dpr", "1")
                    .formData("__ccg", "EXCELLENT")
                    .formData("__rev", "1047863799")
                    .formData("__comet_req", "15")
                    .formData("fb_dtsg", context.getDtsgToken() != null ? context.getDtsgToken() : "")
                    .formData("jazoest", context.getJazoest() != null ? context.getJazoest() : "2")
                    .formData("lsd", context.getLsdToken() != null ? context.getLsdToken() : "")
                    .formData("__spin_b", "trunk");

            if (proxy != null) {
                call.withProxy(proxy);
            } else if (proxySampler != null) {
                call.withProxy(proxySampler);
            }

            String response = cleanJsonResponse(call.execute());
            JsonNode root = JsonUtils.MAPPER.readTree(response);
            JsonNode payload = root.path("payload");

            if (!payload.isMissingNode() && !payload.isNull()) {
                String videoId = payload.path("video_id").asText(null);
                String uploadSessionId = payload.path("upload_session_id").asText(null);
                boolean skipUpload = payload.path("skip_upload").asBoolean(false);
                long startOffset = payload.path("start_offset").asLong(0);
                long endOffset = payload.path("end_offset").asLong(fileSize);

                if (videoId != null && !videoId.isBlank()) {
                    return UploadStartResult.builder()
                            .success(true)
                            .videoId(videoId)
                            .uploadSessionId(uploadSessionId)
                            .skipUpload(skipUpload)
                            .startOffset(startOffset)
                            .endOffset(endOffset)
                            .rawResponse(response)
                            .build();
                }
            }

            String errorMsg = root.path("errorSummary").asText(root.path("error").asText("Khởi tạo upload thất bại"));
            return UploadStartResult.builder()
                    .success(false)
                    .errorCode("START_FAILED")
                    .errorMessage(errorMsg)
                    .rawResponse(response)
                    .build();

        } catch (Exception e) {
            log.error("[UploadVideo] Lỗi gọi start upload session: {}", e.getMessage(), e);
            return UploadStartResult.builder()
                    .success(false)
                    .errorCode("START_EXCEPTION")
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    /**
     * Bước 3: Thăm dò offset trên RUpload
     */
    private void checkRuploadOffset(Bot bot, FacebookWebContext context, String ruploadHost,
                                    String streamId, long startOffset, long endOffset,
                                    ProxyInfo proxy, RandomSamplingCfg proxySampler) {
        String url = String.format(
                "https://%s/fb_video/%s-%d-%d?__aaid=0&__user=%s&__a=1&__comet_req=15&fb_dtsg=%s&jazoest=%s&lsd=%s&__spin_b=trunk",
                ruploadHost,
                streamId,
                startOffset,
                endOffset,
                context.getUserId(),
                urlEncode(context.getDtsgToken()),
                urlEncode(context.getJazoest()),
                urlEncode(context.getLsdToken())
        );

        try {
            HttpRequestCall call = sendRequest.get(url)
                    .header("accept", "*/*")
                    .header("origin", "https://www.facebook.com")
                    .header("referer", "https://www.facebook.com/")
                    .withBot(bot);

            if (proxy != null) {
                call.withProxy(proxy);
            } else if (proxySampler != null) {
                call.withProxy(proxySampler);
            }

            call.execute();
        } catch (Exception e) {
            log.debug("[UploadVideo] RUpload offset probe notice: {}", e.getMessage());
        }
    }

    /**
     * Bước 4: Bơm dữ liệu nhị phân lên RUpload
     */
    private String transferVideoBytes(Bot bot, FacebookWebContext context, String ruploadHost,
                                      String streamId, long startOffset, long endOffset,
                                      String uploadSessionId, String videoId, String waterfallId,
                                      long fileSize, String fileName, String mediaType, byte[] videoBytes,
                                      ProxyInfo proxy, RandomSamplingCfg proxySampler) {
        String url = String.format(
                "https://%s/fb_video/%s-%d-%d?__aaid=0&__user=%s&__a=1&__comet_req=15&fb_dtsg=%s&jazoest=%s&lsd=%s&__spin_b=trunk",
                ruploadHost,
                streamId,
                startOffset,
                endOffset,
                context.getUserId(),
                urlEncode(context.getDtsgToken()),
                urlEncode(context.getJazoest()),
                urlEncode(context.getLsdToken())
        );

        try {
            HttpRequestCall call = sendRequest.post(url)
                    .header("accept", "*/*")
                    .header("origin", "https://www.facebook.com")
                    .header("referer", "https://www.facebook.com/")
                    .header("composer_session_id", waterfallId)
                    .header("id", uploadSessionId != null ? uploadSessionId : "")
                    .header("product_media_id", videoId)
                    .header("start_offset", String.valueOf(startOffset))
                    .header("end_offset", String.valueOf(endOffset))
                    .header("offset", String.valueOf(startOffset))
                    .header("x-entity-length", String.valueOf(fileSize))
                    .header("x-entity-name", fileName)
                    .header("x-entity-type", mediaType)
                    .header("x-total-asset-size", String.valueOf(fileSize))
                    .withBot(bot)
                    .rawBody(videoBytes, "application/octet-stream");

            if (proxy != null) {
                call.withProxy(proxy);
            } else if (proxySampler != null) {
                call.withProxy(proxySampler);
            }

            String response = call.execute();
            if (response != null) {
                return response.trim();
            }
        } catch (Exception e) {
            log.error("[UploadVideo] Lỗi transfer video bytes: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * Bước 5: Chốt phiên upload (requests/receive)
     */
    private UploadVideoResult finishUploadSession(Bot bot, FacebookWebContext context, String receiveUrl,
                                                  String waterfallId, String videoId, long fileSize,
                                                  String chunkToken, String uploadSessionId,
                                                  ProxyInfo proxy, RandomSamplingCfg proxySampler) {
        String url = receiveUrl.contains("?") ? (receiveUrl + "&__a=1") : (receiveUrl + "?__a=1");

        try {
            HttpRequestCall call = sendRequest.post(url)
                    .header("x_fb_video_waterfall_id", waterfallId)
                    .header("origin", "https://www.facebook.com")
                    .header("referer", "https://www.facebook.com/")
                    .withBot(bot)
                    .formData("waterfall_id", waterfallId)
                    .formData("target_id", context.getUserId())
                    .formData("video_id", videoId)
                    .formData("source", "composer")
                    .formData("composer_entry_point_ref", "timeline")
                    .formData("supports_chunking", "true")
                    .formData("supports_upload_service", "true")
                    .formData("partition_start_offset", "0")
                    .formData("partition_end_offset", String.valueOf(fileSize))
                    .formData("start_offset", "0")
                    .formData("end_offset", String.valueOf(fileSize))
                    .formData("upload_speed", "10000000")
                    .formData("fbuploader_video_file_chunk", chunkToken)
                    .formData("composer_dialog_version", "V2")
                    .formData("av", context.getUserId())
                    .formData("__user", context.getUserId())
                    .formData("__a", "1")
                    .formData("__req", "2q")
                    .formData("dpr", "1")
                    .formData("__ccg", "EXCELLENT")
                    .formData("__rev", "1047863799")
                    .formData("__comet_req", "15")
                    .formData("fb_dtsg", context.getDtsgToken() != null ? context.getDtsgToken() : "")
                    .formData("jazoest", context.getJazoest() != null ? context.getJazoest() : "2")
                    .formData("lsd", context.getLsdToken() != null ? context.getLsdToken() : "")
                    .formData("__spin_b", "trunk");

            if (proxy != null) {
                call.withProxy(proxy);
            } else if (proxySampler != null) {
                call.withProxy(proxySampler);
            }

            String rawResponse = cleanJsonResponse(call.execute());
            JsonNode root = JsonUtils.MAPPER.readTree(rawResponse);
            JsonNode payload = root.path("payload");

            if (!payload.isMissingNode() && !payload.isNull()) {
                return UploadVideoResult.ok(videoId, uploadSessionId, fileSize, rawResponse);
            }

            String errorMsg = root.path("errorSummary").asText(root.path("error").asText("Hoàn tất upload thất bại"));
            return UploadVideoResult.fail("RECEIVE_FAILED", errorMsg, rawResponse);

        } catch (Exception e) {
            log.error("[UploadVideo] Lỗi finish upload session: {}", e.getMessage(), e);
            return UploadVideoResult.fail("RECEIVE_EXCEPTION", e.getMessage(), "");
        }
    }

    private String cleanJsonResponse(String rawResponse) {
        if (rawResponse == null) return "";
        String trimmed = rawResponse.trim();
        if (trimmed.startsWith("for (;;);")) {
            return trimmed.substring(9).trim();
        }
        return trimmed;
    }

    private String urlEncode(String val) {
        if (val == null) return "";
        return URLEncoder.encode(val, StandardCharsets.UTF_8);
    }

    private String extractExtension(String fileName) {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        }
        return "mp4";
    }

    private String detectMediaType(String fileName) {
        String ext = extractExtension(fileName);
        return switch (ext) {
            case "mov", "qt" -> "video/quicktime";
            case "mkv" -> "video/x-matroska";
            case "avi" -> "video/x-msvideo";
            case "webm" -> "video/webm";
            case "flv" -> "video/x-flv";
            default -> "video/mp4";
        };
    }

    @Data
    @Builder
    private static class UploadStartResult {
        private boolean success;
        private String videoId;
        private String uploadSessionId;
        private boolean skipUpload;
        private long startOffset;
        private long endOffset;
        private String errorCode;
        private String errorMessage;
        private String rawResponse;
    }
}
