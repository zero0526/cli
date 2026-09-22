package com.fb.cli.dtos.bot;

import java.util.List;

public record BotRandomCfg(
        List<String> excludeIds,
        String platform,
        int sampleSize,
        long seed
) implements BotSamplingConfig {

    public BotRandomCfg(String platform) {
        this(null, platform, 1, 0L);
    }

    public BotRandomCfg(String platform, int sampleSize) {
        this(null, platform, sampleSize, 0L);
    }
}

