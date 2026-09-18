package com.fb.cli.domains.facebook.services;

import com.fb.cli.entities.ActionConfig;

public interface ActionConfigService {

    /**
     * Lấy cấu hình action đang active từ Redis cache hoặc Database
     */
    ActionConfig getActiveConfig(String platform, String actionType, String actionProvider);

    /**
     * Xóa cache Redis cho action cụ thể khi có cập nhật DB
     */
    void evictCache(String platform, String actionType, String actionProvider);
}
