package com.fb.cli.domains.facebook.actions;

import com.fb.cli.dtos.bot.BotRandomCfg;
import com.fb.cli.dtos.bot.BotSamplingConfig;
import com.fb.cli.dtos.facebook.CommentRequest;
import com.fb.cli.dtos.facebook.CommentResult;
import com.fb.cli.dtos.proxy.ProxyInfo;
import com.fb.cli.dtos.proxy.RandomSamplingCfg;
import com.fb.cli.entities.Bot;

public interface Comment {

    /**
     * Phương thức cốt lõi: gửi comment với đầy đủ thông tin cấu hình
     */
    CommentResult sendComment(CommentRequest request);

    /**
     * Tiện ích: Gửi comment với 1 Bot và Proxy cụ thể
     */
    default CommentResult sendComment(String targetId, String content, Bot bot, ProxyInfo proxy) {
        return sendComment(CommentRequest.builder()
                .targetId(targetId)
                .content(content)
                .bot(bot)
                .proxy(proxy)
                .build());
    }

    /**
     * Tiện ích: Gửi comment với 1 Bot cụ thể (không proxy hoặc dùng proxy mặc định)
     */
    default CommentResult sendComment(String targetId, String content, Bot bot) {
        return sendComment(targetId, content, bot, null);
    }

    /**
     * Tiện ích: Gửi comment với cấu hình bốc Bot (BotSamplingConfig) và cấu hình bốc Proxy
     */
    default CommentResult sendComment(String targetId, String content, BotSamplingConfig botConfig, RandomSamplingCfg proxySampler) {
        return sendComment(CommentRequest.builder()
                .targetId(targetId)
                .content(content)
                .botConfig(botConfig)
                .proxySampler(proxySampler)
                .build());
    }

    /**
     * Tiện ích: Gửi comment với platform cụ thể (tự động cấu hình BotRandomCfg) và Proxy sampler
     */
    default CommentResult sendComment(String targetId, String content, String platform, RandomSamplingCfg proxySampler) {
        return sendComment(targetId, content, new BotRandomCfg(platform), proxySampler);
    }
}

