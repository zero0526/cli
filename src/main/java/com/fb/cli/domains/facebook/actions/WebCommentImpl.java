package com.fb.cli.domains.facebook.actions;

import com.fb.cli.domains.facebook.services.ActionConfigService;
import com.fb.cli.domains.facebook.services.FacebookWebContextService;
import com.fb.cli.domains.facebook.utils.ActionTemplateResolver;
import com.fb.cli.domains.facebook.utils.GenericResponseParser;
import com.fb.cli.dtos.facebook.CommentRequest;
import com.fb.cli.dtos.facebook.CommentResult;
import com.fb.cli.dtos.facebook.FacebookWebContext;
import com.fb.cli.entities.ActionConfig;
import com.fb.cli.entities.Bot;
import com.fb.cli.services.external.HttpRequestCall;
import com.fb.cli.services.external.SendRequest;
import com.fb.cli.services.internal.BotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service("facebookWebComment")
@RequiredArgsConstructor
public class WebCommentImpl implements Comment {

    private static final String PLATFORM = "FACEBOOK";
    private static final String ACTION_TYPE = "COMMENT";
    private static final String ACTION_PROVIDER = "WEB_GRAPHQL";

    private final SendRequest sendRequest;
    private final BotService botService;
    private final FacebookWebContextService contextService;
    private final ActionConfigService actionConfigService;

    @Override
    public CommentResult sendComment(CommentRequest request) {
        if (request == null || request.getTargetId() == null || request.getContent() == null) {
            return CommentResult.fail(null, "INVALID_REQUEST", "TargetId và Content không được để trống", "");
        }

        String targetId = request.getTargetId();
        String content = request.getContent();

        // 1. Resolve Bot
        Bot bot = request.getBot();
        if (bot == null && request.getBotConfig() != null) {
            bot = botService.getBot(request.getBotConfig());
        }

        if (bot == null) {
            log.error("[FacebookWebComment] Không tìm thấy Bot để gửi comment");
            return CommentResult.fail(targetId, "BOT_NOT_FOUND", "Không tìm thấy Bot khả dụng", "");
        }

        // 2. Resolve Action Config từ Database / Redis Cache
        ActionConfig config = actionConfigService.getActiveConfig(PLATFORM, ACTION_TYPE, ACTION_PROVIDER);
        if (config == null) {
            log.error("[FacebookWebComment] Không tìm thấy cấu hình active trong DB cho {}/{}/{}", PLATFORM, ACTION_TYPE, ACTION_PROVIDER);
            return CommentResult.fail(targetId, "CONFIG_NOT_FOUND", "Không tìm thấy cấu hình action trong database", "");
        }

        // 3. Resolve Web Context (userId, jazoest, lsd, dtsg)
        FacebookWebContext context = contextService.resolveContext(bot, request.getProxy(), request.getProxySampler());
        if (context == null) {
            log.error("[FacebookWebComment] Không thể lấy Web context (lsd/dtsg) cho bot {}", bot.getBotId());
            return CommentResult.fail(targetId, "AUTH_FAILED", "Không thể lấy Web tokens", "");
        }

        // 4. Build Context Variables & Dynamic Templates
        String feedbackId = Base64.getEncoder().encodeToString(("feedback:" + targetId).getBytes(StandardCharsets.UTF_8));
        String sessionId = UUID.randomUUID().toString();
        String idempotenceId = UUID.randomUUID().toString();
        long timestamp = System.currentTimeMillis();

        Map<String, String> contextMap = new HashMap<>();
        contextMap.put("userId", context.getUserId());
        contextMap.put("targetId", targetId);
        contextMap.put("base64FeedbackId", feedbackId);
        contextMap.put("content", content);
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

            // 6. Parse response qua Extractor Rules từ DB
            GenericResponseParser.ParseResult parseResult = GenericResponseParser.parse(responseText, config.getExtractorRules());
            if (parseResult.isSuccess()) {
                String commentId = parseResult.getExtractedId() != null ? parseResult.getExtractedId() : "";
                log.info("[FacebookWebComment] Gửi comment thành công cho target {}: commentId={}", targetId, commentId);
                return CommentResult.ok(targetId, commentId, responseText);
            } else {
                log.warn("[FacebookWebComment] Gửi comment thất bại: code={}, message={}", parseResult.getErrorCode(), parseResult.getErrorMessage());
                return CommentResult.fail(targetId, parseResult.getErrorCode(), parseResult.getErrorMessage(), responseText);
            }

        } catch (Exception e) {
            log.error("[FacebookWebComment] Lỗi khi gửi comment cho targetId {}: {}", targetId, e.getMessage(), e);
            return CommentResult.fail(targetId, "NETWORK_ERROR", e.getMessage(), "");
        }
    }
}
