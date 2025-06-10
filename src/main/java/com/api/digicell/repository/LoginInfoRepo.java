package com.api.digicell.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.api.digicell.entities.Agent;

@Repository
public interface LoginInfoRepo extends JpaRepository<Agent, Long> {
    boolean existsByEmail(String email);
} 