package com.fb.cli.repositories;

import com.fb.cli.entities.ProxyKey;
import com.fb.cli.enums.RotationType;
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
public interface ProxyKeyRepository extends JpaRepository<ProxyKey, UUID> {

    Optional<ProxyKey> findByApiKey(String apiKey);

    default long countEligibleKeys(RotationType proxyType) {
        return (proxyType == null) ? countAllEligibleKeys() : countEligibleKeysByType(proxyType);
    }

    default Page<ProxyKey> findEligibleKeys(RotationType proxyType, Pageable pageable) {
        return (proxyType == null) ? findAllEligibleKeys(pageable) : findEligibleKeysByType(proxyType, pageable);
    }

    default List<ProxyKey> findEligibleKeys(RotationType proxyType) {
        return (proxyType == null) ? findAllEligibleKeys() : findEligibleKeysByType(proxyType);
    }

    @Query("""
        SELECT COUNT(k) FROM ProxyKey k
        JOIN k.provider p
        WHERE p.isActive = true
          AND k.isActive = true
          AND (k.expiredAt IS NULL OR k.expiredAt > CURRENT_TIMESTAMP)
    """)
    long countAllEligibleKeys();

    @Query("""
        SELECT COUNT(k) FROM ProxyKey k
        JOIN k.provider p
        WHERE p.isActive = true
          AND k.isActive = true
          AND p.proxyType = :proxyType
          AND (k.expiredAt IS NULL OR k.expiredAt > CURRENT_TIMESTAMP)
    """)
    long countEligibleKeysByType(@Param("proxyType") RotationType proxyType);

    @Query(value = """
        SELECT k FROM ProxyKey k
        JOIN FETCH k.provider p
        WHERE p.isActive = true
          AND k.isActive = true
          AND (k.expiredAt IS NULL OR k.expiredAt > CURRENT_TIMESTAMP)
        ORDER BY k.id
    """,
    countQuery = """
        SELECT COUNT(k) FROM ProxyKey k
        JOIN k.provider p
        WHERE p.isActive = true
          AND k.isActive = true
          AND (k.expiredAt IS NULL OR k.expiredAt > CURRENT_TIMESTAMP)
    """)
    Page<ProxyKey> findAllEligibleKeys(Pageable pageable);

    @Query(value = """
        SELECT k FROM ProxyKey k
        JOIN FETCH k.provider p
        WHERE p.isActive = true
          AND k.isActive = true
          AND p.proxyType = :proxyType
          AND (k.expiredAt IS NULL OR k.expiredAt > CURRENT_TIMESTAMP)
        ORDER BY k.id
    """,
    countQuery = """
        SELECT COUNT(k) FROM ProxyKey k
        JOIN k.provider p
        WHERE p.isActive = true
          AND k.isActive = true
          AND p.proxyType = :proxyType
          AND (k.expiredAt IS NULL OR k.expiredAt > CURRENT_TIMESTAMP)
    """)
    Page<ProxyKey> findEligibleKeysByType(@Param("proxyType") RotationType proxyType, Pageable pageable);

    @Query("""
        SELECT k FROM ProxyKey k
        JOIN FETCH k.provider p
        WHERE p.isActive = true
          AND k.isActive = true
          AND (k.expiredAt IS NULL OR k.expiredAt > CURRENT_TIMESTAMP)
    """)
    List<ProxyKey> findAllEligibleKeys();

    @Query("""
        SELECT k FROM ProxyKey k
        JOIN FETCH k.provider p
        WHERE p.isActive = true
          AND k.isActive = true
          AND p.proxyType = :proxyType
          AND (k.expiredAt IS NULL OR k.expiredAt > CURRENT_TIMESTAMP)
    """)
    List<ProxyKey> findEligibleKeysByType(@Param("proxyType") RotationType proxyType);
}
