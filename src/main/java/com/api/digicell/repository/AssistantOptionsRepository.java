package com.api.digicell.repository;

import com.api.digicell.entities.AssistantOptions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssistantOptionsRepository extends JpaRepository<AssistantOptions, Long> {
    
    /**
     * Find assistant options by assistant ID and tenant ID
     */
    Optional<AssistantOptions> findByAssistantIdAndTenantId(Integer assistantId, String tenantId);
    
    /**
     * Find assistant options by assistant ID only (for cases where tenant is not specified)
     */
    Optional<AssistantOptions> findByAssistantId(Integer assistantId);
    
    /**
     * Find first assistant options by assistant ID (fallback when tenant doesn't match)
     */
    Optional<AssistantOptions> findFirstByAssistantId(Integer assistantId);
    
    /**
     * Check if assistant options exist for assistant ID
     */
    boolean existsByAssistantId(Integer assistantId);
    
    /**
     * Find all assistant options for a specific tenant
     */
    List<AssistantOptions> findByTenantId(String tenantId);
    
    /**
     * Custom query to get options with assistant configuration details
     */
    @Query("SELECT ao FROM AssistantOptions ao " +
           "JOIN FETCH ao.assistantConfiguration ac " +
           "WHERE ao.assistantId = :assistantId AND ao.tenantId = :tenantId")
    Optional<AssistantOptions> findByAssistantIdAndTenantIdWithConfig(
            @Param("assistantId") Integer assistantId, 
            @Param("tenantId") String tenantId);
} 