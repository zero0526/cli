package com.fb.cli.services.internal;

import com.fb.cli.dtos.bot.BotSamplingConfig;
import com.fb.cli.entities.Bot;

import java.util.List;

public interface BotService {

    /**
     * Lấy 1 Bot phù hợp cho platform
     */
    Bot getBot(String platform);

    /**
     * Lấy 1 Bot dựa theo cấu hình chiến lược (Random hoặc Determine)
     */
    Bot getBot(BotSamplingConfig config);

    /**
     * Lấy danh sách Bot cho platform với số lượng giới hạn
     */
    List<Bot> getBots(String platform, int limit);
}
