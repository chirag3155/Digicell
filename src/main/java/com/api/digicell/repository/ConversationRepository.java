package com.api.digicell.repository;

import com.api.digicell.entities.Conversation;
import com.api.digicell.entities.Client;
import com.api.digicell.entities.Agent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    /**
     * Fetch all conversations belonging to a given client id.
     */
    List<Conversation> findByClient_ClientId(Long clientId);

    /**
     * Fetch all conversations assigned to a given agent id.
     */
    List<Conversation> findByAgent_AgentId(Long agentId);

    /**
     * Fetch conversations involving a specific client and handled by a specific agent.
     */
    List<Conversation> findByAgent_AgentIdAndClient_ClientId(Long agentId, Long clientId);

    List<Conversation> findByClientAndAgent(Client client, Agent agent);

    Optional<Conversation> findByClientAndAgentAndEndTimeIsNull(Client client, Agent agent);
} 