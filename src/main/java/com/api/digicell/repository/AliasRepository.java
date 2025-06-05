package com.api.digicell.repository;

import com.api.digicell.entities.Alias;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for {@link Alias} entity.
 */
public interface AliasRepository extends JpaRepository<Alias, Long> {
    Optional<Alias> findByKey(String key);

    void deleteByKey(String key);

    boolean existsByKey(String key);
} 