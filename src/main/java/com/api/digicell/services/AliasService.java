package com.api.digicell.services;

import com.api.digicell.dto.AliasCreateDTO;
import com.api.digicell.dto.AliasResponseDTO;
import com.api.digicell.dto.AliasUpdateDTO;
import com.api.digicell.entities.Alias;
import com.api.digicell.exceptions.ResourceNotFoundException;
import com.api.digicell.mapper.AliasMapper;
import com.api.digicell.repository.AliasRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer encapsulating business logic for {@link Alias} CRUD operations.
 */
@Service
@RequiredArgsConstructor
public class AliasService {

    private static final Logger logger = LoggerFactory.getLogger(AliasService.class);
    private final AliasRepository aliasRepository;
    private final AliasMapper aliasMapper;

    /**
     * Create a new alias.
     */
    @Transactional
    public AliasResponseDTO createAlias(AliasCreateDTO createDTO) {
        logger.info("Creating new alias with key: {}", createDTO.getKey());
        logger.debug("Alias creation request details - key: {}, value: {}", createDTO.getKey(), createDTO.getValue());
        
        Alias alias = aliasMapper.toEntity(createDTO);
        Alias savedAlias = aliasRepository.save(alias);
        logger.info("Successfully created alias with key: {}", savedAlias.getKey());
        logger.debug("Created alias details - key: {}, value: {}", savedAlias.getKey(), savedAlias.getValue());
        
        return aliasMapper.toResponseDTO(savedAlias);
    }

    /**
     * Retrieve all aliases.
     */
    public List<AliasResponseDTO> getAllAliases() {
        logger.info("Fetching all aliases");
        List<Alias> aliases = aliasRepository.findAll();
        List<AliasResponseDTO> responseDTOs = aliases.stream()
                .map(aliasMapper::toResponseDTO)
                .collect(Collectors.toList());
        logger.info("Successfully retrieved {} aliases", responseDTOs.size());
        logger.debug("Retrieved aliases - count: {}, keys: {}", 
            responseDTOs.size(), responseDTOs.stream().map(AliasResponseDTO::getKey).collect(Collectors.toList()));
        return responseDTOs;
    }

    /**
     * Fetch a single alias by its unique key.
     */
    public AliasResponseDTO getAliasByKey(String key) {
        logger.info("Fetching alias with key: {}", key);
        Alias alias = aliasRepository.findByKey(key)
                .orElseThrow(() -> {
                    logger.error("Alias not found with key: {}", key);
                    logger.debug("Failed to find alias with key: {}", key);
                    return new ResourceNotFoundException("Alias not found with key: " + key);
                });
        logger.info("Successfully retrieved alias with key: {}", key);
        logger.debug("Retrieved alias details - key: {}, value: {}", alias.getKey(), alias.getValue());
        return aliasMapper.toResponseDTO(alias);
    }

    /**
     * Update an existing alias using its key as identifier.
     */
    @Transactional
    public AliasResponseDTO updateAlias(String key, AliasUpdateDTO updateDTO) {
        logger.info("Updating alias with key: {}", key);
        logger.debug("Alias update request details - key: {}, new value: {}", key, updateDTO.getValue());
        
        Alias alias = aliasRepository.findByKey(key)
                .orElseThrow(() -> {
                    logger.error("Alias not found with key: {}", key);
                    logger.debug("Failed to find alias for update with key: {}", key);
                    return new ResourceNotFoundException("Alias not found with key: " + key);
                });
        
        aliasMapper.updateEntity(alias, updateDTO);
        Alias updatedAlias = aliasRepository.save(alias);
        logger.info("Successfully updated alias with key: {}", key);
        logger.debug("Updated alias details - key: {}, value: {}", updatedAlias.getKey(), updatedAlias.getValue());
        
        return aliasMapper.toResponseDTO(updatedAlias);
    }

    /**
     * Delete an alias by its key.
     */
    @Transactional
    public void deleteAlias(String key) {
        logger.info("Attempting to delete alias with key: {}", key);
        Alias alias = aliasRepository.findByKey(key)
                .orElseThrow(() -> {
                    logger.error("Alias not found with key: {}", key);
                    logger.debug("Failed to find alias for deletion with key: {}", key);
                    return new ResourceNotFoundException("Alias not found with key: " + key);
                });
        
        aliasRepository.delete(alias);
        logger.info("Successfully deleted alias with key: {}", key);
        logger.debug("Deleted alias details - key: {}, value: {}", alias.getKey(), alias.getValue());
    }
} 