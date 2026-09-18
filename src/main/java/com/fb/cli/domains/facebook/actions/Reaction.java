package com.fb.cli.domains.facebook.actions;

import com.fb.cli.dtos.bot.BotRandomCfg;
import com.fb.cli.dtos.bot.BotSamplingConfig;
import com.fb.cli.dtos.facebook.ReactionRequest;
import com.fb.cli.dtos.facebook.ReactionResult;
import com.fb.cli.dtos.proxy.ProxyInfo;
import com.fb.cli.dtos.proxy.RandomSamplingCfg;
import com.fb.cli.entities.Bot;
import com.fb.cli.enums.FacebookReactionType;

public interface Reaction {

    /**
     * Phương thức cốt lõi: gửi reaction với đầy đủ thông tin cấu hình
     */
    ReactionResult sendReaction(ReactionRequest request);

    /**
     * Tiện ích: Bắn reaction với 1 Bot và Proxy cụ thể
     */
    default ReactionResult sendReaction(String targetId, FacebookReactionType type, Bot bot, ProxyInfo proxy) {
        return sendReaction(ReactionRequest.builder()
                .targetId(targetId)
                .type(type)
                .bot(bot)
                .proxy(proxy)
                .build());
    }

    /**
     * Tiện ích: Bắn reaction với 1 Bot cụ thể (không proxy hoặc dùng proxy mặc định)
     */
    default ReactionResult sendReaction(String targetId, FacebookReactionType type, Bot bot) {
        return sendReaction(targetId, type, bot, null);
    }

    /**
     * Tiện ích: Bắn reaction với cấu hình bốc Bot (BotSamplingConfig) và cấu hình bốc Proxy
     */
    default ReactionResult sendReaction(String targetId, FacebookReactionType type, BotSamplingConfig botConfig, RandomSamplingCfg proxySampler) {
        return sendReaction(ReactionRequest.builder()
                .targetId(targetId)
                .type(type)
                .botConfig(botConfig)
                .proxySampler(proxySampler)
                .build());
    }

    /**
     * Tiện ích: Bắn reaction với platform cụ thể (tự động cấu hình BotRandomCfg) và Proxy sampler
     */
    default ReactionResult sendReaction(String targetId, FacebookReactionType type, String platform, RandomSamplingCfg proxySampler) {
        return sendReaction(targetId, type, new BotRandomCfg(platform), proxySampler);
    }
}


