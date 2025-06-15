package com.api.digicell.repository;

import com.api.digicell.entities.Conversation;
import com.api.digicell.entities.Client;
import com.api.digicell.entities.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, String> {

    /**
     * Fetch all conversations assigned to a given agent id.
     */
    List<Conversation> findByUserAccount_UserId(Long userId);

    List<Conversation> findByClient_ClientId(String clientId);

    List<Conversation> findByUserAccount_UserIdAndClient_ClientId(Long userId, String clientId);

    Optional<Conversation> findByConversationIdAndClient_ClientId(String conversationId, String clientId);

    Optional<Conversation> findByClientAndUserAccountAndEndTimeIsNull(Client client, UserAccount userAccount);

    /**
     * Delete all conversations for a given user id.
     */
    void deleteByUserAccount_UserId(Long userId);
} 