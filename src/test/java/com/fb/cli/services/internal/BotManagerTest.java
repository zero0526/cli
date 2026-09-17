package com.fb.cli.services.internal;

import com.fb.cli.dtos.bot.BotSamplingCfg;
import com.fb.cli.entities.Bot;
import com.fb.cli.enums.BotStatus;
import com.fb.cli.repositories.BotRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BotManagerTest {

    @Mock
    private BotRepository botRepository;

    @InjectMocks
    private BotManager botManager;

    @Test
    @DisplayName("BotManager lấy Bot thông qua BotRandomSampler đẩy xuống DB")
    void testGetBotViaRandomSampler() {
        Bot bot = Bot.builder()
                .id(UUID.randomUUID())
                .botId("fb_bot_01")
                .platform("facebook")
                .status(BotStatus.ACTIVE)
                .build();

        BotSamplingCfg cfg = new BotSamplingCfg(1, 42L, "facebook", BotStatus.ACTIVE);

        when(botRepository.countEligibleBots("facebook", BotStatus.ACTIVE)).thenReturn(1L);
        when(botRepository.findEligibleBots(eq("facebook"), eq(BotStatus.ACTIVE), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(bot)));

        Bot result = botManager.getBot(cfg);

        assertNotNull(result);
        assertEquals("fb_bot_01", result.getBotId());
        assertEquals("facebook", result.getPlatform());
        verify(botRepository, times(1)).countEligibleBots("facebook", BotStatus.ACTIVE);
        verify(botRepository, times(1)).findEligibleBots(eq("facebook"), eq(BotStatus.ACTIVE), any(PageRequest.class));
    }

    @Test
    @DisplayName("BotManager trả về null khi không có Bot nào khả dụng")
    void testGetBotReturnsNullWhenEmpty() {
        BotSamplingCfg cfg = new BotSamplingCfg(1, 42L, "facebook", BotStatus.ACTIVE);
        when(botRepository.countEligibleBots("facebook", BotStatus.ACTIVE)).thenReturn(0L);

        Bot result = botManager.getBot(cfg);

        assertNull(result);
        verify(botRepository, times(1)).countEligibleBots("facebook", BotStatus.ACTIVE);
        verify(botRepository, never()).findEligibleBots(any(), any(), any(PageRequest.class));
    }

    @Test
    @DisplayName("BotRandomSampler lấy từ In-Memory Pool")
    void testBotRandomSamplerInMemory() {
        Bot b1 = Bot.builder().id(UUID.randomUUID()).botId("b1").build();
        Bot b2 = Bot.builder().id(UUID.randomUUID()).botId("b2").build();

        BotSamplingCfg cfg = new BotSamplingCfg(1, 100L);
        BotRandomSampler sampler = new BotRandomSampler(cfg, List.of(b1, b2));

        List<Bot> sampledBots = sampler.sample();

        assertNotNull(sampledBots);
        assertEquals(1, sampledBots.size());
    }
}
