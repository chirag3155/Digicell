package com.api.digicell.repository;

import com.api.digicell.entities.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClientRepository extends JpaRepository<Client, Long> {

    List<Client> findByIsAssigned(Boolean isAssigned);

    @Query("SELECT DISTINCT c.client FROM Conversation c WHERE c.agent.agentId = :agentId")
    List<Client> findActiveClientsByAgentId(@Param("agentId") Long agentId);
} 