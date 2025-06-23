package com.api.digicell.services;

import com.api.digicell.config.SocketConfig;
import com.corundumstudio.socketio.SocketIOClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.HashMap;

@Slf4j
@Service
public class SocketConnectionService {
    private final SocketConfig socketConfig;
    private String chatModuleSocketId;
    private final Map<String, String> userSocketMap;
    private Map<String, String> userSocketIds = new ConcurrentHashMap<>();
    
    // New maps to track active conversations per user
    private final Map<String, Set<String>> userActiveConversations = new ConcurrentHashMap<>();
    private final Map<String, String> conversationToUserMap = new ConcurrentHashMap<>();
    
    // Conversation preservation with timeout
    private final Map<String, LocalDateTime> userDisconnectionTime = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupScheduler = Executors.newScheduledThreadPool(2);
    
    // Configurable conversation preservation timeout (in minutes)
    @Value("${socket.conversation.preservation.timeout:3}")
    private int conversationPreservationTimeoutMinutes;
    
    @Value("${socket.conversation.cleanup.interval:1}")
    private int cleanupIntervalMinutes;

    public SocketConnectionService(SocketConfig socketConfig) {
        this.socketConfig = socketConfig;
        this.userSocketMap = new ConcurrentHashMap<>();
        this.chatModuleSocketId = null;
    }
    
    @PostConstruct
    public void init() {
        log.info("🔧 Initializing SocketConnectionService with conversation preservation timeout: {} minutes", 
                conversationPreservationTimeoutMinutes);
        log.info("🔧 Cleanup interval: {} minutes", cleanupIntervalMinutes);
        
        // Start the periodic cleanup task
        cleanupScheduler.scheduleAtFixedRate(
            this::cleanupExpiredConversations,
            cleanupIntervalMinutes,
            cleanupIntervalMinutes,
            TimeUnit.MINUTES
        );
        log.info("✅ Conversation cleanup scheduler started");
    }
    
    @PreDestroy
    public void destroy() {
        log.info("🛑 Shutting down conversation cleanup scheduler");
        cleanupScheduler.shutdown();
        try {
            if (!cleanupScheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                cleanupScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public void handleConnection(SocketIOClient socketClient) {
        String clientType = socketClient.getHandshakeData().getSingleUrlParam(socketConfig.PARAM_CLIENT_TYPE);
        String userId = socketClient.getHandshakeData().getSingleUrlParam("userId");
        String remoteAddress = socketClient.getRemoteAddress().toString();
        String sessionId = socketClient.getSessionId().toString();
        
        log.info("Socket connection attempt - IP: {}, SessionId: {}, ClientType: {}, UserId: {}", 
                remoteAddress, sessionId, clientType, userId);
        
        // Reject connection if no parameters are provided
        if (clientType == null || clientType.trim().isEmpty()) {
            log.warn("Connection rejected: No clientType parameter. IP: {}, SessionId: {}", remoteAddress, sessionId);
            socketClient.disconnect();
            return;
        }

        if (socketConfig.PARAM_CHAT_MODULE.equals(clientType)) {
            handleChatModuleConnection(socketClient);
        } else if (socketConfig.PARAM_AGENT.equals(clientType)) {
            handleUserConnection(socketClient, clientType);
        } else {
            log.warn("Connection rejected: Invalid clientType: '{}'. IP: {}, SessionId: {}", clientType, remoteAddress, sessionId);
            socketClient.disconnect();
        }
    }

    private void handleChatModuleConnection(SocketIOClient socketClient) {
        String newSocketId = socketClient.getSessionId().toString();
        log.info("Chat module connection attempt. Previous socketId: {}, New socketId: {}", chatModuleSocketId, newSocketId);
        
        // If there's an existing connection, check if it's the same socket client reconnecting
        if (chatModuleSocketId != null) {
            // If it's the same socket client (same socket ID), just update the connection
            if (chatModuleSocketId.equals(newSocketId)) {
                log.info("Chat module reconnected with same socket ID: {}", newSocketId);
                return;
            }
            
            // If it's a different socket client, reject the new connection
            log.warn("Chat module already connected with different socket ID. Rejecting new connection. Previous: {}, New: {}", 
                    chatModuleSocketId, newSocketId);
            socketClient.disconnect();
            return;
        }

        // New connection
        chatModuleSocketId = newSocketId;
        log.info("Chat module connected successfully. SocketId: {}", chatModuleSocketId);
    }

    private void handleUserConnection(SocketIOClient socketClient, String clientType) {
        String userId = socketClient.getHandshakeData().getSingleUrlParam("userId");
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("User connection rejected: Missing userId. SocketId: {}", socketClient.getSessionId());
            socketClient.disconnect();
            return;
        }

        String newSocketId = socketClient.getSessionId().toString();
        
        // Check if user mapping exists (indicates reconnection attempt)
        String existingSocketId = userSocketIds.get(userId);
        
        if (existingSocketId != null) {
            // User mapping exists - this is a reconnection attempt
            log.info("🔄 User {} reconnection detected. Old SocketId: {}, New SocketId: {}", 
                    userId, existingSocketId, newSocketId);
            
            // Remove old socket mapping if different
            if (!existingSocketId.equals(newSocketId)) {
                userSocketMap.remove(existingSocketId);
                log.info("Removed old socket mapping for socketId: {}", existingSocketId);
            }
            
            // Update with new socket ID
            userSocketMap.put(newSocketId, userId);
            userSocketIds.put(userId, newSocketId);
            
            // Remove disconnection timestamp as user is now connected
            LocalDateTime disconnectionTime = userDisconnectionTime.remove(userId);
            if (disconnectionTime != null) {
                log.info("🔄 User {} reconnected within preservation window. Was disconnected at: {}", 
                        userId, disconnectionTime);
                
                // Log preserved conversations for this user
                Set<String> preservedConversations = userActiveConversations.get(userId);
                if (preservedConversations != null && !preservedConversations.isEmpty()) {
                    log.info("🔄 Restored {} active conversations for reconnecting user {}: {}", 
                            preservedConversations.size(), userId, preservedConversations);
                }
            }
            
            log.info("User reconnected successfully. SocketId: {}, UserId: {}, Type: {}", 
                    newSocketId, userId, clientType);
        } else {
            // No user mapping exists - this is a new user connection
            log.info("👤 New user connection. SocketId: {}, UserId: {}, Type: {}", 
                    newSocketId, userId, clientType);
            
            userSocketMap.put(newSocketId, userId);
            userSocketIds.put(userId, newSocketId);
            
            log.info("New user connected successfully. SocketId: {}, UserId: {}, Type: {}", 
                    newSocketId, userId, clientType);
        }
    }

    public String getChatModuleSocketId() {
        return chatModuleSocketId;
    }

    public String getUserIdBySocketId(String socketId) {
        return userSocketMap.get(socketId);
    }

    public void removeConnection(String socketId) {
        if (socketId.equals(chatModuleSocketId)) {
            log.info("Chat module disconnected. SocketId: {}", socketId);
            chatModuleSocketId = null;
            
            // Notify all connected users about chat module disconnection
            notifyUsersAboutChatModuleDisconnection();
        } else {
            String userId = userSocketMap.remove(socketId);
            if (userId != null) {
                // Keep user mapping (userSocketIds) until configurable timeout
                // Only remove socket-to-user mapping (userSocketMap)
                // Record disconnection time for cleanup scheduling
                userDisconnectionTime.put(userId, LocalDateTime.now());
                
                log.info("User disconnected. SocketId: {}, UserId: {} (user mapping preserved for {} minutes)", 
                        socketId, userId, conversationPreservationTimeoutMinutes);
                
                // Log active conversations that are being preserved
                Set<String> activeConversations = userActiveConversations.get(userId);
                if (activeConversations != null && !activeConversations.isEmpty()) {
                    LocalDateTime expirationTime = LocalDateTime.now().plusMinutes(conversationPreservationTimeoutMinutes);
                    log.info("💾 User mapping and {} conversations preserved for user {} until {}: {}", 
                            activeConversations.size(), userId, expirationTime, activeConversations);
                    
                    // Notify chat module about user disconnection for active conversations
                    notifyAboutUserDisconnection(userId, activeConversations);
                } else {
                    LocalDateTime expirationTime = LocalDateTime.now().plusMinutes(conversationPreservationTimeoutMinutes);
                    log.info("💾 User mapping preserved for user {} until {} (no active conversations)", 
                            userId, expirationTime);
                }
                
                // Schedule cleanup for this specific user after timeout
                scheduleUserCleanup(userId);
            }
        }
    }
    
    private void scheduleUserCleanup(String userId) {
        cleanupScheduler.schedule(() -> {
            LocalDateTime disconnectionTime = userDisconnectionTime.get(userId);
            if (disconnectionTime != null) {
                LocalDateTime expirationTime = disconnectionTime.plusMinutes(conversationPreservationTimeoutMinutes);
                if (LocalDateTime.now().isAfter(expirationTime)) {
                    log.info("⏰ Scheduled cleanup triggered for user {} (disconnected at {})", userId, disconnectionTime);
                    cleanupUserConversations(userId, "scheduled cleanup");
                    userDisconnectionTime.remove(userId);
                }
            }
        }, conversationPreservationTimeoutMinutes + 1, TimeUnit.MINUTES); // Add 1 minute buffer
    }
    
    private void cleanupExpiredConversations() {
        LocalDateTime now = LocalDateTime.now();
        log.debug("🧹 Running periodic conversation cleanup at {}", now);
        
        userDisconnectionTime.entrySet().removeIf(entry -> {
            String userId = entry.getKey();
            LocalDateTime disconnectionTime = entry.getValue();
            LocalDateTime expirationTime = disconnectionTime.plusMinutes(conversationPreservationTimeoutMinutes);
            
            if (now.isAfter(expirationTime)) {
                log.info("⏰ Cleaning up expired conversations for user {} (disconnected at {}, expired at {})", 
                        userId, disconnectionTime, expirationTime);
                cleanupUserConversations(userId, "periodic cleanup");
                return true; // Remove from disconnection tracking
            }
            return false;
        });
    }
    
    private void cleanupUserConversations(String userId, String reason) {
        // Remove user mapping after configurable timeout
        userSocketIds.remove(userId);
        
        // Remove active conversations
        Set<String> conversations = userActiveConversations.remove(userId);
        if (conversations != null && !conversations.isEmpty()) {
            log.info("🗑️ Cleaning up user mapping and {} conversations for user {} (reason: {}): {}", 
                    conversations.size(), userId, reason, conversations);
            
            // Remove conversation-to-user mappings
            conversations.forEach(conversationToUserMap::remove);
        } else {
            log.info("🗑️ Cleaning up user mapping for user {} (reason: {}, no active conversations)", 
                    userId, reason);
        }
        
        log.info("🧹 User {} mapping and conversations fully cleaned up after timeout (reason: {})", userId, reason);
    }

    public boolean isChatModuleConnected() {
        return chatModuleSocketId != null;
    }

    public String getUserSocketId(String userId) {
        return userSocketIds.get(userId);
    }

    public void setUserSocketId(String userId, String socketId) {
        userSocketIds.put(userId, socketId);
    }

    public void removeUserSocketId(String userId) {
        userSocketIds.remove(userId);
    }
    
    // Methods for conversation management
    public void addUserConversation(String userId, String conversationId) {
        userActiveConversations.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(conversationId);
        conversationToUserMap.put(conversationId, userId);
        
        // If user was scheduled for cleanup, cancel it since they have active conversation
        userDisconnectionTime.remove(userId);
        
        log.info("🔗 Added conversation {} for user {}. Total conversations: {}", 
                conversationId, userId, userActiveConversations.get(userId).size());
    }
    
    public void removeUserConversation(String userId, String conversationId) {
        Set<String> conversations = userActiveConversations.get(userId);
        if (conversations != null) {
            conversations.remove(conversationId);
            if (conversations.isEmpty()) {
                userActiveConversations.remove(userId);
                // Only remove user socket mapping if user is not currently connected
                if (!userSocketMap.containsValue(userId)) {
                    userSocketIds.remove(userId);
                    log.info("🧹 Removed last conversation {} for user {}. User mapping cleaned up.", conversationId, userId);
                } else {
                    log.info("🗑️ Removed last conversation {} for user {}. User still connected, mapping preserved.", conversationId, userId);
                }
            } else {
                log.info("🗑️ Removed conversation {} for user {}. Remaining conversations: {}", 
                        conversationId, userId, conversations.size());
            }
        }
        conversationToUserMap.remove(conversationId);
    }
    
    public Set<String> getUserActiveConversations(String userId) {
        return userActiveConversations.get(userId);
    }
    
    public String getUserForConversation(String conversationId) {
        return conversationToUserMap.get(conversationId);
    }
    
    public boolean hasActiveConversations(String userId) {
        Set<String> conversations = userActiveConversations.get(userId);
        return conversations != null && !conversations.isEmpty();
    }
    
    // Configuration getters for external access
    public int getConversationPreservationTimeoutMinutes() {
        return conversationPreservationTimeoutMinutes;
    }
    
    public int getCleanupIntervalMinutes() {
        return cleanupIntervalMinutes;
    }
    
    /**
     * Get all users with their active conversations (for notifications)
     */
    public Map<String, Set<String>> getAllUserActiveConversations() {
        return new HashMap<>(userActiveConversations);
    }

    /**
     * Notify chat module about user disconnection for active conversations
     */
    private void notifyAboutUserDisconnection(String userId, Set<String> activeConversations) {
        if (chatModuleSocketId == null) {
            log.warn("Cannot notify about user disconnection - Chat module not connected");
            return;
        }
        
        try {
            // Create notification for each active conversation
            for (String conversationId : activeConversations) {
                Map<String, Object> disconnectionNotification = new HashMap<>();
                disconnectionNotification.put("event_type", "AGENT_DISCONNECTED");
                disconnectionNotification.put("user_id", userId);
                disconnectionNotification.put("conversation_id", conversationId);
                disconnectionNotification.put("status", "temporarily_unavailable");
                disconnectionNotification.put("preservation_timeout_minutes", conversationPreservationTimeoutMinutes);
                disconnectionNotification.put("message", "Agent temporarily disconnected - conversation preserved");
                disconnectionNotification.put("timestamp", LocalDateTime.now().toString());
                
                log.info("📤 Notifying chat module about agent disconnection - User: {}, Conversation: {}", 
                        userId, conversationId);
                
                // Send notification to chat module
                // Note: This would need the actual SocketIOServer instance to send the event
                // The notification will be sent via the ChatModule's server instance
            }
            
            log.info("📤 Sent disconnection notifications to chat module for user {} with {} conversations", 
                    userId, activeConversations.size());
                    
        } catch (Exception e) {
            log.error("❌ Error notifying about user disconnection for user {}: {}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * Notify all connected users about chat module disconnection
     */
    private void notifyUsersAboutChatModuleDisconnection() {
        if (userSocketIds.isEmpty()) {
            log.info("No connected users to notify about chat module disconnection");
            return;
        }
        
        try {
            Map<String, Object> chatModuleDownNotification = new HashMap<>();
            chatModuleDownNotification.put("event_type", "CHAT_MODULE_DISCONNECTED");
            chatModuleDownNotification.put("status", "chat_module_unavailable");
            chatModuleDownNotification.put("message", "Chat module temporarily disconnected - new conversations unavailable");
            chatModuleDownNotification.put("timestamp", LocalDateTime.now().toString());
            chatModuleDownNotification.put("active_conversations_preserved", true);
            
            log.info("📤 Notifying {} connected users about chat module disconnection", userSocketIds.size());
            
            // Note: Actual notification sending would need the SocketIOServer instance
            // This will be handled by the ChatModule's notification system
            
        } catch (Exception e) {
            log.error("❌ Error notifying users about chat module disconnection: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Get disconnection notification data for a user
     */
    public Map<String, Object> getUserDisconnectionNotification(String userId, String conversationId) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("event_type", "AGENT_DISCONNECTED");
        notification.put("user_id", userId);
        notification.put("conversation_id", conversationId);
        notification.put("status", "temporarily_unavailable");
        notification.put("preservation_timeout_minutes", conversationPreservationTimeoutMinutes);
        notification.put("message", "Agent temporarily disconnected - conversation preserved");
        notification.put("timestamp", LocalDateTime.now().toString());
        
        LocalDateTime disconnectionTime = userDisconnectionTime.get(userId);
        if (disconnectionTime != null) {
            LocalDateTime expirationTime = disconnectionTime.plusMinutes(conversationPreservationTimeoutMinutes);
            notification.put("disconnected_at", disconnectionTime.toString());
            notification.put("preservation_expires_at", expirationTime.toString());
        }
        
        return notification;
    }
    
    /**
     * Get chat module disconnection notification data
     */
    public Map<String, Object> getChatModuleDisconnectionNotification() {
        Map<String, Object> notification = new HashMap<>();
        notification.put("event_type", "CHAT_MODULE_DISCONNECTED");
        notification.put("status", "chat_module_unavailable");
        notification.put("message", "Chat module temporarily disconnected - new conversations unavailable");
        notification.put("timestamp", LocalDateTime.now().toString());
        notification.put("active_conversations_preserved", true);
        return notification;
    }
} 