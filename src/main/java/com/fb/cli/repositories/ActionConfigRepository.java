package com.fb.cli.repositories;

import com.fb.cli.entities.ActionConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ActionConfigRepository extends JpaRepository<ActionConfig, UUID> {

    @Query("SELECT ac FROM ActionConfig ac " +
           "WHERE UPPER(ac.platform) = UPPER(:platform) " +
           "AND UPPER(ac.actionType) = UPPER(:actionType) " +
           "AND UPPER(ac.actionProvider) = UPPER(:actionProvider) " +
           "AND ac.isActive = true " +
           "ORDER BY ac.version DESC LIMIT 1")
    Optional<ActionConfig> findActiveConfig(
            @Param("platform") String platform,
            @Param("actionType") String actionType,
            @Param("actionProvider") String actionProvider
    );
}
