package com.fb.cli.services.internal;

import com.fb.cli.dtos.bot.BotRandomCfg;
import com.fb.cli.entities.Bot;
import com.fb.cli.repositories.BotRepository;

public class BotDetermineSampler implements BotSampleStrategy{
    private final BotRepository botRepository;
    private BotRandomCfg config;

    @Override
    public Bot sample() {
        return null;
    }
}
