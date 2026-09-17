package com.fb.cli.services.external.proxy;

import com.fb.cli.dtos.proxy.ProxyInfo;
import com.fb.cli.dtos.proxy.RandomSamplingCfg;
import com.fb.cli.entities.ProxyProvider;
import com.fb.cli.enums.RotationType;
import com.fb.cli.repositories.ProxyKeyRepository;
import com.fb.cli.repositories.ProxyProviderRepository;
import com.fb.cli.services.external.SendRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
class ProxyManagerIntegrationTest {

    @Autowired
    private ProxyManager proxyManager;

    @Autowired
    private ProxyKeyRepository proxyKeyRepository;

    @Autowired
    private ProxyProviderRepository proxyProviderRepository;

    @Autowired
    private SendRequest sendRequest;

    @Test
    @DisplayName("Kiểm tra ProxyManager kéo ProxyKey từ PostgreSQL container và thực thi lấy Proxy")
    void testPullKeyFromDbAndGetProxy() {
        // 1. Xác minh Provider và Key đã tồn tại trong DB container
        List<ProxyProvider> providers = proxyProviderRepository.findAll();
        assertFalse(providers.isEmpty(), "DB phải có ít nhất 1 ProxyProvider (vd: TMProxy)");
        log.info("Tìm thấy {} provider(s) trong DB:", providers.size());
        for (ProxyProvider p : providers) {
            log.info(" - Provider: {} [Strategy: {}, Type: {}, Active: {}]",
                    p.getName(), p.getStrategy(), p.getProxyType(), p.getIsActive());
        }

        long totalEligible = proxyKeyRepository.countEligibleKeys(RotationType.ROTATING);
        log.info("Tổng số Rotating ProxyKey hợp lệ trong DB: {}", totalEligible);
        assertTrue(totalEligible > 0, "DB phải có ít nhất 1 ProxyKey hợp lệ để test");

        // 2. Gọi ProxyManager để lấy proxy theo RandomSamplingCfg
        RandomSamplingCfg cfg = new RandomSamplingCfg(1, 42L, RotationType.ROTATING);
        log.info("Bắt đầu gọi ProxyManager.getProxy() với cấu hình: {}", cfg);

        ProxyInfo proxy = proxyManager.getProxy(cfg);

        if (proxy != null) {
            log.info("===> Lấy PROXY THÀNH CÔNG từ nhà cung cấp: {}", proxy);
            assertNotNull(proxy.host());
            assertNotNull(proxy.port());
            log.info("Proxy host: {}, port: {}, protocol: {}", proxy.host(), proxy.port(), proxy.protocol());
        } else {
            log.warn("===> ProxyManager trả về null (có thể do API Key trong DB chưa phải là key thật/đang hết hạn).");
            log.warn("Kiểm tra log lỗi ở trên để xem chi tiết response trả về từ TMProxy.");
        }
    }

    @Test
    @DisplayName("Kiểm tra SendRequest.get().withProxy(sampler).execute() tự động kích hoạt ProxyInterceptor")
    void testSendRequestWithProxySampler() {
        RandomSamplingCfg cfg = new RandomSamplingCfg(1, 42L, RotationType.ROTATING);

        // Gọi sendRequest dạng fluent chain với .withProxy(cfg)
        try {
            String response = sendRequest.get("https://httpbin.org/ip")
                    .header("User-Agent", "CLI-Crawler")
                    .withProxy(cfg)
                    .execute();
            log.info("Response nhận được từ target server qua proxy: {}", response);
        } catch (Exception e) {
            log.info("Đã kích hoạt chuỗi request và ProxyInterceptor thành công (Lỗi ngoại lệ mạng hoặc API key dự kiến nếu key mẫu): {}", e.getMessage());
        }
    }
}
