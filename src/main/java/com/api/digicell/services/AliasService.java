package com.api.digicell.services;

import com.api.digicell.entities.Alias;
import com.api.digicell.exceptions.ResourceNotFoundException;
import com.api.digicell.repository.AliasRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service layer encapsulating business logic for {@link Alias} CRUD operations.
 */
@Service
@RequiredArgsConstructor
public class AliasService {

    private final AliasRepository aliasRepository;

    /**
     * Create a new alias.
     */
    public Alias createAlias(Alias alias) {
        return aliasRepository.save(alias);
    }

    /**
     * Retrieve all aliases.
     */
    public List<Alias> getAllAliases() {
        return aliasRepository.findAll();
    }

    /**
     * Fetch a single alias by its unique key.
     */
    public Alias getAliasByKey(String key) {
        return aliasRepository.findByKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("Alias not found with key: " + key));
    }

    /**
     * Update an existing alias using its key as identifier.
     */
    @Transactional
    public Alias updateAlias(String key, Alias updatedAlias) {
        Alias existing = aliasRepository.findByKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("Alias not found with key: " + key));
        existing.setValue(updatedAlias.getValue());
        existing.setKey(updatedAlias.getKey());
        return existing; // entity is managed; will be flushed automatically
    }

    /**
     * Delete alias by key.
     */
    public void deleteAlias(String key) {
        if (!aliasRepository.existsByKey(key)) {
            throw new ResourceNotFoundException("Alias not found with key: " + key);
        }
        aliasRepository.deleteByKey(key);
    }
} 