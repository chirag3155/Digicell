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
     * Fetch all conversations assigned to a given agent id.
     */
    List<Conversation> findByAgent_AgentId(Long agentId);

    List<Conversation> findByClient_ClientId(Long clientId);

    List<Conversation> findByAgent_AgentIdAndClient_ClientId(Long agentId, Long clientId);

    Optional<Conversation> findByConversationIdAndClient_ClientId(Long conversationId, Long clientId);

    Optional<Conversation> findByClientAndAgentAndEndTimeIsNull(Client client, Agent agent);
} 