package com.fb.cli.dtos.bot;

import java.util.List;

public record BotRandomCfg(
        List<String> excludeIds,
        String platform,
        int sampleSize,
        long seed
) {
}
