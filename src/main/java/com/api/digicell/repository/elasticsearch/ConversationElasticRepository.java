package com.api.digicell.repository.elasticsearch;

import com.api.digicell.document.ConversationDocument;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Lazy
public interface ConversationElasticRepository extends ElasticsearchRepository<ConversationDocument, String> {
    List<ConversationDocument> findByUserId(Long userId);
    List<ConversationDocument> findByClientId(String clientId);
    List<ConversationDocument> findByUserIdAndClientId(Long userId, String clientId);
    Optional<ConversationDocument> findByConversationIdAndClientId(String conversationId, String clientId);
} 