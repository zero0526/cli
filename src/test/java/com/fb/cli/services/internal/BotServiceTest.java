package com.fb.cli.services.internal;

import com.fb.cli.entities.Bot;
import com.fb.cli.enums.BotStatus;
import com.fb.cli.repositories.BotRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class BotServiceTest {

    @Test
    @DisplayName("getBot trả về Bot khi có bot ACTIVE trong DB")
    void testGetBotSuccess() {
        BotRepository mockRepo = Mockito.mock(BotRepository.class);
        Bot bot = Bot.builder()
                .botId("bot_01")
                .platform("facebook")
                .status(BotStatus.ACTIVE)
                .token("fb_token_123")
                .build();

        when(mockRepo.countEligibleBots("facebook", BotStatus.ACTIVE)).thenReturn(5L);
        when(mockRepo.findEligibleBots(eq("facebook"), eq(BotStatus.ACTIVE), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(bot)));

        BotServiceImpl sampler = new BotServiceImpl(mockRepo);
        Bot result = sampler.getBot("facebook");

        assertNotNull(result);
        assertEquals("bot_01", result.getBotId());
        assertEquals("fb_token_123", result.getToken());
    }

    @Test
    @DisplayName("getBot trả về null khi không có bot ACTIVE nào trong DB")
    void testGetBotEmpty() {
        BotRepository mockRepo = Mockito.mock(BotRepository.class);
        when(mockRepo.countEligibleBots("facebook", BotStatus.ACTIVE)).thenReturn(0L);

        BotServiceImpl sampler = new BotServiceImpl(mockRepo);
        Bot result = sampler.getBot("facebook");

        assertNull(result);
    }

    @Test
    @DisplayName("getBots lấy đúng số lượng bot yêu cầu")
    void testGetBotsWithLimit() {
        BotRepository mockRepo = Mockito.mock(BotRepository.class);
        Bot bot1 = Bot.builder().botId("bot_01").platform("facebook").status(BotStatus.ACTIVE).build();

        when(mockRepo.countEligibleBots("facebook", BotStatus.ACTIVE)).thenReturn(10L);
        when(mockRepo.findEligibleBots(eq("facebook"), eq(BotStatus.ACTIVE), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(bot1)));

        BotServiceImpl sampler = new BotServiceImpl(mockRepo);
        List<Bot> bots = sampler.getBots("facebook", 2);

        assertEquals(2, bots.size());
    }

    @Test
    @DisplayName("getBot với BotDetermineCfg tìm theo botId")
    void testGetBotWithDetermineCfg() {
        BotRepository mockRepo = Mockito.mock(BotRepository.class);
        Bot bot = Bot.builder().botId("target_bot").platform("facebook").build();
        when(mockRepo.findByBotId("target_bot")).thenReturn(java.util.Optional.of(bot));

        BotServiceImpl service = new BotServiceImpl(mockRepo);
        Bot result = service.getBot(new com.fb.cli.dtos.bot.BotDetermineCfg("facebook", "target_bot"));

        assertNotNull(result);
        assertEquals("target_bot", result.getBotId());
    }
}

