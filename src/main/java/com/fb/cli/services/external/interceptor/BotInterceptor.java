package com.fb.cli.services.external.interceptor;

import com.fb.cli.entities.Bot;
import com.fb.cli.services.internal.BotManager;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.function.BiConsumer;

@Slf4j
@Component
public class BotInterceptor implements RequestInterceptor {

    private final ObjectProvider<BotManager> botManagerProvider;
    private BotManager directBotManager;

    @Autowired
    public BotInterceptor(@Lazy ObjectProvider<BotManager> botManagerProvider) {
        this.botManagerProvider = botManagerProvider;
    }

    public BotInterceptor(BotManager botManager) {
        this.botManagerProvider = null;
        this.directBotManager = botManager;
    }

    private BotManager getBotManager() {
        if (this.directBotManager != null) {
            return this.directBotManager;
        }
        if (this.botManagerProvider != null) {
            return this.botManagerProvider.getIfAvailable();
        }
        return null;
    }

    @Override
    public int getOrder() {
        return -50; // Chạy sau ProxyInterceptor (-100) nhưng trước custom headers
    }

    @Override
    public Request.Builder intercept(Request.Builder builder, RequestOptions options) {
        if (options == null) {
            return builder;
        }

        // 1. Tự động resolve Bot nếu chưa có và có cấu hình sampler
        if (options.getBot() == null) {
            resolveBot(options);
        }

        Bot bot = options.getBot();
        if (bot == null) {
            return builder;
        }

        // 2. Thực thi lambda function do người dùng tự định nghĩa để map các trường của Bot vào Request
        BiConsumer<Bot, Request.Builder> mapper = options.getBotMapper();
        if (mapper != null) {
            try {
                mapper.accept(bot, builder);
            } catch (Exception e) {
                log.error("[BotInterceptor] Lỗi khi thực thi lambda mapper cho bot {}: {}", bot.getBotId(), e.getMessage(), e);
            }
        } else {
            // Ánh xạ mặc định nếu không truyền lambda
            applyDefaultBotMapping(bot, builder);
        }

        return builder;
    }

    /**
     * Kéo Bot từ BotManager dựa vào sampler trong options
     */
    public Bot resolveBot(RequestOptions options) {
        if (options == null) {
            return null;
        }

        BotManager botManager = getBotManager();
        if (botManager == null) {
            log.error("[BotInterceptor] BotManager chưa được khởi tạo hoặc không khả dụng");
            return null;
        }

        Bot bot = null;
        if (options.getBotSampleStrategy() != null) {
            bot = botManager.getBot(options.getBotSampleStrategy());
        } else if (options.getBotSampler() != null) {
            bot = botManager.getBot(options.getBotSampler());
        }

        if (bot != null) {
            log.info("[BotInterceptor] Đã lấy Bot thành công: {} [{}]", bot.getBotId(), bot.getPlatform());
            options.setBot(bot);
        } else if (options.getBotSampler() != null || options.getBotSampleStrategy() != null) {
            log.error("[BotInterceptor] Không tìm thấy Bot nào thỏa mãn điều kiện sampler");
        }

        return bot;
    }

    private void applyDefaultBotMapping(Bot bot, Request.Builder builder) {
        if (bot.getToken() != null && !bot.getToken().isBlank()) {
            builder.addHeader("Authorization", "Bearer " + bot.getToken());
        }
        if (bot.getCookies() != null && !bot.getCookies().isBlank()) {
            builder.addHeader("Cookie", bot.getCookies());
        }
        if (bot.getUserAgent() != null && !bot.getUserAgent().isBlank()) {
            builder.header("User-Agent", bot.getUserAgent());
        }
    }
}
