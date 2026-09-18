package com.fb.cli.dtos.facebook;

import com.fb.cli.dtos.bot.BotSamplingConfig;
import com.fb.cli.dtos.proxy.ProxyInfo;
import com.fb.cli.dtos.proxy.RandomSamplingCfg;
import com.fb.cli.entities.Bot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequest {

    /**
     * Target ID: có thể là Post ID hoặc Comment ID (nếu reply)
     */
    private String targetId;

    /**
     * Nội dung text của comment
     */
    private String content;

    /**
     * URL ảnh hoặc file đính kèm (nếu có)
     */
    private String attachmentUrl;

    /**
     * Bot cụ thể thực hiện hành động (ưu tiên nếu có)
     */
    private Bot bot;

    /**
     * Cấu hình chiến lược để tự động lấy Bot (nếu chưa truyền Bot cụ thể)
     */
    private BotSamplingConfig botConfig;

    /**
     * Proxy cụ thể (ưu tiên nếu có)
     */
    private ProxyInfo proxy;

    /**
     * Cấu hình proxy sampler để tự động lấy proxy
     */
    private RandomSamplingCfg proxySampler;
}

