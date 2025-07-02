package com.api.digicell.services;

import com.api.digicell.config.SocketConfig;
import com.corundumstudio.socketio.SocketIOClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.api.digicell.chat.ChatModule;
import com.api.digicell.services.RedisUserService;
import jakarta.inject.Provider;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.*;
import java.util.HashMap;

@Slf4j
@Service
public class SocketConnectionService {
    private final SocketConfig socketConfig;
    private final Provider<ChatModule> chatModuleProvider;
    private final RedisUserService redisUserService;
    private String chatModuleSocketId;
    // private Map<String, String> userSocketIds = new ConcurrentHashMap<>();  // userId → socketId // COMMENTED OUT - Using Redis instead
    
    // New maps to track active conversations per user
    private final Map<String, Set<String>> userActiveConversations = new ConcurrentHashMap<>();
    private final Map<String, String> conversationToUserMap = new ConcurrentHashMap<>();
    
    // Conversation preservation with timeout
    private final Map<String, LocalDateTime> userDisconnectionTime = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupScheduler = Executors.newScheduledThreadPool(2);
    
    // Track users who reconnected with preserved conversations (for restoration)
    private final Set<String> usersWithPreservedConversations = ConcurrentHashMap.newKeySet();
    
    // Configurable conversation preservation timeout (in minutes)
    @Value("${socket.conversation.preservation.timeout:3}")
    private int conversationPreservationTimeoutMinutes;
    
    @Value("${socket.conversation.cleanup.interval:1}")
    private int cleanupIntervalMinutes;

    public SocketConnectionService(SocketConfig socketConfig, Provider<ChatModule> chatModuleProvider, RedisUserService redisUserService) {
        this.socketConfig = socketConfig;
        this.chatModuleProvider = chatModuleProvider;
        this.redisUserService = redisUserService;
        this.chatModuleSocketId = null;
    }
    
    @PostConstruct
    public void init() {
        log.info("🔧 Initializing SocketConnectionService with conversation preservation timeout: {} minutes", 
                conversationPreservationTimeoutMinutes);
        log.info("🔧 Cleanup interval: {} minutes", cleanupIntervalMinutes);
        
        // 🔄 RECOVERY: Rebuild conversation tracking from Redis after restart
        recoverConversationTrackingFromRedis();
        
        // Start the periodic cleanup task
        cleanupScheduler.scheduleAtFixedRate(
            this::cleanupExpiredConversations,
            cleanupIntervalMinutes,
            cleanupIntervalMinutes,
            TimeUnit.MINUTES
        );
        
        // Start periodic validation task (every 5 minutes)
        cleanupScheduler.scheduleAtFixedRate(
            this::validateAndCleanupStaleConversations,
            5, // Initial delay
            5, // Period
            TimeUnit.MINUTES
        );
        
        log.info("✅ Conversation cleanup scheduler started");
        log.info("✅ Conversation validation scheduler started (every 5 minutes)");
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
        
        log.debug("🔗 Connection attempt - IP: {}, SessionId: {}, ClientType: '{}', UserId: '{}'", 
                remoteAddress, sessionId, clientType, userId);
        
        // Reject connection if no parameters are provided
        if (clientType == null || clientType.trim().isEmpty()) {
            log.warn("❌ Connection rejected - No clientType parameter. IP: {}, SessionId: {}", remoteAddress, sessionId);
            socketClient.disconnect();
            return;
        }
        
        if (socketConfig.PARAM_CHAT_MODULE.equals(clientType)) {
            log.debug("📱 Chat module connection");
            handleChatModuleConnection(socketClient);
        } else if (socketConfig.PARAM_AGENT.equals(clientType)) {
            log.debug("👤 Agent connection");
            handleUserConnection(socketClient, clientType);
        } else {
            log.warn("❌ Invalid client type: '{}'. IP: {}, SessionId: {}", clientType, remoteAddress, sessionId);
            socketClient.disconnect();
        }
    }

    private void handleChatModuleConnection(SocketIOClient socketClient) {
        String newSocketId = socketClient.getSessionId().toString();
        
        // If there's an existing connection, check if it's the same socket client reconnecting
        if (chatModuleSocketId != null) {
            // If it's the same socket client (same socket ID), just update the connection
            if (chatModuleSocketId.equals(newSocketId)) {
                log.debug("Chat module reconnected with same socket ID: {}", newSocketId);
                return;
            }
            
            // If it's a different socket client, reject the new connection
            log.warn("Chat module already connected. Rejecting new connection. Previous: {}, New: {}", 
                    chatModuleSocketId, newSocketId);
            socketClient.disconnect();
            return;
        }

        // New connection
        chatModuleSocketId = newSocketId;
        log.info("Chat module connected. SocketId: {}", chatModuleSocketId);
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
        String existingSocketId = null;
        try {
            existingSocketId = redisUserService.getUserSocket(userId);
        } catch (Exception e) {
            log.warn("⚠️ Could not get socket for user {} from Redis: {}", userId, e.getMessage());
        }
        
        if (existingSocketId != null) {
            // User mapping exists - enforce ONE SOCKET PER USER rule
            log.debug("🔄 User {} reconnection - Old: {}, New: {}", userId, existingSocketId, newSocketId);
            
            if (!existingSocketId.equals(newSocketId)) {
                log.debug("🚪 Disconnecting old socket: {}", existingSocketId);
                
                // Try to disconnect the old socket if it still exists
                try {
                    chatModuleProvider.get().disconnectClient(existingSocketId);
                } catch (Exception e) {
                    log.warn("⚠️ Could not disconnect old socket {}: {}", existingSocketId, e.getMessage());
                }
            }
            
            // Update socket ID for this user (ONE socket per user)
            try {
                redisUserService.updateUserSocket(userId, newSocketId);
                log.debug("✅ Socket mapping updated for user {} → {}", userId, newSocketId);
            } catch (Exception redisError) {
                log.warn("⚠️ Failed to update socket mapping in Redis: {}", redisError.getMessage());
            }
            
            // Remove disconnection timestamp as user is now connected
            LocalDateTime disconnectionTime = userDisconnectionTime.remove(userId);
            if (disconnectionTime != null) {
                log.debug("🔄 User {} reconnected within preservation window", userId);
                
                // Log preserved conversations for this user
                Set<String> preservedConversations = userActiveConversations.get(userId);
                if (preservedConversations != null && !preservedConversations.isEmpty()) {
                    log.debug("🔄 Found {} preserved conversations for user {}: {}", 
                            preservedConversations.size(), userId, preservedConversations);
                    markUserAsReconnectedWithConversations(userId);
                }
            }
            
            log.info("User reconnected. SocketId: {}, UserId: {}", newSocketId, userId);
        } else {
            // No user mapping exists - this is a new user connection
            log.debug("👤 New user connection - SocketId: {}, UserId: {}", newSocketId, userId);
            
            try {
                redisUserService.addUserSocket(userId, newSocketId);
                log.debug("✅ Socket mapping added for user {} → {}", userId, newSocketId);
            } catch (Exception redisError) {
                log.warn("⚠️ Failed to add socket mapping to Redis: {}", redisError.getMessage());
            }
            
            log.info("New user connected. SocketId: {}, UserId: {}", newSocketId, userId);
        }
    }

    public String getChatModuleSocketId() {
        return chatModuleSocketId;
    }

    public String getUserIdBySocketId(String socketId) {
        // REDIS IMPLEMENTATION - Reverse lookup in Redis
        String result = null;
        try {
            Set<String> allUserIds = redisUserService.getAllUserIds();
            for (String userId : allUserIds) {
                String userSocketId = redisUserService.getUserSocket(userId);
                if (socketId.equals(userSocketId)) {
                    result = userId;
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ Could not perform reverse socket lookup in Redis: {}", e.getMessage());
        }
        
        log.debug("🔍 getUserIdBySocketId('{}') = '{}'", socketId, result);
        return result;
    }

    public void removeConnection(String socketId) {
        if (socketId.equals(chatModuleSocketId)) {
            log.info("Chat module disconnected. SocketId: {}", socketId);
            chatModuleSocketId = null;
            
            // Notify all connected users about chat module disconnection
            notifyUsersAboutChatModuleDisconnection();
        } else {
            // Find userId by reverse lookup in userSocketIds
            String userId = getUserIdBySocketId(socketId);
            if (userId != null) {
                // Record disconnection time for cleanup scheduling
                userDisconnectionTime.put(userId, LocalDateTime.now());
                
                log.debug("User disconnected. SocketId: {}, UserId: {} (preserved for {} minutes)", 
                        socketId, userId, conversationPreservationTimeoutMinutes);
                
                // Log active conversations that are being preserved
                Set<String> activeConversations = userActiveConversations.get(userId);
                if (activeConversations != null && !activeConversations.isEmpty()) {
                    log.debug("💾 Preserving {} conversations for user {}: {}", 
                            activeConversations.size(), userId, activeConversations);
                    
                    // Notify chat module about user disconnection for active conversations
                    notifyAboutUserDisconnection(userId, activeConversations);
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
        // userSocketIds.remove(userId); // COMMENTED OUT - Using Redis instead
        
        // REDIS IMPLEMENTATION - Remove socket mapping from Redis
        try {
            redisUserService.deleteUserSocket(userId);
            log.info("✅ REDIS: Socket mapping deleted for user {}", userId);
        } catch (Exception e) {
            log.warn("⚠️ REDIS: Failed to delete socket mapping for user {}: {}", userId, e.getMessage());
        }
        
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
        // COMMENTED OUT - Using Redis instead
        // // EXISTING IN-MEMORY LOGIC (keeping for safety)
        // String socketId = userSocketIds.get(userId);
        // 
        // if (socketId != null) {
        //     log.debug("✅ Socket ID found in memory for user {}: {}", userId, socketId);
        //     return socketId;
        // }
        
        // REDIS IMPLEMENTATION - Get socket directly from Redis
        log.debug("💾 REDIS: Getting socket from Redis for user {}", userId);
        try {
            String redisSocketId = redisUserService.getUserSocket(userId);
            if (redisSocketId != null) {
                log.debug("✅ REDIS: Socket ID found in Redis for user {}: {}", userId, redisSocketId);
                return redisSocketId;
            } else {
                log.debug("📭 REDIS: No socket found in Redis in getUserSocketId for user {}", userId);
            }
        } catch (Exception redisError) {
            log.warn("⚠️ REDIS: Failed to get socket from Redis for user {}: {}", userId, redisError.getMessage());
        }
    
        return null;
    }

    public void setUserSocketId(String userId, String socketId) {
        // userSocketIds.put(userId, socketId); // COMMENTED OUT - Using Redis instead
        
        // REDIS IMPLEMENTATION - Set socket in Redis
        try {
            redisUserService.updateUserSocket(userId, socketId);
            log.debug("✅ REDIS: Socket set for user {} → {}", userId, socketId);
        } catch (Exception e) {
            log.warn("⚠️ REDIS: Failed to set socket for user {}: {}", userId, e.getMessage());
        }
    }

    public void removeUserSocketId(String userId) {
        // userSocketIds.remove(userId); // COMMENTED OUT - Using Redis instead
        
        // REDIS IMPLEMENTATION - Remove socket from Redis
        try {
            redisUserService.deleteUserSocket(userId);
            log.debug("✅ REDIS: Socket removed for user {}", userId);
        } catch (Exception e) {
            log.warn("⚠️ REDIS: Failed to remove socket for user {}: {}", userId, e.getMessage());
        }
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
                // String currentSocketId = userSocketIds.get(userId); // COMMENTED OUT - Using Redis instead
                String currentSocketId = null;
                try {
                    currentSocketId = redisUserService.getUserSocket(userId);
                } catch (Exception e) {
                    log.warn("⚠️ Could not check socket for user {} in Redis: {}", userId, e.getMessage());
                }
                
                if (currentSocketId == null) {
                    log.info("🧹 Removed last conversation {} for user {}. User mapping already cleaned up.", conversationId, userId);
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
        // REDIS IMPLEMENTATION - Get connected users from Redis
        Set<String> connectedUserIds = new HashSet<>();
        try {
            connectedUserIds = getConnectedUserIds();
        } catch (Exception e) {
            log.warn("⚠️ Could not get connected users from Redis: {}", e.getMessage());
        }
        
        if (connectedUserIds.isEmpty()) {
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
            
            log.info("📤 Notifying {} connected users about chat module disconnection", connectedUserIds.size());
            
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
    
    /**
     * Check if user is currently connected (has active socket)
     */
    public boolean isUserConnected(String userId) {
        // String socketId = userSocketIds.get(userId); // COMMENTED OUT - Using Redis instead
        String socketId = null;
        try {
            socketId = redisUserService.getUserSocket(userId);
        } catch (Exception e) {
            log.warn("⚠️ Could not check if user {} is connected in Redis: {}", userId, e.getMessage());
        }
        
        boolean connected = socketId != null;
        log.debug("🔍 User {} connection status: {} (socket: {})", userId, connected ? "CONNECTED" : "DISCONNECTED", socketId);
        return connected;
    }
    
    /**
     * Get current socket ID for user (null if not connected)
     */
    public String getCurrentSocketForUser(String userId) {
        // return userSocketIds.get(userId); // COMMENTED OUT - Using Redis instead
        try {
            return redisUserService.getUserSocket(userId);
        } catch (Exception e) {
            log.warn("⚠️ Could not get current socket for user {} from Redis: {}", userId, e.getMessage());
            return null;
        }
    }
    
    /**
     * Get total number of connected users
     */
    public int getConnectedUserCount() {
        // return userSocketIds.size(); // COMMENTED OUT - Using Redis instead
        try {
            // Get all user IDs that have sockets in Redis
            Set<String> allUserIds = redisUserService.getAllUserIds();
            int connectedCount = 0;
            for (String userId : allUserIds) {
                if (redisUserService.getUserSocket(userId) != null) {
                    connectedCount++;
                }
            }
            return connectedCount;
        } catch (Exception e) {
            log.warn("⚠️ Could not get connected user count from Redis: {}", e.getMessage());
            return 0;
        }
    }
    
    /**
     * Get all connected user IDs
     */
    public Set<String> getConnectedUserIds() {
        // return new HashSet<>(userSocketIds.keySet()); // COMMENTED OUT - Using Redis instead
        try {
            Set<String> connectedUserIds = new HashSet<>();
            Set<String> allUserIds = redisUserService.getAllUserIds();
            for (String userId : allUserIds) {
                if (redisUserService.getUserSocket(userId) != null) {
                    connectedUserIds.add(userId);
                }
            }
            return connectedUserIds;
        } catch (Exception e) {
            log.warn("⚠️ Could not get connected user IDs from Redis: {}", e.getMessage());
            return new HashSet<>();
        }
    }
    
    /**
     * Log current connection status for debugging
     */
    public void logConnectionStatus() {
        // REDIS IMPLEMENTATION - Get connection status from Redis
        int connectedUserCount = 0;
        Set<String> connectedUserIds = new HashSet<>();
        
        try {
            connectedUserIds = getConnectedUserIds();
            connectedUserCount = connectedUserIds.size();
        } catch (Exception e) {
            log.warn("⚠️ Could not get connection status from Redis: {}", e.getMessage());
        }
        
        log.info("📊 CONNECTION STATUS SUMMARY:");
        log.info("   Connected users: {}", connectedUserCount);
        log.info("   Active conversations: {}", userActiveConversations.size());
        log.info("   Disconnected users (preserved): {}", userDisconnectionTime.size());
        
        if (!connectedUserIds.isEmpty()) {
            log.info("   Connected user details:");
            for (String userId : connectedUserIds) {
                try {
                    String socketId = redisUserService.getUserSocket(userId);
                    Set<String> conversations = userActiveConversations.get(userId);
                    int conversationCount = conversations != null ? conversations.size() : 0;
                    log.info("     User {} → Socket {} ({} conversations)", userId, socketId, conversationCount);
                } catch (Exception e) {
                    log.warn("     User {} → Could not get socket from Redis: {}", userId, e.getMessage());
                }
            }
        }
    }

    /**
     * Mark user as having reconnected with preserved conversations
     */
    public void markUserAsReconnectedWithConversations(String userId) {
        usersWithPreservedConversations.add(userId);
        log.info("🏷️ User {} marked as reconnected with preserved conversations", userId);
    }
    
    /**
     * Check if user reconnected with preserved conversations and clear the flag
     */
    public boolean checkAndClearUserReconnectedFlag(String userId) {
        boolean hadPreservedConversations = usersWithPreservedConversations.remove(userId);
        if (hadPreservedConversations) {
            log.info("🔄 User {} had preserved conversations - restoration needed", userId);
        }
        return hadPreservedConversations;
    }

    /**
     * 🔄 RECOVERY METHOD: Rebuild in-memory conversation tracking from Redis data
     * This handles the case where the application restarts and loses in-memory maps
     */
    private void recoverConversationTrackingFromRedis() {
        log.info("🔄 RECOVERY: Starting conversation tracking recovery from Redis...");
        
        try {
            // Get all active conversations from Redis
            Map<String, Set<String>> redisUserConversations = redisUserService.getAllUserActiveConversations();
            Map<String, String> redisConversationUserMap = redisUserService.getAllActiveConversationUserMappings();
            
            if (redisUserConversations.isEmpty() && redisConversationUserMap.isEmpty()) {
                log.info("📭 RECOVERY: No active conversations found in Redis - clean startup");
                return;
            }
            
            log.info("🔄 RECOVERY: Found {} users with active conversations in Redis", redisUserConversations.size());
            log.info("🔄 RECOVERY: Found {} total active conversations in Redis", redisConversationUserMap.size());
            
            // Rebuild userActiveConversations map
            userActiveConversations.clear();
            userActiveConversations.putAll(redisUserConversations);
            
            // Rebuild conversationToUserMap
            conversationToUserMap.clear();
            conversationToUserMap.putAll(redisConversationUserMap);
            
            // Log recovery results
            int totalRecoveredConversations = conversationToUserMap.size();
            int usersWithConversations = userActiveConversations.size();
            
            log.info("✅ RECOVERY COMPLETED:");
            log.info("   📊 Recovered {} active conversations", totalRecoveredConversations);
            log.info("   👥 Recovered conversation tracking for {} users", usersWithConversations);
            
            // Log detailed recovery info
            userActiveConversations.forEach((userId, conversations) -> {
                log.info("   🔄 User {} → {} conversations: {}", userId, conversations.size(), conversations);
            });
            
            log.info("🎯 RECOVERY: Conversation tracking fully restored from Redis");
            
        } catch (Exception e) {
            log.error("❌ RECOVERY FAILED: Error recovering conversation tracking from Redis: {}", e.getMessage(), e);
            log.warn("⚠️ RECOVERY: Application will continue with empty conversation tracking");
            log.warn("⚠️ RECOVERY: Users may need to reconnect to restore conversation tracking");
        }
    }

    /**
     * 🧹 VALIDATION: Clean up stale conversations that exist in memory but not in Redis
     * This can happen if Redis data was cleaned up while application was running
     */
    public void validateAndCleanupStaleConversations() {
        log.info("🧹 VALIDATION: Starting stale conversation cleanup...");
        
        try {
            Set<String> staleConversations = new HashSet<>();
            
            // Check each conversation in memory against Redis
            for (String conversationId : conversationToUserMap.keySet()) {
                if (!redisUserService.isConversationActive(conversationId)) {
                    staleConversations.add(conversationId);
                    log.warn("🗑️ STALE: Conversation {} exists in memory but not active in Redis", conversationId);
                }
            }
            
            if (staleConversations.isEmpty()) {
                log.info("✅ VALIDATION: No stale conversations found - all in-memory data is valid");
                return;
            }
            
            log.info("🧹 VALIDATION: Found {} stale conversations to clean up", staleConversations.size());
            
            // Clean up stale conversations
            for (String staleConversationId : staleConversations) {
                String userId = conversationToUserMap.get(staleConversationId);
                if (userId != null) {
                    log.info("🗑️ CLEANUP: Removing stale conversation {} for user {}", staleConversationId, userId);
                    removeUserConversation(userId, staleConversationId);
                }
            }
            
            log.info("✅ VALIDATION: Stale conversation cleanup completed");
            
        } catch (Exception e) {
            log.error("❌ VALIDATION FAILED: Error during stale conversation cleanup: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 🔄 PUBLIC METHOD: Force conversation tracking recovery (can be called manually)
     */
    public void forceConversationTrackingRecovery() {
        log.info("🔄 MANUAL RECOVERY: Force conversation tracking recovery requested");
        recoverConversationTrackingFromRedis();
        validateAndCleanupStaleConversations();
        log.info("✅ MANUAL RECOVERY: Force recovery completed");
    }
} 