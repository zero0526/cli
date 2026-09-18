package com.fb.cli.domains.facebook.actions;

import com.fb.cli.domains.facebook.services.ActionConfigService;
import com.fb.cli.domains.facebook.services.FacebookWebContextService;
import com.fb.cli.domains.facebook.utils.ActionTemplateResolver;
import com.fb.cli.domains.facebook.utils.GenericResponseParser;
import com.fb.cli.dtos.facebook.FacebookWebContext;
import com.fb.cli.dtos.facebook.ShareRequest;
import com.fb.cli.dtos.facebook.ShareResult;
import com.fb.cli.entities.ActionConfig;
import com.fb.cli.entities.Bot;
import com.fb.cli.services.external.HttpRequestCall;
import com.fb.cli.services.external.SendRequest;
import com.fb.cli.services.internal.BotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service("facebookWebShare")
@RequiredArgsConstructor
public class FacebookWebShareImpl implements Share {

    private static final String PLATFORM = "FACEBOOK";
    private static final String ACTION_TYPE = "SHARE";
    private static final String ACTION_PROVIDER = "WEB_GRAPHQL";

    private final SendRequest sendRequest;
    private final BotService botService;
    private final FacebookWebContextService contextService;
    private final ActionConfigService actionConfigService;

    @Override
    public ShareResult sendShare(ShareRequest request) {
        if (request == null || request.getTargetId() == null || request.getTargetId().isBlank()) {
            return ShareResult.fail(null, "INVALID_REQUEST", "TargetId (numeric post id) không được để trống", "");
        }

        String targetId = request.getTargetId();
        String content = request.getContent() != null ? request.getContent() : "";
        String privacy = request.getPrivacy() != null ? request.getPrivacy() : "FRIENDS";

        // 1. Resolve Bot
        Bot bot = request.getBot();
        if (bot == null && request.getBotConfig() != null) {
            bot = botService.getBot(request.getBotConfig());
        }

        if (bot == null) {
            log.error("[FacebookWebShare] Không tìm thấy Bot để thực hiện share");
            return ShareResult.fail(targetId, "BOT_NOT_FOUND", "Không tìm thấy Bot khả dụng", "");
        }

        // 2. Resolve Action Config từ Database / Redis Cache
        ActionConfig config = actionConfigService.getActiveConfig(PLATFORM, ACTION_TYPE, ACTION_PROVIDER);
        if (config == null) {
            log.error("[FacebookWebShare] Không tìm thấy cấu hình active trong DB cho {}/{}/{}", PLATFORM, ACTION_TYPE, ACTION_PROVIDER);
            return ShareResult.fail(targetId, "CONFIG_NOT_FOUND", "Không tìm thấy cấu hình action trong database", "");
        }

        // 3. Resolve Web Context (userId, jazoest, lsd, dtsg)
        FacebookWebContext context = contextService.resolveContext(bot, request.getProxy(), request.getProxySampler());
        if (context == null) {
            log.error("[FacebookWebShare] Không thể lấy Web context (lsd/dtsg) cho bot {}", bot.getBotId());
            return ShareResult.fail(targetId, "AUTH_FAILED", "Không thể lấy Web tokens", "");
        }

        // 4. Build Context Variables & Dynamic Templates
        String sessionId = UUID.randomUUID().toString();
        String idempotenceId = UUID.randomUUID().toString();
        long timestamp = System.currentTimeMillis();

        Map<String, String> contextMap = new HashMap<>();
        contextMap.put("userId", context.getUserId());
        contextMap.put("targetId", targetId);
        contextMap.put("content", content);
        contextMap.put("privacy", privacy);
        contextMap.put("sessionId", sessionId);
        contextMap.put("idempotenceId", idempotenceId);
        contextMap.put("timestamp", String.valueOf(timestamp));
        contextMap.put("lsdToken", context.getLsdToken());
        contextMap.put("dtsgToken", context.getDtsgToken());
        contextMap.put("jazoest", context.getJazoest());

        // Resolve Payload Variables template -> Gán vào PAYLOAD_VARIABLES
        String resolvedPayload = ActionTemplateResolver.resolveTemplate(config.getPayloadTemplate(), contextMap);
        contextMap.put("PAYLOAD_VARIABLES", resolvedPayload);

        // Resolve Form Data & Headers
        Map<String, String> headers = ActionTemplateResolver.resolveMapTemplate(config.getHeaders(), contextMap);
        Map<String, String> formData = ActionTemplateResolver.resolveMapTemplate(config.getFormParams(), contextMap);

        // 5. Send HTTP Request
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

            // 6. Parse response bằng extractor rules từ Database
            GenericResponseParser.ParseResult parseResult = GenericResponseParser.parse(responseText, config.getExtractorRules());
            if (parseResult.isSuccess()) {
                String storyId = parseResult.getExtractedId() != null ? parseResult.getExtractedId() : targetId;
                log.info("[FacebookWebShare] Share bài viết {} thành công! Story ID: {}", targetId, storyId);
                return ShareResult.ok(targetId, storyId, responseText);
            } else {
                log.error("[FacebookWebShare] Lỗi từ Facebook GraphQL: code={}, msg={}", parseResult.getErrorCode(), parseResult.getErrorMessage());
                return ShareResult.fail(targetId, parseResult.getErrorCode(), parseResult.getErrorMessage(), responseText);
            }

        } catch (Exception e) {
            log.error("[FacebookWebShare] Ngoại lệ khi gửi share cho target {}: {}", targetId, e.getMessage(), e);
            return ShareResult.fail(targetId, "EXCEPTION", e.getMessage(), "");
        }
    }
}
