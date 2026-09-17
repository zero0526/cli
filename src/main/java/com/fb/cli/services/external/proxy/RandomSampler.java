package com.fb.cli.services.external.proxy;

import com.fb.cli.dtos.proxy.RandomSamplingCfg;
import com.fb.cli.entities.ProxyKey;
import com.fb.cli.enums.RotationType;
import com.fb.cli.repositories.ProxyKeyRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.*;

@Slf4j
public class RandomSampler implements ProxySampleStrategy<ProxyKey> {

    @Getter
    private final RandomSamplingCfg config;
    private final ProxyKeyRepository proxyKeyRepository;
    private List<ProxyKey> inMemoryPool;

    public RandomSampler(ProxyKeyRepository proxyKeyRepository, RandomSamplingCfg config) {
        this.proxyKeyRepository = proxyKeyRepository;
        this.config = config;
        this.inMemoryPool = null;
    }

    public RandomSampler(RandomSamplingCfg config) {
        this(null, config);
    }

    public RandomSampler(RandomSamplingCfg config, List<ProxyKey> pool) {
        this.proxyKeyRepository = null;
        this.config = config;
        this.inMemoryPool = pool != null ? new ArrayList<>(pool) : new ArrayList<>();
    }

    public void setPool(List<ProxyKey> pool) {
        this.inMemoryPool = pool != null ? new ArrayList<>(pool) : new ArrayList<>();
    }

    @Override
    public List<ProxyKey> sample() {
        // 1. Ưu tiên lấy từ in-memory pool nếu có (dùng cho testing hoặc nguồn tĩnh)
        if (inMemoryPool != null && !inMemoryPool.isEmpty()) {
            return sampleFromList(inMemoryPool);
        }

        // 2. Kéo trực tiếp từ Database thông qua ProxyKeyRepository
        if (proxyKeyRepository != null) {
            return sampleFromDb();
        }

        log.warn("[RandomSampler] Không có ProxyKeyRepository lẫn inMemoryPool để lấy ProxyKey");
        return Collections.emptyList();
    }

    @Override
    public List<ProxyKey> sample(List<ProxyKey> candidates) {
        if (candidates != null && !candidates.isEmpty()) {
            return sampleFromList(candidates);
        }
        return sample();
    }

    /**
     * Kéo các ProxyKey từ database bằng cách đẩy điều kiện lọc xuống DB,
     * chỉ random offset trên RAM và fetch đúng số lượng cần thiết.
     */
    private List<ProxyKey> sampleFromDb() {
        RotationType proxyType = (config != null) ? config.proxyType() : null;
        int targetSize = (config != null && config.sampleSize() > 0) ? config.sampleSize() : 1;
        long seed = (config != null) ? config.seed() : 0;

        // 1. Đẩy điều kiện lọc xuống DB để đếm tổng số proxy thỏa mãn (không tải all vào RAM)
        long totalEligible = proxyKeyRepository.countEligibleKeys(proxyType);
        if (totalEligible <= 0) {
            log.error("[RandomSampler] Không tìm thấy proxy key nào hợp lệ và còn hạn trong hệ thống cho proxyType: {}", proxyType);
            return Collections.emptyList();
        }

        // 2. Random các vị trí (offset) trên ứng dụng dựa trên seed và sampleSize
        int targetCount = (int) Math.min(targetSize, totalEligible);
        List<Integer> randomOffsets = sampleOffsets((int) totalEligible, targetCount, seed);

        // 3. Kéo chính xác các ProxyKey cần thiết từ DB theo các offset đã random
        List<ProxyKey> sampledKeys = new ArrayList<>();
        for (int offset : randomOffsets) {
            Page<ProxyKey> page = proxyKeyRepository.findEligibleKeys(proxyType, PageRequest.of(offset, 1));
            if (page.hasContent()) {
                sampledKeys.add(page.getContent().get(0));
            }
        }

        if (sampledKeys.isEmpty()) {
            log.error("[RandomSampler] Không thể tải key từ DB theo các offset đã random: {}", randomOffsets);
        }

        return sampledKeys;
    }

    private List<ProxyKey> sampleFromList(List<ProxyKey> candidates) {
        int targetSize = (config != null && config.sampleSize() > 0)
                ? Math.min(config.sampleSize(), candidates.size())
                : 1;

        List<ProxyKey> copy = new ArrayList<>(candidates);
        Random random = (config != null && config.seed() != 0)
                ? new Random(config.seed())
                : new Random();

        Collections.shuffle(copy, random);
        return copy.subList(0, targetSize);
    }

    /**
     * Random các offset riêng biệt trên app trong dải [0, total)
     */
    private List<Integer> sampleOffsets(int total, int sampleSize, long seed) {
        if (total <= 0 || sampleSize <= 0) {
            return Collections.emptyList();
        }
        if (total <= sampleSize) {
            List<Integer> all = new ArrayList<>(total);
            for (int i = 0; i < total; i++) all.add(i);
            return all;
        }

        Random random = (seed != 0) ? new Random(seed) : new Random();
        Set<Integer> chosen = new LinkedHashSet<>();
        while (chosen.size() < sampleSize) {
            chosen.add(random.nextInt(total));
        }
        return new ArrayList<>(chosen);
    }
}
