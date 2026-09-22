package com.fb.cli.domains.facebook.services;

import com.fb.cli.dtos.facebook.FacebookWebContext;
import com.fb.cli.dtos.proxy.ProxyInfo;
import com.fb.cli.dtos.proxy.RandomSamplingCfg;
import com.fb.cli.entities.Bot;
import com.fb.cli.persistences.redis.RedisService;
import com.fb.cli.services.external.HttpRequestCall;
import com.fb.cli.services.external.SendRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class FacebookWebContextService {

    private static final Pattern C_USER_PATTERN = Pattern.compile("c_user=(\\d+)");
    private static final Pattern JAZOEST_PATTERN = Pattern.compile("jazoest=(\\d+)");
    private static final Pattern LSD_PATTERN_1 = Pattern.compile("\"token\":\"([^\"]+)\"},323\\]");
    private static final Pattern LSD_PATTERN_2 = Pattern.compile("\\[\"LSD\",\\[\\],\\{\"token\":\"([^\"]+)\"");
    private static final Pattern DTSG_PATTERN_1 = Pattern.compile("\"dtsg\":\\{\"token\":\"([^\"]+)\"");
    private static final Pattern DTSG_PATTERN_2 = Pattern
            .compile("\"DTSGInitialData\",\\[\\],\\{\"token\":\"([^\"]+)\"");

    private final SendRequest sendRequest;
    private final RedisService redisService;

    /**
     * Resolve FacebookWebContext (userId, jazoest, lsd, dtsg) từ Redis cache hoặc
     * bóc tách trực tiếp từ www.facebook.com
     */
    public FacebookWebContext resolveContext(Bot bot, ProxyInfo proxy, RandomSamplingCfg proxySampler) {
        if (bot == null) {
            log.error("[FacebookWebContext] Bot không được để trống");
            return null;
        }

        String userId = extractUserId(bot);
        if (userId == null || userId.isBlank()) {
            log.error("[FacebookWebContext] Không thể xác định userId cho bot: {}", bot.getBotId());
            return null;
        }

        String cacheKey = "fb:web_context:" + userId;

        // 1. Kiểm tra cache Redis
        FacebookWebContext cached = redisService.get(cacheKey, FacebookWebContext.class);
        if (cached != null && cached.getDtsgToken() != null && cached.getLsdToken() != null) {
            log.debug("[FacebookWebContext] Đã tải context từ Redis cache cho user {}", userId);
            return cached;
        }

        // 2. Kéo trực tiếp từ www.facebook.com
        log.info("[FacebookWebContext] Đang lấy web tokens mới từ Facebook cho user {}...", userId);
        try {
            HttpRequestCall call = sendRequest.get("https://www.facebook.com")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("sec-fetch-site", "none")
                    .header("sec-fetch-mode", "navigate")
                    .header("sec-fetch-dest", "document")
                    .withBot(bot);

            if (proxy != null) {
                call.withProxy(proxy);
            } else if (proxySampler != null) {
                call.withProxy(proxySampler);
            }

            String html = call.execute();
            if (html == null || html.isBlank()) {
                log.error("[FacebookWebContext] Nhận HTML rỗng khi gọi facebook.com");
                return null;
            }

            String jazoest = matchFirst(JAZOEST_PATTERN, html);
            String lsd = matchFirst(LSD_PATTERN_1, html);
            if (lsd == null) {
                lsd = matchFirst(LSD_PATTERN_2, html);
            }

            String dtsg = matchFirst(DTSG_PATTERN_1, html);
            if (dtsg == null) {
                dtsg = matchFirst(DTSG_PATTERN_2, html);
            }

            if (dtsg == null || lsd == null) {
                log.error("[FacebookWebContext] Không tìm thấy dtsg/lsd token trong phản hồi HTML (dtsg={}, lsd={})",
                        dtsg, lsd);
                return null;
            }

            FacebookWebContext context = FacebookWebContext.builder()
                    .userId(userId)
                    .jazoest(jazoest != null ? jazoest : "2")
                    .lsdToken(lsd)
                    .dtsgToken(dtsg)
                    .build();

            // Cache vào Redis với TTL 12 giờ
            redisService.set(cacheKey, context, 1, TimeUnit.HOURS);
            log.info(
                    "[FacebookWebContext] Lấy và cache tokens thành công cho user {}: jazoest={}, lsd={}..., dtsg={}...",
                    userId, context.getJazoest(), lsd.substring(0, Math.min(8, lsd.length())),
                    dtsg.substring(0, Math.min(8, dtsg.length())));

            return context;
        } catch (Exception e) {
            log.error("[FacebookWebContext] Lỗi khi lấy web tokens từ facebook.com: {}", e.getMessage(), e);
            return null;
        }
    }

    public String extractUserId(Bot bot) {
        if (bot.getCookies() != null && !bot.getCookies().isBlank()) {
            Matcher matcher = C_USER_PATTERN.matcher(bot.getCookies());
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        if (bot.getBotId() != null && bot.getBotId().matches("\\d+")) {
            return bot.getBotId();
        }
        return null;
    }

    private String matchFirst(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
