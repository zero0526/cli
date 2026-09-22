package com.fb.cli.domains.facebook.actions;

import com.fb.cli.domains.facebook.services.ActionConfigService;
import com.fb.cli.domains.facebook.utils.ActionTemplateResolver;
import com.fb.cli.dtos.proxy.ProxyInfo;
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
@Service("facebookAppUsername")
@RequiredArgsConstructor
public class UsernameImpl implements Username {

    private static final String PLATFORM = "FACEBOOK";
    private static final String ACTION_PROVIDER = "APP_GRAPHQL";
    private static final String ACTION_CHANGE_NAME = "CHANGE_NAME";

    private final SendRequest sendRequest;
    private final BotService botService;
    private final ActionConfigService actionConfigService;

    @Override
    public boolean changeName(String firstName, String middleName, String lastName) {
        Bot bot = botService.getBot(PLATFORM);
        if (bot == null) {
            log.error("[UsernameImpl] Không tìm thấy bot Facebook khả dụng");
            return false;
        }
        return changeName(firstName, middleName, lastName, bot, null);
    }

    @Override
    public boolean changeName(String firstName, String middleName, String lastName, Bot bot, ProxyInfo proxy) {
        if (bot == null) {
            log.error("[UsernameImpl] Bot không được để trống");
            return false;
        }

        ActionConfig config = actionConfigService.getActiveConfig(PLATFORM, ACTION_CHANGE_NAME, ACTION_PROVIDER);
        if (config == null) {
            log.error("[UsernameImpl] Không tìm thấy action config cho {}/{}", PLATFORM, ACTION_CHANGE_NAME);
            return false;
        }

        try {
            String safeFirst = firstName != null ? firstName.trim() : "";
            String safeMiddle = middleName != null ? middleName.trim() : "";
            String safeLast = lastName != null ? lastName.trim() : "";

            StringBuilder fullNameBuilder = new StringBuilder(safeFirst);
            if (!safeMiddle.isEmpty()) {
                if (fullNameBuilder.length() > 0) fullNameBuilder.append(" ");
                fullNameBuilder.append(safeMiddle);
            }
            if (!safeLast.isEmpty()) {
                if (fullNameBuilder.length() > 0) fullNameBuilder.append(" ");
                fullNameBuilder.append(safeLast);
            }
            String fullName = fullNameBuilder.toString().trim();

            long random14Digits = (long) (Math.random() * 90000000000000L) + 10000000000000L;
            String latencyQplInstanceId = random14Digits + "E14";
            String clientTraceId = UUID.randomUUID().toString();

            Map<String, String> contextMap = new HashMap<>();
            contextMap.put("accessToken", bot.getToken());
            contextMap.put("userId", bot.getBotId());
            contextMap.put("firstName", safeFirst);
            contextMap.put("middleName", safeMiddle);
            contextMap.put("lastName", safeLast);
            contextMap.put("fullName", fullName);
            contextMap.put("deviceId", "d0db7ed7-f05d-4ab4-a34b-27ea5eb599f1");
            contextMap.put("latencyQplInstanceId", latencyQplInstanceId);
            contextMap.put("clientTraceId", clientTraceId);

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
            boolean success = response != null && response.contains("\"data\"") && !response.contains("\"error\"");
            if (success) {
                log.info("[UsernameImpl] Đổi tên thành công cho bot {}: full_name='{}'", bot.getBotId(), fullName);
                return true;
            } else {
                log.warn("[UsernameImpl] Đổi tên thất bại: {}", response);
                return false;
            }

        } catch (Exception e) {
            log.error("[UsernameImpl] Lỗi khi đổi tên: {}", e.getMessage(), e);
            return false;
        }
    }
}
