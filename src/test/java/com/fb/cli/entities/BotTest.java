package com.fb.cli.entities;

import com.fb.cli.enums.BotStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BotTest {

    @Test
    @DisplayName("Khởi tạo Bot entity với Builder và kiểm tra các thuộc tính")
    void testBotEntityBuilder() {
        UUID id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        Bot bot = Bot.builder()
                .id(id)
                .botId("fb_bot_001")
                .botName("Facebook Crawler Bot 1")
                .platform("facebook")
                .status(BotStatus.ACTIVE)
                .proxyUrl("http://103.15.22.1:8080")
                .phone("0912345678")
                .email("bot01@example.com")
                .password("secret123")
                .cookies("c_user=10001; xs=abc;")
                .token("EAAB...")
                .refreshToken("r_token_123")
                .tokenExpiresAt(now.plusDays(30))
                .sessionData("{\"device_id\":\"dev-123\"}")
                .userAgent("Mozilla/5.0")
                .settings("{\"retry_count\":3}")
                .metadata("{\"group\":\"v1\"}")
                .lastLoginAt(now)
                .lastError(null)
                .createdAt(now)
                .lastModifiedAt(now)
                .build();

        assertEquals(id, bot.getId());
        assertEquals("fb_bot_001", bot.getBotId());
        assertEquals("Facebook Crawler Bot 1", bot.getBotName());
        assertEquals("facebook", bot.getPlatform());
        assertEquals(BotStatus.ACTIVE, bot.getStatus());
        assertEquals("http://103.15.22.1:8080", bot.getProxyUrl());
        assertEquals("0912345678", bot.getPhone());
        assertEquals("bot01@example.com", bot.getEmail());
        assertEquals("secret123", bot.getPassword());
        assertEquals("c_user=10001; xs=abc;", bot.getCookies());
        assertEquals("EAAB...", bot.getToken());
        assertEquals("r_token_123", bot.getRefreshToken());
        assertEquals("{\"device_id\":\"dev-123\"}", bot.getSessionData());
        assertEquals("{\"retry_count\":3}", bot.getSettings());
        assertEquals("{\"group\":\"v1\"}", bot.getMetadata());
    }

    @Test
    @DisplayName("Kiểm tra giá trị mặc định của Bot entity")
    void testBotEntityDefaults() {
        Bot bot = new Bot();
        assertEquals(BotStatus.ACTIVE, bot.getStatus());
        assertEquals("{}", bot.getSettings());
        assertEquals("{}", bot.getMetadata());
    }
}
