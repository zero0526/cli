package com.fb.cli.domains.facebook.actions;

import com.fb.cli.dtos.bot.BotRandomCfg;
import com.fb.cli.dtos.bot.BotSamplingConfig;
import com.fb.cli.dtos.facebook.ShareRequest;
import com.fb.cli.dtos.facebook.ShareResult;
import com.fb.cli.dtos.proxy.ProxyInfo;
import com.fb.cli.dtos.proxy.RandomSamplingCfg;
import com.fb.cli.entities.Bot;

public interface Share {

    /**
     * Phương thức cốt lõi: gửi request share bài viết lên Feed
     */
    ShareResult sendShare(ShareRequest request);

    /**
     * Tiện ích: Share với 1 Bot và Proxy cụ thể
     */
    default ShareResult sendShare(String targetId, String content, Bot bot, ProxyInfo proxy) {
        return sendShare(ShareRequest.builder()
                .targetId(targetId)
                .content(content)
                .bot(bot)
                .proxy(proxy)
                .build());
    }

    /**
     * Tiện ích: Share với 1 Bot cụ thể
     */
    default ShareResult sendShare(String targetId, String content, Bot bot) {
        return sendShare(targetId, content, bot, null);
    }

    /**
     * Tiện ích: Share với cấu hình bốc Bot và cấu hình bốc Proxy
     */
    default ShareResult sendShare(String targetId, String content, BotSamplingConfig botConfig, RandomSamplingCfg proxySampler) {
        return sendShare(ShareRequest.builder()
                .targetId(targetId)
                .content(content)
                .botConfig(botConfig)
                .proxySampler(proxySampler)
                .build());
    }

    /**
     * Tiện ích: Share với platform cụ thể
     */
    default ShareResult sendShare(String targetId, String content, String platform, RandomSamplingCfg proxySampler) {
        return sendShare(targetId, content, new BotRandomCfg(platform), proxySampler);
    }
}
