package com.fb.cli.domains.facebook.actions;

import com.fb.cli.domains.facebook.services.ActionConfigService;
import com.fb.cli.domains.facebook.utils.ActionTemplateResolver;
import com.fb.cli.dtos.proxy.ProxyInfo;
import com.fb.cli.entities.ActionConfig;
import com.fb.cli.entities.Bot;
import com.fb.cli.enums.FBFriendReqStatus;
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
@Service("facebookAppFriendRequest")
@RequiredArgsConstructor
public class FriendRequestImpl implements FriendRequest {

    private static final String PLATFORM = "FACEBOOK";
    private static final String ACTION_PROVIDER = "APP_GRAPHQL";
    private static final String ACTION_SEND_FRIEND_REQUEST = "SEND_FRIEND_REQUEST";

    private final SendRequest sendRequest;
    private final BotService botService;
    private final ActionConfigService actionConfigService;

    @Override
    public FBFriendReqStatus sendFriendReq(String targetFriendTarget) {
        Bot bot = botService.getBot(PLATFORM);
        if (bot == null) {
            log.error("[FriendRequestImpl] Không tìm thấy bot Facebook khả dụng");
            return FBFriendReqStatus.FRIEND_REQUEST_FAILED;
        }
        return sendFriendReq(targetFriendTarget, bot, null);
    }

    @Override
    public FBFriendReqStatus sendFriendReq(String targetFriendTarget, Bot bot, ProxyInfo proxy) {
        if (bot == null) {
            log.error("[FriendRequestImpl] Bot không được để trống");
            return FBFriendReqStatus.FRIEND_REQUEST_FAILED;
        }
        if (targetFriendTarget == null || targetFriendTarget.isBlank()) {
            log.error("[FriendRequestImpl] Target user ID không được để trống");
            return FBFriendReqStatus.USER_NOT_FOUND;
        }

        ActionConfig config = actionConfigService.getActiveConfig(PLATFORM, ACTION_SEND_FRIEND_REQUEST, ACTION_PROVIDER);
        if (config == null) {
            log.error("[FriendRequestImpl] Không tìm thấy action config cho {}/{}", PLATFORM, ACTION_SEND_FRIEND_REQUEST);
            return FBFriendReqStatus.FRIEND_REQUEST_FAILED;
        }

        try {
            long currentTimestamp = System.currentTimeMillis();
            String sessionId = "UFS-" + UUID.randomUUID() + "-fg-2";
            String clientTraceId = UUID.randomUUID().toString();
            String dspCorrelationId = "1&" + UUID.randomUUID() + ".1477&137";
            String connUuid = UUID.randomUUID().toString();

            Map<String, String> contextMap = new HashMap<>();
            contextMap.put("accessToken", bot.getToken());
            contextMap.put("userId", bot.getBotId());
            contextMap.put("targetUserId", targetFriendTarget.trim());
            contextMap.put("sessionId", sessionId);
            contextMap.put("clientTraceId", clientTraceId);
            contextMap.put("dspCorrelationId", dspCorrelationId);
            contextMap.put("connUuid", connUuid);
            contextMap.put("timestamp", String.valueOf(currentTimestamp));

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
            if (response == null || response.isBlank()) {
                log.warn("[FriendRequestImpl] Response rỗng khi gửi lời mời kết bạn tới {}", targetFriendTarget);
                return FBFriendReqStatus.FRIEND_REQUEST_FAILED;
            }

            // Phân loại kết quả trả về theo enum FBFriendReqStatus
            if (response.contains("DUPLICATE_REQUEST")) {
                log.warn("[FriendRequestImpl] Lời mời kết bạn bị trùng lặp (DUPLICATE_REQUEST) với {}", targetFriendTarget);
                return FBFriendReqStatus.DUPLICATE_REQUEST;
            }
            if (response.contains("BLOCKED_BY_USER")) {
                log.warn("[FriendRequestImpl] Bị chặn bởi user {} (BLOCKED_BY_USER)", targetFriendTarget);
                return FBFriendReqStatus.BLOCKED_BY_USER;
            }
            if (response.contains("USER_NOT_FOUND")) {
                log.warn("[FriendRequestImpl] Không tìm thấy user {} (USER_NOT_FOUND)", targetFriendTarget);
                return FBFriendReqStatus.USER_NOT_FOUND;
            }
            if (response.contains("FRIEND_REQUEST_BLOCKED")) {
                log.warn("[FriendRequestImpl] Tính năng kết bạn bị chặn (FRIEND_REQUEST_BLOCKED)");
                return FBFriendReqStatus.FRIEND_REQUEST_BLOCKED;
            }
            if (response.contains("FriendRequestFailureException")) {
                log.warn("[FriendRequestImpl] Lỗi ngoại lệ FriendRequestFailureException với {}", targetFriendTarget);
                return FBFriendReqStatus.FriendRequestFailureException;
            }

            // Kiểm tra thành công
            boolean isSuccess = response.contains("friend_request_send")
                    || response.contains("OUTGOING_REQUEST")
                    || (response.contains("\"data\"") && !response.contains("\"error\""));

            if (isSuccess) {
                log.info("[FriendRequestImpl] Gửi lời mời kết bạn thành công từ bot {} tới target {}", bot.getBotId(), targetFriendTarget);
                return FBFriendReqStatus.SUCCESS;
            } else {
                log.warn("[FriendRequestImpl] Gửi kết bạn thất bại: {}", response);
                return FBFriendReqStatus.FRIEND_REQUEST_FAILED;
            }

        } catch (Exception e) {
            log.error("[FriendRequestImpl] Lỗi khi gửi lời mời kết bạn tới {}: {}", targetFriendTarget, e.getMessage(), e);
            return FBFriendReqStatus.FRIEND_REQUEST_FAILED;
        }
    }
}
