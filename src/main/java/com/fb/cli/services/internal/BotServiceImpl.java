package com.fb.cli.services.internal;

import com.fb.cli.dtos.bot.BotDetermineCfg;
import com.fb.cli.dtos.bot.BotSamplingConfig;
import com.fb.cli.entities.Bot;
import com.fb.cli.enums.BotStatus;
import com.fb.cli.repositories.BotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotServiceImpl implements BotService {

    private final BotRepository botRepository;
    private final Random random = new Random();

    @Override
    public Bot getBot(String platform) {
        List<Bot> bots = getBots(platform, 1);
        return bots.isEmpty() ? null : bots.get(0);
    }

    @Override
    public Bot getBot(BotSamplingConfig config) {
        if (config == null) {
            return null;
        }
        if (config instanceof BotDetermineCfg determineCfg) {
            if (determineCfg.id() != null) {
                return botRepository.findById(determineCfg.id()).orElse(null);
            }
            if (determineCfg.bot_id() != null) {
                return botRepository.findByBotId(determineCfg.bot_id()).orElse(null);
            }
        }
        return getBot(config.platform());
    }

    @Override
    public List<Bot> getBots(String platform, int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }

        long totalEligible = botRepository.countEligibleBots(platform, BotStatus.ACTIVE);
        if (totalEligible <= 0) {
            log.warn("[BotRandomSampler] Không tìm thấy Bot ACTIVE nào cho platform: {}", platform);
            return Collections.emptyList();
        }

        int sampleCount = (int) Math.min(limit, totalEligible);
        Set<Integer> offsets = new LinkedHashSet<>();
        while (offsets.size() < sampleCount) {
            offsets.add(random.nextInt((int) totalEligible));
        }

        List<Bot> result = new ArrayList<>();
        for (int offset : offsets) {
            Page<Bot> page = botRepository.findEligibleBots(platform, BotStatus.ACTIVE, PageRequest.of(offset, 1));
            if (page.hasContent()) {
                result.add(page.getContent().get(0));
            }
        }
        return result;
    }
}

