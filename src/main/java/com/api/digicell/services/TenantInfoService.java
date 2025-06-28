package com.api.digicell.services;

import com.api.digicell.entities.AssistantConfiguration;
import com.api.digicell.entities.AssistantOptions;
import com.api.digicell.repository.AssistantConfigurationRepository;
import com.api.digicell.repository.AssistantOptionsRepository;
import com.api.digicell.responses.TenantInfoResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TenantInfoService {
    
    private static final Logger logger = LoggerFactory.getLogger(TenantInfoService.class);
    
    private final AssistantConfigurationRepository assistantConfigurationRepository;
    private final AssistantOptionsRepository assistantOptionsRepository;
    
    @Value("${app.quickation.items}")
    private String quickationItemsConfig;
    
    /**
     * Get tenant information by assistant ID
     * 
     * @param assistantId the assistant ID to look up
     * @return TenantInfoResponse containing assistant_id, name, tenant_id and options
     * @throws IllegalArgumentException if assistant is not found
     */
    public TenantInfoResponse getTenantInfoByAssistantId(Integer assistantId) {
        logger.info("Fetching tenant info for assistant ID: {}", assistantId);
        
        // First get the assistant configuration to ensure it exists
        AssistantConfiguration assistantConfig = assistantConfigurationRepository.findById(assistantId)
                .orElseThrow(() -> {
                    logger.warn("Assistant configuration not found for ID: {}", assistantId);
                    return new IllegalArgumentException("Assistant configuration not found for ID: " + assistantId);
                });
        
        String assistantName = assistantConfig.getName();
        String tenantId = assistantConfig.getTenantId();
        
        logger.debug("Found assistant: name={}, tenantId={} for assistantId={}", assistantName, tenantId, assistantId);
        
        // Try to get options from assistant_options table
        List<String> options = getOptionsWithFallback(assistantId);
        
        return new TenantInfoResponse(assistantId, assistantName, tenantId, options);
    }
    
    /**
     * Get options with fallback mechanism:
     * 1. Try to get from assistant_options table by assistant_id
     * 2. If not found, use property file values
     * 3. If property file is empty, return empty list
     * 
     * @param assistantId the assistant ID
     * @return List of options or empty list
     */
    private List<String> getOptionsWithFallback(Integer assistantId) {
        logger.debug("Getting options for assistantId={}", assistantId);
        
        // Step 1: Try to find by assistant_id
        Optional<AssistantOptions> optionsById = assistantOptionsRepository.findByAssistantId(assistantId);
        if (optionsById.isPresent()) {
            List<String> options = optionsById.get().getOptionsAsList();
            logger.debug("Found options from DB by assistantId: {} options", options.size());
            
            // If options exist and are not empty, return them
            if (!options.isEmpty()) {
                return options;
            }
            
            logger.debug("Options from DB are empty, falling back to property file");
        }
        
        // Step 2: Fall back to property file values
        List<String> propertyOptions = getQuickationItems();
        if (!propertyOptions.isEmpty()) {
            logger.debug("Using options from property file: {} options", propertyOptions.size());
            return propertyOptions;
        }
        
        // Step 3: Return empty list if nothing found
        logger.debug("No options found anywhere, returning empty list");
        return List.of();
    }
    
    /**
     * Parse quickation items from configuration
     * 
     * @return List of quickation items
     */
    private List<String> getQuickationItems() {
        if (quickationItemsConfig == null || quickationItemsConfig.trim().isEmpty()) {
            logger.debug("Property app.quickation.items is null or empty");
            return List.of();
        }
        
        return Arrays.stream(quickationItemsConfig.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .collect(Collectors.toList());
    }
} 