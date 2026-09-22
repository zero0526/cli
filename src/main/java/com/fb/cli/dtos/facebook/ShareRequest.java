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
public class ShareRequest {

    /**
     * Target Numeric Post ID (ví dụ: 1426391936363340)
     */
    private String targetId;

    /**
     * Nội dung caption đính kèm khi share (tùy chọn)
     */
    private String content;

    /**
     * Quyền riêng tư: EVERYONE, FRIENDS, SELF (mặc định FRIENDS)
     */
    @Builder.Default
    private String privacy = "FRIENDS";

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
