package com.fb.cli.services.internal;

import com.fb.cli.dtos.bot.BotRandomCfg;
import com.fb.cli.entities.Bot;
import com.fb.cli.repositories.BotRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class BotDetermineSampler {
    private final BotRepository botRepository;
    private BotRandomCfg config;

    public List<Bot> sample() {
        return null;
    }
}
