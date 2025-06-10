package com.api.digicell.repository;

import com.api.digicell.entities.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    List<Client> findByIsAssigned(Boolean isAssigned);

    @Query("SELECT DISTINCT c.client FROM Conversation c WHERE c.agent.agentId = :agentId")
    List<Client> findByAgent_AgentId(@Param("agentId") Long agentId);
} 