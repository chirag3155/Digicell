package com.api.digicell.repository;

import com.api.digicell.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    List<User> findByIsAssigned(Boolean isAssigned);

    @Query("SELECT DISTINCT c.user FROM Conversation c WHERE c.agent.agentId = :agentId")
    List<User> findActiveUsersByAgentId(@Param("agentId") Long agentId);
} 