package com.api.digicell.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.api.digicell.entities.ChatMessage;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * JPA converter that converts List<List<ChatMessage>> to JSON String for persistence
 * and vice-versa.
 */
@Converter
public class ChatHistoryConverter implements AttributeConverter<List<List<ChatMessage>>, String> {
    private static final Logger logger = LoggerFactory.getLogger(ChatHistoryConverter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override
    public String convertToDatabaseColumn(List<List<ChatMessage>> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            logger.debug("Converting empty chat history to JSON");
            return "[]";
        }
        try {
            logger.debug("Converting chat history with {} sessions to JSON", attribute.size());
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            logger.error("Error converting chat history to JSON: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Unable to convert chat history to JSON", e);
        }
    }

    @Override
    public List<List<ChatMessage>> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            logger.debug("Converting empty JSON to chat history");
            return Collections.emptyList();
        }
        try {
            logger.debug("Converting JSON to chat history");
            return objectMapper.readValue(dbData, 
                objectMapper.getTypeFactory().constructCollectionType(List.class, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ChatMessage.class)));
        } catch (JsonProcessingException e) {
            logger.error("Error converting JSON to chat history: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Unable to convert JSON to chat history", e);
        }
    }
} 