package com.fb.cli.repositories;

import com.fb.cli.entities.ProxyProvider;
import com.fb.cli.enums.ProviderStrategy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProxyProviderRepository extends JpaRepository<ProxyProvider, UUID> {
    Optional<ProxyProvider> findByName(String name);
    Optional<ProxyProvider> findByStrategy(ProviderStrategy strategy);
}
