package com.fb.cli.domains.facebook.actions;

import com.fb.cli.dtos.bot.BotSamplingConfig;
import com.fb.cli.dtos.facebook.CreateStoryRequest;
import com.fb.cli.dtos.facebook.CreateStoryResult;
import com.fb.cli.dtos.proxy.ProxyInfo;
import com.fb.cli.dtos.proxy.RandomSamplingCfg;
import com.fb.cli.entities.Bot;

import java.io.File;

public interface CreateStory {

    /**
     * Phương thức cốt lõi: Đăng bài viết / story lên Timeline (kèm ảnh hoặc chỉ text)
     */
    CreateStoryResult createStory(CreateStoryRequest request);

    /**
     * Tiện ích: Đăng bài kèm dữ liệu bytes của ảnh
     */
    default CreateStoryResult createStory(String content, byte[] imageBytes, String imageName, Bot bot, ProxyInfo proxy) {
        return createStory(CreateStoryRequest.builder()
                .content(content)
                .imageBytes(imageBytes)
                .imageName(imageName)
                .bot(bot)
                .proxy(proxy)
                .build());
    }

    /**
     * Tiện ích: Đăng bài kèm File ảnh
     */
    default CreateStoryResult createStory(String content, File imageFile, Bot bot, ProxyInfo proxy) {
        return createStory(CreateStoryRequest.builder()
                .content(content)
                .imageFile(imageFile)
                .bot(bot)
                .proxy(proxy)
                .build());
    }

    /**
     * Tiện ích: Đăng bài với photoId đã có sẵn
     */
    default CreateStoryResult createStoryWithPhotoId(String content, String photoId, Bot bot, ProxyInfo proxy) {
        return createStory(CreateStoryRequest.builder()
                .content(content)
                .photoId(photoId)
                .bot(bot)
                .proxy(proxy)
                .build());
    }

    /**
     * Tiện ích: Đăng bài kèm ảnh sử dụng cấu hình bốc Bot và Proxy
     */
    default CreateStoryResult createStory(String content, byte[] imageBytes, String imageName, BotSamplingConfig botConfig, RandomSamplingCfg proxySampler) {
        return createStory(CreateStoryRequest.builder()
                .content(content)
                .imageBytes(imageBytes)
                .imageName(imageName)
                .botConfig(botConfig)
                .proxySampler(proxySampler)
                .build());
    }
}
