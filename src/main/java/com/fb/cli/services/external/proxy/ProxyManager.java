package com.fb.cli.services.external.proxy;

import com.fb.cli.dtos.proxy.ProxyInfo;
import com.fb.cli.dtos.proxy.RandomSamplingCfg;
import com.fb.cli.entities.ProxyKey;
import com.fb.cli.entities.ProxyProvider;
import com.fb.cli.enums.ProviderStrategy;
import com.fb.cli.enums.RotationType;
import com.fb.cli.repositories.ProxyKeyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ProxyManager {

    private final ProxyKeyRepository proxyKeyRepository;
    private final Map<ProviderStrategy, ProxyProviderStrategy> strategyMap;

    @Autowired
    public ProxyManager(
            ProxyKeyRepository proxyKeyRepository,
            List<ProxyProviderStrategy> strategies
    ) {
        this.proxyKeyRepository = proxyKeyRepository;
        this.strategyMap = (strategies != null)
                ? strategies.stream()
                .filter(s -> s.getStrategy() != null)
                .collect(Collectors.toMap(ProxyProviderStrategy::getStrategy, Function.identity(), (a, b) -> a))
                : Collections.emptyMap();
    }

    /**
     * Constructor dùng cho testing trực tiếp với map strategy
     */
    public ProxyManager(
            ProxyKeyRepository proxyKeyRepository,
            Map<ProviderStrategy, ProxyProviderStrategy> strategyMap
    ) {
        this.proxyKeyRepository = proxyKeyRepository;
        this.strategyMap = strategyMap != null ? strategyMap : Collections.emptyMap();
    }

    /**
     * Lấy 1 proxy phù hợp dựa vào RandomSamplingCfg.
     * Tự động khởi tạo RandomSampler tương ứng và điều phối lấy proxy.
     */
    public ProxyInfo getProxy(RandomSamplingCfg config) {
        if (config == null) {
            log.error("[ProxyManager] RandomSamplingCfg must not be null");
            return null;
        }
        return getProxy(new RandomSampler(this.proxyKeyRepository, config));
    }

    /**
     * Lấy 1 proxy phù hợp dựa vào ProxySampleStrategy.
     * Trả về null kèm log error nếu không có proxy nào thỏa mãn sampler.
     */
    public ProxyInfo getProxy(ProxySampleStrategy<ProxyKey> sampler) {
        List<ProxyInfo> proxies = getProxies(sampler);
        if (proxies == null || proxies.isEmpty()) {
            return null;
        }
        return proxies.get(0);
    }

    /**
     * Lấy danh sách proxy theo ProxySampleStrategy.
     * ProxyManager đóng vai trò Coordinator (Điều phối viên):
     * 1. Phân việc cho Sampler lấy danh sách apiToken / ProxyKey lên.
     * 2. Phân việc cho Provider tương ứng của từng apiToken lấy Proxy ra.
     */
    public List<ProxyInfo> getProxies(ProxySampleStrategy<ProxyKey> sampler) {
        if (sampler == null) {
            log.error("[ProxyManager] ProxySampleStrategy must not be null");
            return null;
        }

        // 1. Phân việc cho sampler lấy apiToken / ProxyKey lên
        List<ProxyKey> sampledKeys = sampler.sample();
        if (sampledKeys == null || sampledKeys.isEmpty()) {
            log.error("[ProxyManager] Sampler không tìm thấy proxy key nào thỏa mãn điều kiện");
            return null;
        }

        // 2. Phân việc cho provider của từng apiToken lấy proxy ra
        List<ProxyInfo> result = new ArrayList<>();
        for (ProxyKey key : sampledKeys) {
            ProxyProvider provider = key.getProvider();
            if (provider == null) {
                log.error("[ProxyManager] Key {} không gắn với Provider nào", key.getApiKey());
                continue;
            }

            ProxyProviderStrategy strategy = strategyMap.get(provider.getStrategy());
            if (strategy == null) {
                log.error("[ProxyManager] Không tìm thấy ProxyProviderStrategy cho chiến lược: {}", provider.getStrategy());
                continue;
            }

            try {
                ProxyInfo proxyInfo;
                if (provider.getProxyType() == RotationType.ROTATING) {
                    proxyInfo = strategy.getNewProxy(key.getApiKey());
                } else {
                    proxyInfo = strategy.getCurrentProxy(key.getApiKey());
                }

                if (proxyInfo != null) {
                    result.add(proxyInfo);
                } else {
                    log.error("[ProxyManager] Strategy {} trả về null khi lấy proxy cho key {}", provider.getStrategy(), key.getApiKey());
                }
            } catch (Exception e) {
                log.error("[ProxyManager] Lỗi khi gọi strategy {} để lấy proxy cho key {}: {}",
                        provider.getStrategy(), key.getApiKey(), e.getMessage(), e);
            }
        }

        if (result.isEmpty()) {
            log.error("[ProxyManager] Không có proxy nào được lấy thành công từ {} keys đã chọn", sampledKeys.size());
            return null;
        }

        return result;
    }
}
