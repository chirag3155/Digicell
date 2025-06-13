package com.api.digicell.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

/**
 * Enhanced logging utility class that provides additional logging functionality
 * on top of the existing SLF4J logging infrastructure.
 * This class complements the existing ChatUtils logging methods.
 */
@Component
public class LoggingUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(LoggingUtils.class);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    // MDC Keys for structured logging
    public static final String MDC_USER_ID = "userId";
    public static final String MDC_SESSION_ID = "sessionId";
    public static final String MDC_REQUEST_ID = "requestId";
    public static final String MDC_AGENT_ID = "agentId";
    public static final String MDC_CONVERSATION_ID = "conversationId";
    
    /**
     * Initialize a logging context with a unique request ID
     */
    public static String initializeRequestContext() {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put(MDC_REQUEST_ID, requestId);
        return requestId;
    }
    
    /**
     * Set user context for logging
     */
    public static void setUserContext(String userId) {
        if (userId != null && !userId.trim().isEmpty()) {
            MDC.put(MDC_USER_ID, userId);
        }
    }
    
    /**
     * Set agent context for logging
     */
    public static void setAgentContext(String agentId) {
        if (agentId != null && !agentId.trim().isEmpty()) {
            MDC.put(MDC_AGENT_ID, agentId);
        }
    }
    
    /**
     * Set conversation context for logging
     */
    public static void setConversationContext(String conversationId) {
        if (conversationId != null && !conversationId.trim().isEmpty()) {
            MDC.put(MDC_CONVERSATION_ID, conversationId);
        }
    }
    
    /**
     * Set session context for logging
     */
    public static void setSessionContext(String sessionId) {
        if (sessionId != null && !sessionId.trim().isEmpty()) {
            MDC.put(MDC_SESSION_ID, sessionId);
        }
    }
    
    /**
     * Clear all MDC context
     */
    public static void clearContext() {
        MDC.clear();
    }
    
    /**
     * Clear specific MDC key
     */
    public static void clearContext(String key) {
        MDC.remove(key);
    }
    
    /**
     * Get current MDC context as map
     */
    public static Map<String, String> getCurrentContext() {
        return MDC.getCopyOfContextMap();
    }
    
    /**
     * Log method entry with parameters
     */
    public static void logMethodEntry(Logger logger, String methodName, Object... params) {
        if (logger.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Entering method: ").append(methodName);
            if (params != null && params.length > 0) {
                sb.append(" with parameters: ");
                for (int i = 0; i < params.length; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(params[i]);
                }
            }
            logger.debug(sb.toString());
        }
    }
    
    /**
     * Log method exit with return value
     */
    public static void logMethodExit(Logger logger, String methodName, Object returnValue) {
        if (logger.isDebugEnabled()) {
            logger.debug("Exiting method: {} with return value: {}", methodName, returnValue);
        }
    }
    
    /**
     * Log method exit without return value
     */
    public static void logMethodExit(Logger logger, String methodName) {
        if (logger.isDebugEnabled()) {
            logger.debug("Exiting method: {}", methodName);
        }
    }
    
    /**
     * Log execution time of a method
     */
    public static void logExecutionTime(Logger logger, String methodName, long startTime) {
        long executionTime = System.currentTimeMillis() - startTime;
        if (logger.isDebugEnabled()) {
            logger.debug("Method {} executed in {} ms", methodName, executionTime);
        }
        
        // Log slow operations as warnings
        if (executionTime > 1000) {
            logger.warn("Slow operation detected: {} took {} ms", methodName, executionTime);
        }
    }
    
    /**
     * Log database operation
     */
    public static void logDatabaseOperation(Logger logger, String operation, String entity, Object id) {
        logger.info("Database operation: {} on entity: {} with id: {}", operation, entity, id);
    }
    
    /**
     * Log API request
     */
    public static void logApiRequest(Logger logger, String method, String endpoint, String userId) {
        setUserContext(userId);
        logger.info("API Request: {} {} by user: {}", method, endpoint, userId);
    }
    
    /**
     * Log API response
     */
    public static void logApiResponse(Logger logger, String method, String endpoint, int statusCode, long responseTime) {
        logger.info("API Response: {} {} - Status: {} - Time: {}ms", method, endpoint, statusCode, responseTime);
    }
    
    /**
     * Log business operation
     */
    public static void logBusinessOperation(Logger logger, String operation, String details) {
        logger.info("Business Operation: {} - {}", operation, details);
    }
    
    /**
     * Log security event
     */
    public static void logSecurityEvent(Logger logger, String event, String details) {
        logger.warn("Security Event: {} - {}", event, details);
    }
    
    /**
     * Log system event
     */
    public static void logSystemEvent(Logger logger, String event, String details) {
        logger.info("System Event: {} - {}", event, details);
    }
    
    /**
     * Create a formatted timestamp string
     */
    public static String getCurrentTimestamp() {
        return LocalDateTime.now().format(TIMESTAMP_FORMATTER);
    }
    
    /**
     * Log performance metrics
     */
    public static void logPerformanceMetrics(Logger logger, String operation, Map<String, Object> metrics) {
        StringBuilder sb = new StringBuilder();
        sb.append("Performance Metrics for ").append(operation).append(": ");
        metrics.forEach((key, value) -> sb.append(key).append("=").append(value).append(" "));
        logger.info(sb.toString());
    }
} 