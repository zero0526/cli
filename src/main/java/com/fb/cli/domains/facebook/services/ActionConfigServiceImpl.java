package com.fb.cli.domains.facebook.services;

import com.fb.cli.entities.ActionConfig;
import com.fb.cli.persistences.redis.RedisService;
import com.fb.cli.repositories.ActionConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActionConfigServiceImpl implements ActionConfigService {

    private static final String CACHE_KEY_PREFIX = "config:action:";
    private static final long CACHE_TTL_HOURS = 1;

    private final ActionConfigRepository actionConfigRepository;
    private final RedisService redisService;

    @Override
    public ActionConfig getActiveConfig(String platform, String actionType, String actionProvider) {
        String cacheKey = buildCacheKey(platform, actionType, actionProvider);

        // 1. Kiểm tra cache Redis
        ActionConfig cached = redisService.get(cacheKey, ActionConfig.class);
        if (cached != null) {
            log.debug("[ActionConfigService] Hit cache for {}", cacheKey);
            return cached;
        }

        // 2. Query từ DB
        log.info("[ActionConfigService] Miss cache for {}. Querying database...", cacheKey);
        ActionConfig config = actionConfigRepository.findActiveConfig(platform, actionType, actionProvider)
                .orElse(null);

        if (config != null) {
            // Cache vào Redis với TTL 1 giờ
            redisService.set(cacheKey, config, CACHE_TTL_HOURS, TimeUnit.HOURS);
            log.info("[ActionConfigService] Cached config {} (version={})", cacheKey, config.getVersion());
        } else {
            log.warn("[ActionConfigService] Không tìm thấy action config active nào cho: platform={}, actionType={}, provider={}",
                    platform, actionType, actionProvider);
        }

        return config;
    }

    @Override
    public void evictCache(String platform, String actionType, String actionProvider) {
        String cacheKey = buildCacheKey(platform, actionType, actionProvider);
        redisService.delete(cacheKey);
        log.info("[ActionConfigService] Evicted cache key: {}", cacheKey);
    }

    private String buildCacheKey(String platform, String actionType, String actionProvider) {
        return CACHE_KEY_PREFIX + (platform != null ? platform.toUpperCase() : "UNKNOWN") + ":"
                + (actionType != null ? actionType.toUpperCase() : "UNKNOWN") + ":"
                + (actionProvider != null ? actionProvider.toUpperCase() : "UNKNOWN");
    }
}
