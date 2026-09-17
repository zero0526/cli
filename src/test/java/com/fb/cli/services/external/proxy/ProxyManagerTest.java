package com.fb.cli.services.external.proxy;

import com.fb.cli.dtos.proxy.ProxyInfo;
import com.fb.cli.dtos.proxy.RandomSamplingCfg;
import com.fb.cli.entities.ProxyKey;
import com.fb.cli.entities.ProxyProvider;
import com.fb.cli.enums.ProviderStrategy;
import com.fb.cli.enums.RotationType;
import com.fb.cli.repositories.ProxyKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProxyManagerTest {

    @Mock
    private ProxyKeyRepository proxyKeyRepository;

    @Mock
    private ProxyProviderStrategy tmProviderStrategy;

    private ProxyManager proxyManager;
    private ProxyProvider rotatingProvider;
    private ProxyKey rotatingKey;

    @BeforeEach
    void setUp() {
        rotatingProvider = ProxyProvider.builder()
                .id(UUID.randomUUID())
                .name("TMProxy")
                .strategy(ProviderStrategy.TM_Strategy)
                .proxyType(RotationType.ROTATING)
                .baseUrl("https://tmproxy.com/api/proxy")
                .isActive(true)
                .build();

        rotatingKey = ProxyKey.builder()
                .id(UUID.randomUUID())
                .apiKey("tm_test_api_key")
                .provider(rotatingProvider)
                .isActive(true)
                .expiredAt(LocalDateTime.now().plusDays(5))
                .build();

        proxyManager = new ProxyManager(proxyKeyRepository, Map.of(ProviderStrategy.TM_Strategy, tmProviderStrategy));
    }

    @Test
    void shouldReturnProxySuccessfullyWhenEligibleKeyExists() {
        RandomSamplingCfg cfg = new RandomSamplingCfg(1, 0L, RotationType.ROTATING);
        when(proxyKeyRepository.countEligibleKeys(RotationType.ROTATING)).thenReturn(1L);
        when(proxyKeyRepository.findEligibleKeys(eq(RotationType.ROTATING), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(rotatingKey)));
        when(tmProviderStrategy.getNewProxy("tm_test_api_key"))
                .thenReturn(new ProxyInfo("103.15.22.1", 8080, null, null, "http", 1750000000L));

        ProxyInfo proxy = proxyManager.getProxy(cfg);

        assertNotNull(proxy);
        assertEquals("103.15.22.1", proxy.host());
        assertEquals(8080, proxy.port());
        verify(proxyKeyRepository, times(1)).countEligibleKeys(RotationType.ROTATING);
        verify(proxyKeyRepository, times(1)).findEligibleKeys(eq(RotationType.ROTATING), any(PageRequest.class));
        verify(tmProviderStrategy, times(1)).getNewProxy("tm_test_api_key");
    }

    @Test
    void shouldReturnNullWhenNoEligibleKeysFoundInDb() {
        RandomSamplingCfg cfg = new RandomSamplingCfg(1, 0L, RotationType.ROTATING);
        when(proxyKeyRepository.countEligibleKeys(RotationType.ROTATING)).thenReturn(0L);

        ProxyInfo proxy = proxyManager.getProxy(cfg);

        assertNull(proxy);
        verify(proxyKeyRepository, times(1)).countEligibleKeys(RotationType.ROTATING);
        verify(proxyKeyRepository, never()).findEligibleKeys(any(), any(PageRequest.class));
        verify(tmProviderStrategy, never()).getNewProxy(any());
    }

    @Test
    void shouldReturnNullWhenStrategyThrowsException() {
        RandomSamplingCfg cfg = new RandomSamplingCfg(1, 0L, RotationType.ROTATING);
        when(proxyKeyRepository.countEligibleKeys(RotationType.ROTATING)).thenReturn(1L);
        when(proxyKeyRepository.findEligibleKeys(eq(RotationType.ROTATING), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(rotatingKey)));
        when(tmProviderStrategy.getNewProxy("tm_test_api_key"))
                .thenThrow(new RuntimeException("Connection timeout"));

        ProxyInfo proxy = proxyManager.getProxy(cfg);

        assertNull(proxy);
    }

    @Test
    void shouldReturnNullWhenConfigIsNull() {
        ProxyInfo proxy = proxyManager.getProxy((RandomSamplingCfg) null);
        assertNull(proxy);
    }
}
