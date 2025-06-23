package com.api.digicell.services;

import com.api.digicell.entities.AssistantConfiguration;
import com.api.digicell.repository.AssistantConfigurationRepository;
import com.api.digicell.responses.TenantInfoResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TenantInfoService {
    
    private static final Logger logger = LoggerFactory.getLogger(TenantInfoService.class);
    
    private final AssistantConfigurationRepository assistantConfigurationRepository;
    
    @Value("${app.quickation.items:Special Offers,DTH offers,Digital apps,Contact us}")
    private String quickationItemsConfig;
    
    /**
     * Get tenant information by assistant ID
     * 
     * @param assistantId the assistant ID to look up
     * @return TenantInfoResponse containing tenant ID and quickation items
     * @throws IllegalArgumentException if assistant is not found
     */
    public TenantInfoResponse getTenantInfoByAssistantId(Integer assistantId) {
        logger.info("Fetching tenant info for assistant ID: {}", assistantId);
        
        AssistantConfiguration assistantConfig = assistantConfigurationRepository.findById(assistantId)
                .orElseThrow(() -> {
                    logger.warn("Assistant configuration not found for ID: {}", assistantId);
                    return new IllegalArgumentException("Assistant configuration not found for ID: " + assistantId);
                });
        
        String tenantId = assistantConfig.getTenantId();
        List<String> quickationItems = getQuickationItems();
        
        logger.debug("Found tenant ID: {} for assistant ID: {}", tenantId, assistantId);
        
        return new TenantInfoResponse(tenantId, quickationItems);
    }
    
    /**
     * Parse quickation items from configuration
     * 
     * @return List of quickation items
     */
    private List<String> getQuickationItems() {
        return Arrays.stream(quickationItemsConfig.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .collect(Collectors.toList());
    }
} 