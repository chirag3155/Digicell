package com.api.digicell.repository.elasticsearch;

import com.api.digicell.document.ConversationDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationElasticRepository extends ElasticsearchRepository<ConversationDocument, String> {
    List<ConversationDocument> findByUserId(Long userId);
    List<ConversationDocument> findByClientId(Long clientId);
    List<ConversationDocument> findByUserIdAndClientId(Long userId, Long clientId);
    Optional<ConversationDocument> findByConversationIdAndClientId(Long conversationId, Long clientId);
} 