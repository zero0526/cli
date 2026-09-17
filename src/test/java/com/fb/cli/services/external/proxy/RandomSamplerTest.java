package com.fb.cli.services.external.proxy;

import com.fb.cli.dtos.proxy.RandomSamplingCfg;
import com.fb.cli.entities.ProxyKey;
import com.fb.cli.enums.RotationType;
import com.fb.cli.repositories.ProxyKeyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RandomSamplerTest {

    @Mock
    private ProxyKeyRepository proxyKeyRepository;

    @Test
    @DisplayName("RandomSampler lấy ProxyKey từ Database thông qua ProxyKeyRepository")
    void testSampleFromDb() {
        ProxyKey key1 = ProxyKey.builder().id(UUID.randomUUID()).apiKey("key_1").build();
        RandomSamplingCfg cfg = new RandomSamplingCfg(1, 42L, RotationType.ROTATING);

        when(proxyKeyRepository.countEligibleKeys(RotationType.ROTATING)).thenReturn(1L);
        when(proxyKeyRepository.findEligibleKeys(eq(RotationType.ROTATING), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(key1)));

        RandomSampler sampler = new RandomSampler(proxyKeyRepository, cfg);
        List<ProxyKey> sampled = sampler.sample();

        assertNotNull(sampled);
        assertEquals(1, sampled.size());
        assertEquals("key_1", sampled.get(0).getApiKey());
        verify(proxyKeyRepository, times(1)).countEligibleKeys(RotationType.ROTATING);
        verify(proxyKeyRepository, times(1)).findEligibleKeys(eq(RotationType.ROTATING), any(PageRequest.class));
    }

    @Test
    @DisplayName("RandomSampler lấy từ In-Memory Pool")
    void testSampleFromInMemoryPool() {
        ProxyKey key1 = ProxyKey.builder().id(UUID.randomUUID()).apiKey("k1").build();
        ProxyKey key2 = ProxyKey.builder().id(UUID.randomUUID()).apiKey("k2").build();
        ProxyKey key3 = ProxyKey.builder().id(UUID.randomUUID()).apiKey("k3").build();

        RandomSamplingCfg cfg = new RandomSamplingCfg(2, 100L, RotationType.ROTATING);
        RandomSampler sampler = new RandomSampler(cfg, List.of(key1, key2, key3));

        List<ProxyKey> sampled = sampler.sample();

        assertNotNull(sampled);
        assertEquals(2, sampled.size());
    }

    @Test
    @DisplayName("RandomSampler trả về rỗng khi DB không có key nào")
    void testSampleFromDbEmpty() {
        RandomSamplingCfg cfg = new RandomSamplingCfg(1, 42L, RotationType.ROTATING);
        when(proxyKeyRepository.countEligibleKeys(RotationType.ROTATING)).thenReturn(0L);

        RandomSampler sampler = new RandomSampler(proxyKeyRepository, cfg);
        List<ProxyKey> sampled = sampler.sample();

        assertNotNull(sampled);
        assertTrue(sampled.isEmpty());
    }
}
