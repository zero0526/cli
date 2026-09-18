package com.fb.cli.domains.facebook.actions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fb.cli.domains.facebook.services.ActionConfigService;
import com.fb.cli.domains.facebook.services.FacebookWebContextService;
import com.fb.cli.domains.facebook.services.UploadImage;
import com.fb.cli.domains.facebook.services.UploadVideo;
import com.fb.cli.domains.facebook.utils.ActionTemplateResolver;
import com.fb.cli.domains.facebook.utils.GenericResponseParser;
import com.fb.cli.dtos.facebook.*;
import com.fb.cli.entities.ActionConfig;
import com.fb.cli.entities.Bot;
import com.fb.cli.services.external.HttpRequestCall;
import com.fb.cli.services.external.SendRequest;
import com.fb.cli.services.internal.BotService;
import com.fb.cli.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;

@Slf4j
@Service("facebookWebCreateStory")
@RequiredArgsConstructor
public class FacebookWebCreateStoryImpl implements CreateStory {

    private static final String PLATFORM = "FACEBOOK";
    private static final String ACTION_TYPE = "CREATE_STORY";
    private static final String ACTION_PROVIDER = "WEB_GRAPHQL";

    private static final String DEFAULT_PHOTO_ATTACHMENT_TEMPLATE = "{\"photo\":{\"id\":\"{{photoId}}\"}}";
    private static final String DEFAULT_VIDEO_ATTACHMENT_TEMPLATE =
            "{\"video\":{\"id\":\"{{videoId}}\",\"audio_descriptions\":null,\"additional_video_metadata\":{\"translatedAudioMetadata\":[]},\"notify_when_processed\":true,\"transcriptions\":null,\"was_created_via_unified_video_flow\":{\"was_created_via_unified_video_flow\":true}}}";

    private final SendRequest sendRequest;
    private final BotService botService;
    private final FacebookWebContextService contextService;
    private final ActionConfigService actionConfigService;
    private final UploadImage uploadImage;
    private final UploadVideo uploadVideo;

    @Override
    public CreateStoryResult createStory(CreateStoryRequest request) {
        if (request == null) {
            return CreateStoryResult.fail("INVALID_REQUEST", "Request không được để trống", "");
        }

        // 1. Resolve Bot
        Bot bot = request.getBot();
        if (bot == null && request.getBotConfig() != null) {
            bot = botService.getBot(request.getBotConfig());
        }
        if (bot == null) {
            log.error("[FacebookWebCreateStory] Không tìm thấy Bot khả dụng");
            return CreateStoryResult.fail("BOT_NOT_FOUND", "Không tìm thấy Bot khả dụng", "");
        }

        // 2. Thu thập danh sách MediaItem cần xử lý
        List<CreateStoryRequest.MediaItem> mediaItems = collectMediaItems(request);

        // 3. Upload các media chưa có ID (ảnh và video không giới hạn số lượng)
        List<String> uploadedPhotoIds = new ArrayList<>();
        List<String> uploadedVideoIds = new ArrayList<>();

        for (CreateStoryRequest.MediaItem item : mediaItems) {
            if (item.getType() == CreateStoryRequest.MediaType.PHOTO) {
                String pId = item.getId();
                if (pId == null || pId.isBlank()) {
                    UploadImageResult uploadResult = performUploadPhoto(bot, item, request);
                    if (!uploadResult.isSuccess()) {
                        log.error("[FacebookWebCreateStory] Upload ảnh thất bại: {}", uploadResult.getErrorMessage());
                        return CreateStoryResult.fail(uploadResult.getErrorCode(), "Upload ảnh thất bại: " + uploadResult.getErrorMessage(), uploadResult.getRawResponse());
                    }
                    pId = uploadResult.getPhotoId();
                    item.setId(pId);
                }
                uploadedPhotoIds.add(pId);
            } else if (item.getType() == CreateStoryRequest.MediaType.VIDEO) {
                String vId = item.getId();
                if (vId == null || vId.isBlank()) {
                    UploadVideoResult uploadResult = performUploadVideo(bot, item, request);
                    if (!uploadResult.isSuccess()) {
                        log.error("[FacebookWebCreateStory] Upload video thất bại: {}", uploadResult.getErrorMessage());
                        return CreateStoryResult.fail(uploadResult.getErrorCode(), "Upload video thất bại: " + uploadResult.getErrorMessage(), uploadResult.getRawResponse());
                    }
                    vId = uploadResult.getVideoId();
                    item.setId(vId);
                }
                uploadedVideoIds.add(vId);
            }
        }

        // 4. Resolve Action Config từ Database / Redis Cache
        ActionConfig config = actionConfigService.getActiveConfig(PLATFORM, ACTION_TYPE, ACTION_PROVIDER);
        if (config == null) {
            log.error("[FacebookWebCreateStory] Không tìm thấy cấu hình active trong DB cho {}/{}/{}", PLATFORM, ACTION_TYPE, ACTION_PROVIDER);
            return CreateStoryResult.fail("CONFIG_NOT_FOUND", "Không tìm thấy cấu hình action trong database", "");
        }

        // 5. Resolve Web Context (userId, jazoest, lsd, dtsg)
        FacebookWebContext context = contextService.resolveContext(bot, request.getProxy(), request.getProxySampler());
        if (context == null) {
            log.error("[FacebookWebCreateStory] Không thể lấy Web context cho bot {}", bot.getBotId());
            return CreateStoryResult.fail("AUTH_FAILED", "Không thể lấy Web tokens", "");
        }

        // 6. Build Context Variables & Dynamic Attachments
        String content = request.getContent() != null ? request.getContent() : "";
        String privacy = request.getPrivacy() != null ? request.getPrivacy() : "FRIENDS";
        String sessionId = UUID.randomUUID().toString();
        String idempotenceId = UUID.randomUUID().toString();
        long timestamp = System.currentTimeMillis();

        String attachmentsJson = buildAttachmentsJson(mediaItems, config.getMetadata());

        Map<String, String> contextMap = new HashMap<>();
        contextMap.put("userId", context.getUserId());
        contextMap.put("content", content);
        contextMap.put("attachments", attachmentsJson);
        contextMap.put("photoId", uploadedPhotoIds.isEmpty() ? "" : uploadedPhotoIds.get(0));
        contextMap.put("videoId", uploadedVideoIds.isEmpty() ? "" : uploadedVideoIds.get(0));
        contextMap.put("privacy", privacy);
        contextMap.put("sessionId", sessionId);
        contextMap.put("idempotenceId", idempotenceId);
        contextMap.put("timestamp", String.valueOf(timestamp));
        contextMap.put("lsdToken", context.getLsdToken());
        contextMap.put("dtsgToken", context.getDtsgToken());
        contextMap.put("jazoest", context.getJazoest());

        // Resolve Payload Variables template -> Gán vào PAYLOAD_VARIABLES
        String resolvedPayload = ActionTemplateResolver.resolveTemplate(config.getPayloadTemplate(), contextMap);

        // Hỗ trợ tương thích ngược cho template cũ có hardcode photo
        if (uploadedPhotoIds.isEmpty() && uploadedVideoIds.isEmpty()) {
            resolvedPayload = resolvedPayload.replaceAll("\"attachments\"\\s*:\\s*\\[\\s*\\{\\s*\"photo\"\\s*:\\s*\\{\\s*\"id\"\\s*:\\s*\"\"\\s*\\}\\s*\\}\\s*\\]", "\"attachments\":[]");
        }

        contextMap.put("PAYLOAD_VARIABLES", resolvedPayload);

        // Resolve Form Data & Headers
        Map<String, String> headers = ActionTemplateResolver.resolveMapTemplate(config.getHeaders(), contextMap);
        Map<String, String> formData = ActionTemplateResolver.resolveMapTemplate(config.getFormParams(), contextMap);

        // 7. Gửi HTTP Request
        try {
            HttpRequestCall call = sendRequest.request(config.getHttpMethod(), config.getEndpointUrl())
                    .headers(headers)
                    .withBot(bot)
                    .formBody(formData);

            if (request.getProxy() != null) {
                call.withProxy(request.getProxy());
            } else if (request.getProxySampler() != null) {
                call.withProxy(request.getProxySampler());
            }

            String responseText = call.execute();

            // 8. Parse response qua Extractor Rules & JSON Node
            GenericResponseParser.ParseResult parseResult = GenericResponseParser.parse(responseText, config.getExtractorRules());
            if (parseResult.isSuccess()) {
                String storyId = parseResult.getExtractedId();
                String postId = null;
                String postUrl = null;

                try {
                    JsonNode root = JsonUtils.MAPPER.readTree(responseText);
                    JsonNode storyNode = root.path("data").path("story_create").path("story");
                    if (!storyNode.isMissingNode() && !storyNode.isNull()) {
                        if (storyId == null || storyId.isBlank()) {
                            storyId = storyNode.path("id").asText(null);
                        }
                        postId = storyNode.path("legacy_story_hideable_id").asText(null);
                        postUrl = storyNode.path("url").asText(null);
                    }
                } catch (Exception ignored) {}

                log.info("[FacebookWebCreateStory] Đăng bài/story thành công! storyId={}, postId={}, photos={}, videos={}",
                        storyId, postId, uploadedPhotoIds, uploadedVideoIds);
                return CreateStoryResult.ok(storyId, postId, uploadedPhotoIds, uploadedVideoIds, postUrl, responseText);
            } else {
                log.error("[FacebookWebCreateStory] Lỗi GraphQL: code={}, msg={}", parseResult.getErrorCode(), parseResult.getErrorMessage());
                return CreateStoryResult.fail(parseResult.getErrorCode(), parseResult.getErrorMessage(), responseText);
            }

        } catch (Exception e) {
            log.error("[FacebookWebCreateStory] Ngoại lệ khi tạo story: {}", e.getMessage(), e);
            return CreateStoryResult.fail("EXCEPTION", e.getMessage(), "");
        }
    }

    private List<CreateStoryRequest.MediaItem> collectMediaItems(CreateStoryRequest request) {
        List<CreateStoryRequest.MediaItem> items = new ArrayList<>();
        if (request.getMediaItems() != null && !request.getMediaItems().isEmpty()) {
            items.addAll(request.getMediaItems());
        }

        // Photos
        if (request.getPhotoId() != null && !request.getPhotoId().isBlank()) {
            items.add(CreateStoryRequest.MediaItem.photo(request.getPhotoId()));
        }
        if (request.getPhotoIds() != null) {
            for (String pid : request.getPhotoIds()) {
                if (pid != null && !pid.isBlank()) items.add(CreateStoryRequest.MediaItem.photo(pid));
            }
        }
        if (request.getImageBytes() != null && request.getImageBytes().length > 0) {
            items.add(CreateStoryRequest.MediaItem.photo(request.getImageBytes(), request.getImageName()));
        }
        if (request.getImageBytesList() != null) {
            for (byte[] b : request.getImageBytesList()) {
                if (b != null && b.length > 0) items.add(CreateStoryRequest.MediaItem.photo(b, request.getImageName()));
            }
        }
        if (request.getImageFile() != null) {
            items.add(CreateStoryRequest.MediaItem.photo(request.getImageFile()));
        }
        if (request.getImageFiles() != null) {
            for (File f : request.getImageFiles()) {
                if (f != null) items.add(CreateStoryRequest.MediaItem.photo(f));
            }
        }
        if (request.getImagePath() != null && !request.getImagePath().isBlank()) {
            items.add(CreateStoryRequest.MediaItem.photo(request.getImagePath(), request.getImageName()));
        }
        if (request.getImagePaths() != null) {
            for (String path : request.getImagePaths()) {
                if (path != null && !path.isBlank()) items.add(CreateStoryRequest.MediaItem.photo(path, request.getImageName()));
            }
        }

        // Videos
        if (request.getVideoId() != null && !request.getVideoId().isBlank()) {
            items.add(CreateStoryRequest.MediaItem.video(request.getVideoId()));
        }
        if (request.getVideoIds() != null) {
            for (String vid : request.getVideoIds()) {
                if (vid != null && !vid.isBlank()) items.add(CreateStoryRequest.MediaItem.video(vid));
            }
        }
        if (request.getVideoBytes() != null && request.getVideoBytes().length > 0) {
            items.add(CreateStoryRequest.MediaItem.video(request.getVideoBytes(), request.getVideoName()));
        }
        if (request.getVideoBytesList() != null) {
            for (byte[] b : request.getVideoBytesList()) {
                if (b != null && b.length > 0) items.add(CreateStoryRequest.MediaItem.video(b, request.getVideoName()));
            }
        }
        if (request.getVideoFile() != null) {
            items.add(CreateStoryRequest.MediaItem.video(request.getVideoFile()));
        }
        if (request.getVideoFiles() != null) {
            for (File f : request.getVideoFiles()) {
                if (f != null) items.add(CreateStoryRequest.MediaItem.video(f));
            }
        }
        if (request.getVideoPath() != null && !request.getVideoPath().isBlank()) {
            items.add(CreateStoryRequest.MediaItem.video(request.getVideoPath(), request.getVideoName()));
        }
        if (request.getVideoPaths() != null) {
            for (String path : request.getVideoPaths()) {
                if (path != null && !path.isBlank()) items.add(CreateStoryRequest.MediaItem.video(path, request.getVideoName()));
            }
        }

        return items;
    }

    private UploadImageResult performUploadPhoto(Bot bot, CreateStoryRequest.MediaItem item, CreateStoryRequest request) {
        String fileName = (item.getFileName() != null && !item.getFileName().isBlank()) ? item.getFileName() : request.getImageName();
        if (item.getBytes() != null && item.getBytes().length > 0) {
            return uploadImage.uploadPhoto(bot, item.getBytes(), fileName, request.getProxy(), request.getProxySampler());
        }
        if (item.getFile() != null) {
            return uploadImage.uploadPhoto(bot, item.getFile(), request.getProxy(), request.getProxySampler());
        }
        if (item.getFilePath() != null && !item.getFilePath().isBlank()) {
            return uploadImage.uploadPhoto(bot, new File(item.getFilePath()), request.getProxy(), request.getProxySampler());
        }
        return UploadImageResult.fail("EMPTY_MEDIA", "Dữ liệu ảnh rỗng", "");
    }

    private UploadVideoResult performUploadVideo(Bot bot, CreateStoryRequest.MediaItem item, CreateStoryRequest request) {
        String fileName = (item.getFileName() != null && !item.getFileName().isBlank()) ? item.getFileName() : request.getVideoName();
        if (item.getBytes() != null && item.getBytes().length > 0) {
            return uploadVideo.uploadVideo(bot, item.getBytes(), fileName, request.getProxy(), request.getProxySampler());
        }
        if (item.getFile() != null) {
            return uploadVideo.uploadVideo(bot, item.getFile(), request.getProxy(), request.getProxySampler());
        }
        if (item.getFilePath() != null && !item.getFilePath().isBlank()) {
            return uploadVideo.uploadVideo(bot, new File(item.getFilePath()), request.getProxy(), request.getProxySampler());
        }
        return UploadVideoResult.fail("EMPTY_MEDIA", "Dữ liệu video rỗng", "");
    }

    private String buildAttachmentsJson(List<CreateStoryRequest.MediaItem> items, String metadataJson) {
        if (items == null || items.isEmpty()) {
            return "[]";
        }

        String photoTemplate = DEFAULT_PHOTO_ATTACHMENT_TEMPLATE;
        String videoTemplate = DEFAULT_VIDEO_ATTACHMENT_TEMPLATE;

        if (metadataJson != null && !metadataJson.isBlank()) {
            try {
                JsonNode metaNode = JsonUtils.MAPPER.readTree(metadataJson);
                JsonNode templates = metaNode.path("attachment_templates");
                if (!templates.isMissingNode()) {
                    JsonNode photoNode = templates.path("photo");
                    if (!photoNode.isMissingNode() && !photoNode.isNull()) {
                        photoTemplate = photoNode.toString();
                    }
                    JsonNode videoNode = templates.path("video");
                    if (!videoNode.isMissingNode() && !videoNode.isNull()) {
                        videoTemplate = videoNode.toString();
                    }
                }
            } catch (Exception e) {
                log.warn("[FacebookWebCreateStory] Không thể parse metadata attachment_templates, dùng template mặc định: {}", e.getMessage());
            }
        }

        List<String> attachmentElements = new ArrayList<>();
        for (CreateStoryRequest.MediaItem item : items) {
            if (item.getType() == CreateStoryRequest.MediaType.PHOTO && item.getId() != null) {
                String resolved = ActionTemplateResolver.resolveTemplate(photoTemplate, Map.of("photoId", item.getId()));
                attachmentElements.add(resolved);
            } else if (item.getType() == CreateStoryRequest.MediaType.VIDEO && item.getId() != null) {
                String resolved = ActionTemplateResolver.resolveTemplate(videoTemplate, Map.of("videoId", item.getId()));
                attachmentElements.add(resolved);
            }
        }

        return "[" + String.join(",", attachmentElements) + "]";
    }
}
