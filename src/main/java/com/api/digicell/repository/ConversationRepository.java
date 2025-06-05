package com.api.digicell.repository;

import com.api.digicell.entities.Conversation;
import com.api.digicell.entities.User;
import com.api.digicell.entities.Agent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    /**
     * Fetch all conversations belonging to a given user id.
     */
    List<Conversation> findByUser_UserId(Long userId);

    /**
     * Fetch all conversations assigned to a given agent id.
     */
    List<Conversation> findByAgent_AgentId(Long agentId);

    /**
     * Fetch conversations involving a specific user and handled by a specific agent.
     */
    List<Conversation> findByAgent_AgentIdAndUser_UserId(Long agentId, Long userId);

    List<Conversation> findByUserAndAgent(User user, Agent agent);

    Optional<Conversation> findByUserAndAgentAndEndTimeIsNull(User user, Agent agent);
} 