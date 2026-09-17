package com.fb.cli.dtos.bot;

import com.fb.cli.enums.BotStatus;

public record BotSamplingCfg(
        int sampleSize,
        long seed,
        String platform,
        BotStatus status
) {
    public BotSamplingCfg(int sampleSize, long seed, String platform) {
        this(sampleSize, seed, platform, BotStatus.ACTIVE);
    }

    public BotSamplingCfg(int sampleSize, long seed) {
        this(sampleSize, seed, null, BotStatus.ACTIVE);
    }
}
