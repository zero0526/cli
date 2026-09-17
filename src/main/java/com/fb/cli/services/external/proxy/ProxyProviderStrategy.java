package com.fb.cli.services.external.proxy;

import com.fb.cli.dtos.proxy.ProxyInfo;
import com.fb.cli.enums.ProviderStrategy;

import java.time.LocalDateTime;

public interface ProxyProviderStrategy {
    ProxyInfo getNewProxy(String apiKey);
    ProxyInfo getCurrentProxy(String apiKey);
    LocalDateTime keyExpiredTime(String apiKey);
    default ProviderStrategy getStrategy() {
        return null;
    }
}
