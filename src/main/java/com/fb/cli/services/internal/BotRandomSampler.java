package com.fb.cli.services.internal;

import com.fb.cli.dtos.bot.BotSamplingCfg;
import com.fb.cli.entities.Bot;
import com.fb.cli.enums.BotStatus;
import com.fb.cli.repositories.BotRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.*;

@Slf4j
public class BotRandomSampler implements BotSampleStrategy {

    @Getter
    private final BotSamplingCfg config;
    private final BotRepository botRepository;
    private List<Bot> inMemoryPool;

    public BotRandomSampler(BotRepository botRepository, BotSamplingCfg config) {
        this.botRepository = botRepository;
        this.config = config;
        this.inMemoryPool = null;
    }

    public BotRandomSampler(BotSamplingCfg config) {
        this(null, config);
    }

    public BotRandomSampler(BotSamplingCfg config, List<Bot> pool) {
        this.botRepository = null;
        this.config = config;
        this.inMemoryPool = pool != null ? new ArrayList<>(pool) : new ArrayList<>();
    }

    public void setPool(List<Bot> pool) {
        this.inMemoryPool = pool != null ? new ArrayList<>(pool) : new ArrayList<>();
    }

    @Override
    public List<Bot> sample() {
        // 1. Ưu tiên lấy từ in-memory pool nếu có (cho mock/unit test)
        if (inMemoryPool != null && !inMemoryPool.isEmpty()) {
            return sampleFromList(inMemoryPool);
        }

        // 2. Kéo trực tiếp từ Database thông qua BotRepository
        if (botRepository != null) {
            return sampleFromDb();
        }

        log.warn("[BotRandomSampler] Không có BotRepository lẫn inMemoryPool để lấy Bot");
        return Collections.emptyList();
    }

    @Override
    public List<Bot> sample(List<Bot> candidates) {
        if (candidates != null && !candidates.isEmpty()) {
            return sampleFromList(candidates);
        }
        return sample();
    }

    /**
     * Kéo các Bot từ database bằng cách đẩy điều kiện lọc xuống DB,
     * chỉ random offset trên RAM và fetch đúng số lượng cần thiết.
     */
    private List<Bot> sampleFromDb() {
        String platform = (config != null) ? config.platform() : null;
        BotStatus status = (config != null) ? config.status() : BotStatus.ACTIVE;
        int targetSize = (config != null && config.sampleSize() > 0) ? config.sampleSize() : 1;
        long seed = (config != null) ? config.seed() : 0;

        // 1. Đẩy điều kiện lọc xuống DB để đếm tổng số Bot thỏa mãn
        long totalEligible = botRepository.countEligibleBots(platform, status);
        if (totalEligible <= 0) {
            log.error("[BotRandomSampler] Không tìm thấy Bot nào khả dụng trong DB cho platform: {}, status: {}", platform, status);
            return Collections.emptyList();
        }

        // 2. Random các vị trí (offset) trên ứng dụng dựa trên seed và sampleSize
        int targetCount = (int) Math.min(targetSize, totalEligible);
        List<Integer> randomOffsets = sampleOffsets((int) totalEligible, targetCount, seed);

        // 3. Kéo chính xác các Bot cần thiết từ DB theo các offset đã random
        List<Bot> sampledBots = new ArrayList<>();
        for (int offset : randomOffsets) {
            Page<Bot> page = botRepository.findEligibleBots(platform, status, PageRequest.of(offset, 1));
            if (page.hasContent()) {
                sampledBots.add(page.getContent().get(0));
            }
        }

        if (sampledBots.isEmpty()) {
            log.error("[BotRandomSampler] Không thể tải Bot từ DB theo các offset đã random: {}", randomOffsets);
        }

        return sampledBots;
    }

    private List<Bot> sampleFromList(List<Bot> candidates) {
        int targetSize = (config != null && config.sampleSize() > 0)
                ? Math.min(config.sampleSize(), candidates.size())
                : 1;

        List<Bot> copy = new ArrayList<>(candidates);
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
