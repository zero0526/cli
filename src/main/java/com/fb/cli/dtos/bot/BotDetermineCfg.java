package com.fb.cli.dtos.bot;

import java.util.UUID;

public record BotDetermineCfg(
        String platform,
        String bot_id,
        UUID id
) implements BotSamplingConfig {

    public BotDetermineCfg(String platform, String bot_id) {
        this(platform, bot_id, null);
    }

    public BotDetermineCfg(String platform, UUID id) {
        this(platform, null, id);
    }
}

