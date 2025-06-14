package com.api.digicell.repository;

import com.api.digicell.entities.Conversation;
import com.api.digicell.entities.Client;
import com.api.digicell.entities.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    /**
     * Fetch all conversations assigned to a given user id.
     */
    List<Conversation> findByUserAccount_UserId(Long userId);

    List<Conversation> findByClient_ClientId(Long clientId);

    List<Conversation> findByUserAccount_UserIdAndClient_ClientId(Long userId, Long clientId);

    Optional<Conversation> findByConversationIdAndClient_ClientId(Long conversationId, Long clientId);

    Optional<Conversation> findByClientAndUserAccountAndEndTimeIsNull(Client client, UserAccount userAccount);

    /**
     * Delete all conversations for a given user id.
     */
    void deleteByUserAccount_UserId(Long userId);
} 