package com.fb.cli.services.internal;

import com.fb.cli.dtos.bot.BotSamplingCfg;
import com.fb.cli.entities.Bot;
import com.fb.cli.enums.BotStatus;
import com.fb.cli.repositories.BotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class BotManager {

    private final BotRepository botRepository;

    /**
     * Lấy 1 Bot phù hợp dựa vào BotSamplingCfg.
     * Tự động khởi tạo BotRandomSampler tương ứng và điều phối lấy bot.
     */
    public Bot getBot(BotSamplingCfg config) {
        if (config == null) {
            log.error("[BotManager] BotSamplingCfg must not be null");
            return null;
        }
        return getBot(new BotRandomSampler(this.botRepository, config));
    }

    /**
     * Lấy 1 Bot ngẫu nhiên theo Platform (với status mặc định là ACTIVE).
     */
    public Bot getBot(String platform) {
        return getBot(new BotSamplingCfg(1, 0L, platform, BotStatus.ACTIVE));
    }

    /**
     * Lấy 1 Bot phù hợp dựa vào BotSampleStrategy.
     * Trả về null kèm log error nếu không tìm thấy Bot nào thỏa mãn.
     */
    public Bot getBot(BotSampleStrategy sampler) {
        List<Bot> bots = getBots(sampler);
        if (bots == null || bots.isEmpty()) {
            return null;
        }
        return bots.get(0);
    }

    /**
     * Lấy danh sách Bot theo BotSamplingCfg.
     */
    public List<Bot> getBots(BotSamplingCfg config) {
        if (config == null) {
            log.error("[BotManager] BotSamplingCfg must not be null");
            return Collections.emptyList();
        }
        List<Bot> result = getBots(new BotRandomSampler(this.botRepository, config));
        return result != null ? result : Collections.emptyList();
    }

    /**
     * Lấy danh sách Bot theo BotSampleStrategy.
     * BotManager đóng vai trò Coordinator (Điều phối viên):
     * 1. Phân việc cho Sampler lấy danh sách Bot lên từ DB/Pool.
     * 2. Kiểm tra tính hợp lệ và trả về cho caller.
     */
    public List<Bot> getBots(BotSampleStrategy sampler) {
        if (sampler == null) {
            log.error("[BotManager] BotSampleStrategy must not be null");
            return Collections.emptyList();
        }

        List<Bot> sampledBots = sampler.sample();
        if (sampledBots == null || sampledBots.isEmpty()) {
            log.error("[BotManager] Sampler không tìm thấy Bot nào thỏa mãn điều kiện");
            return Collections.emptyList();
        }

        return sampledBots;
    }

    /**
     * Tìm Bot theo botId trên platform
     */
    public Optional<Bot> findByBotId(String botId) {
        return botRepository.findByBotId(botId);
    }
}
