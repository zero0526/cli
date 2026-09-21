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

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service("facebookAppBirthDay")
@RequiredArgsConstructor
public class BirthDayImpl implements BirthDay {

    private static final String PLATFORM = "FACEBOOK";
    private static final String ACTION_PROVIDER = "APP_GRAPHQL";
    private static final String ACTION_UPDATE_BIRTHDAY = "UPDATE_BIRTHDAY";

    private final SendRequest sendRequest;
    private final BotService botService;
    private final ActionConfigService actionConfigService;

    @Override
    public boolean updateBirthDay() {
        Bot bot = botService.getBot(PLATFORM);
        if (bot == null) {
            log.error("[BirthDayImpl] Không tìm thấy bot Facebook khả dụng");
            return false;
        }
        // Mặc định chọn một ngày sinh ngẫu nhiên hợp lệ (ví dụ: tuổi 20-28)
        int randomYear = 1996 + (int) (Math.random() * 8);
        int randomMonth = 1 + (int) (Math.random() * 12);
        int randomDay = 1 + (int) (Math.random() * 28);
        return updateBirthDay(randomDay, randomMonth, randomYear, bot, null);
    }

    @Override
    public boolean updateBirthDay(int day, int month, int year, Bot bot, ProxyInfo proxy) {
        if (bot == null) {
            log.error("[BirthDayImpl] Bot không được để trống");
            return false;
        }

        ActionConfig config = actionConfigService.getActiveConfig(PLATFORM, ACTION_UPDATE_BIRTHDAY, ACTION_PROVIDER);
        if (config == null) {
            log.error("[BirthDayImpl] Không tìm thấy action config cho {}/{}", PLATFORM, ACTION_UPDATE_BIRTHDAY);
            return false;
        }

        try {
            LocalDate birthDate = LocalDate.of(year, month, day);
            long timestamp = birthDate.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
            String connUuid = UUID.randomUUID().toString().replace("-", "");
            String clientTraceId = UUID.randomUUID().toString();

            Map<String, String> contextMap = new HashMap<>();
            contextMap.put("accessToken", bot.getToken());
            contextMap.put("userId", bot.getBotId());
            contextMap.put("birthdayTimestamp", String.valueOf(timestamp));
            contextMap.put("connUuid", connUuid);
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

            // Kiểm tra lỗi theo pattern Facebook Bloks
            boolean hasError = response == null
                    || response.contains("Đã xảy ra lỗi")
                    || response.contains("OpenDialog")
                    || response.contains("Hãy thử thay đổi cài đặt")
                    || response.contains("\"error\"");

            boolean hasSuccess = response != null && (
                    response.contains("is_mutation_successful")
                            || response.contains("Đã cập nhật ngày sinh")
                            || response.contains("PopScreen")
                            || response.contains("ShowToastV2")
                            || (response.contains("\"data\"") && !response.contains("\"error\""))
            );

            if (!hasError && hasSuccess) {
                log.info("[BirthDayImpl] Cập nhật ngày sinh thành công cho bot {}: {}/{}/{}", bot.getBotId(), day, month, year);
                return true;
            } else {
                log.warn("[BirthDayImpl] Cập nhật ngày sinh thất bại: {}", response);
                return false;
            }

        } catch (Exception e) {
            log.error("[BirthDayImpl] Lỗi khi cập nhật ngày sinh: {}", e.getMessage(), e);
            return false;
        }
    }
}
