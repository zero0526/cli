package com.fb.cli.dtos.bot;

import java.util.UUID;

public record BotDetermineCfg(
        String platform,
        String bot_id,
        UUID id
) {
}
