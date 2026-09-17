package com.fb.cli.repositories;

import com.fb.cli.entities.Bot;
import com.fb.cli.enums.BotStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BotRepository extends JpaRepository<Bot, UUID> {

    Optional<Bot> findByBotId(String botId);

    List<Bot> findByPlatform(String platform);

    List<Bot> findByStatus(BotStatus status);

    List<Bot> findByPlatformAndStatus(String platform, BotStatus status);

    default long countEligibleBots(String platform, BotStatus status) {
        if (platform != null && status != null) {
            return countByPlatformAndStatus(platform, status);
        } else if (platform != null) {
            return countByPlatform(platform);
        } else if (status != null) {
            return countByStatus(status);
        }
        return count();
    }

    default Page<Bot> findEligibleBots(String platform, BotStatus status, Pageable pageable) {
        if (platform != null && status != null) {
            return findByPlatformAndStatus(platform, status, pageable);
        } else if (platform != null) {
            return findByPlatform(platform, pageable);
        } else if (status != null) {
            return findByStatus(status, pageable);
        }
        return findAll(pageable);
    }

    @Query("SELECT COUNT(b) FROM Bot b WHERE b.platform = :platform AND b.status = :status")
    long countByPlatformAndStatus(@Param("platform") String platform, @Param("status") BotStatus status);

    @Query("SELECT COUNT(b) FROM Bot b WHERE b.platform = :platform")
    long countByPlatform(@Param("platform") String platform);

    @Query("SELECT COUNT(b) FROM Bot b WHERE b.status = :status")
    long countByStatus(@Param("status") BotStatus status);

    Page<Bot> findByPlatformAndStatus(String platform, BotStatus status, Pageable pageable);

    Page<Bot> findByPlatform(String platform, Pageable pageable);

    Page<Bot> findByStatus(BotStatus status, Pageable pageable);
}
