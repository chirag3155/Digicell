package com.api.digicell.repository;

import com.api.digicell.entities.AssistantConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AssistantConfigurationRepository extends JpaRepository<AssistantConfiguration, Integer> {
    
    /**
     * Find assistant configuration by assistant ID (which is the primary key)
     */
    Optional<AssistantConfiguration> findById(Integer assistantId);
} 