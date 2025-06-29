package com.api.digicell.services;

import com.api.digicell.model.ChatUser;
import com.api.digicell.model.ChatRoom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;

@Service
@Slf4j
public class RedisUserService {

    private static final String USER_KEY_PREFIX = "chat:user:";
    private static final String USER_SOCKET_PREFIX = "chat:users:socket";
    private static final String ROOM_KEY_PREFIX = "chat:room:";
    private static final String TENANT_POOL_PREFIX = "chat:tenant:pool:";
    private static final long DEFAULT_TTL_HOURS = 24; // 24 hours TTL for user data

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Add a new ChatUser to Redis
     */
    
    public void addUser(ChatUser user) {
        if (user == null || user.getUserId() == null) {
            log.warn("❌ Cannot add null user or user with null ID");
            return;
        }

        try {
            String key = getUserKey(user.getUserId());
            redisTemplate.opsForValue().set(key, user, DEFAULT_TTL_HOURS, TimeUnit.HOURS);
            log.debug("✅ User {} added to Redis with TTL {} hours", user.getUserId(), DEFAULT_TTL_HOURS);
        } catch (Exception e) {
            log.error("❌ Error adding user {} to Redis: {}", user.getUserId(), e.getMessage(), e);
        }
    }

    /**
     * Update an existing ChatUser in Redis
     */
    public void updateUser(ChatUser user) {
        if (user == null || user.getUserId() == null) {
            log.warn("❌ Cannot update null user or user with null ID");
            return;
        }

        try {
            String key = getUserKey(user.getUserId());
            redisTemplate.opsForValue().set(key, user, DEFAULT_TTL_HOURS, TimeUnit.HOURS);
            log.debug("✅ User {} updated in Redis", user.getUserId());
        } catch (Exception e) {
            log.error("❌ Error updating user {} in Redis: {}", user.getUserId(), e.getMessage(), e);
        }
    }

    /**
     * Get a ChatUser from Redis by userId
     */
    public ChatUser getUser(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("❌ Cannot get user with null or empty userId");
            return null;
        }

        try {
            String key = getUserKey(userId);
            Object userObj = redisTemplate.opsForValue().get(key);
            
            if (userObj instanceof ChatUser) {
                ChatUser user = (ChatUser) userObj;
                
                // ✅ FIX: Automatically validate and fix count issues on retrieval
                user.validateAndFixCount();
                
                // If user data was corrected, update it in Redis
                if (user.getCurrentClientCount() != ((ChatUser) userObj).getCurrentClientCount()) {
                    log.info("🔄 AUTO-CORRECTED: User {} data was corrected during retrieval, updating Redis", userId);
                    updateUser(user);
                }
                
                log.debug("✅ User {} retrieved from Redis", userId);
                return user;
            } else if (userObj instanceof java.util.LinkedHashMap) {
                // ✅ FIX: Handle existing LinkedHashMap data (deserialization issue)
                log.warn("🔄 LEGACY DATA DETECTED: User {} stored as LinkedHashMap, converting to ChatUser", userId);
                ChatUser user = convertLinkedHashMapToChatUser((java.util.LinkedHashMap<String, Object>) userObj);
                
                if (user != null) {
                    // Validate and fix the converted user
                    user.validateAndFixCount();
                    
                    // Store the corrected ChatUser object back to Redis
                    updateUser(user);
                    log.info("✅ LEGACY DATA CONVERTED: User {} converted from LinkedHashMap to ChatUser and updated in Redis", userId);
                    return user;
                } else {
                    log.error("❌ CONVERSION FAILED: Could not convert LinkedHashMap to ChatUser for user {}", userId);
                    return null;
                }
            } else {
                log.debug("📭 User {} not found in Redis", userId);
                return null;
            }
        } catch (Exception e) {
            log.error("❌ Error retrieving user {} from Redis: {}", userId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * ✅ HELPER: Convert LinkedHashMap to ChatUser (for legacy data)
     */
    private ChatUser convertLinkedHashMapToChatUser(java.util.LinkedHashMap<String, Object> map) {
        try {
            ChatUser user = new ChatUser();
            
            // Map basic fields
            user.setUserId((String) map.get("userId"));
            user.setEmail((String) map.get("email"));
            user.setIpAddress((String) map.get("ipAddress"));
            user.setUserName((String) map.get("userName"));
            user.setUserLabel((String) map.get("userLabel"));
            
            // Handle numeric fields safely
            Object clientCountObj = map.get("currentClientCount");
            if (clientCountObj instanceof Number) {
                user.setCurrentClientCount(((Number) clientCountObj).intValue());
            } else {
                user.setCurrentClientCount(0);
            }
            
            Object offlineRequestedObj = map.get("offlineRequested");
            if (offlineRequestedObj instanceof Boolean) {
                user.setOfflineRequested((Boolean) offlineRequestedObj);
            } else {
                user.setOfflineRequested(false);
            }
            
            Object lastPingTimeObj = map.get("lastPingTime");
            if (lastPingTimeObj instanceof Number) {
                user.setLastPingTime(((Number) lastPingTimeObj).longValue());
            } else {
                user.setLastPingTime(System.currentTimeMillis());
            }
            
            // Handle activeConversations set
            Object activeConversationsObj = map.get("activeConversations");
            
            java.util.Set<String> activeConversations = new java.util.HashSet<>();
            
            if (activeConversationsObj instanceof java.util.List) {
                activeConversations = new java.util.HashSet<>((java.util.List<String>) activeConversationsObj);
            }
            
            user.setActiveConversations(activeConversations);
            
            log.info("✅ Successfully converted LinkedHashMap to ChatUser for user {}", user.getUserId());
            return user;
            
        } catch (Exception e) {
            log.error("❌ Error converting LinkedHashMap to ChatUser: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Delete a ChatUser from Redis
     */
    public boolean deleteUser(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("❌ Cannot delete user with null or empty userId");
            return false;
        }

        try {
            String key = getUserKey(userId);
            Boolean deleted = redisTemplate.delete(key);
            
            if (Boolean.TRUE.equals(deleted)) {
                log.debug("✅ User {} deleted from Redis", userId);
                return true;
            } else {
                log.debug("📭 User {} was not found in Redis for deletion", userId);
                return false;
            }
        } catch (Exception e) {
            log.error("❌ Error deleting user {} from Redis: {}", userId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Generate Redis key for user
     */
    private String getUserKey(String userId) {
        return USER_KEY_PREFIX + userId;
    }

    /**
     * Check if user key exists in Redis (for debugging)
     */
    public boolean userKeyExists(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return false;
        }
        
        try {
            String key = getUserKey(userId);
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("❌ Error checking if user key exists for {} in Redis: {}", userId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get raw user data from Redis (for debugging)
     */
    public Object getRawUserData(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return null;
        }
        
        try {
            String key = getUserKey(userId);
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("❌ Error getting raw user data for {} from Redis: {}", userId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Add socket ID for a user
     */
    public void addUserSocket(String userId, String socketId) {
        if (userId == null || userId.trim().isEmpty() || socketId == null || socketId.trim().isEmpty()) {
            log.warn("❌ Cannot add socket with null or empty userId or socketId");
            return;
        }

        try {
            String key = getUserSocketKey(userId);
            redisTemplate.opsForValue().set(key, socketId, DEFAULT_TTL_HOURS, TimeUnit.HOURS);
            log.debug("✅ Socket {} added for user {} with TTL {} hours", socketId, userId, DEFAULT_TTL_HOURS);
        } catch (Exception e) {
            log.error("❌ Error adding socket {} for user {} to Redis: {}", socketId, userId, e.getMessage(), e);
        }
    }

    /**
     * Update socket ID for a user
     */
    public void updateUserSocket(String userId, String socketId) {
        if (userId == null || userId.trim().isEmpty() || socketId == null || socketId.trim().isEmpty()) {
            log.warn("❌ Cannot update socket with null or empty userId or socketId");
            return;
        }

        try {
            String key = getUserSocketKey(userId);
            redisTemplate.opsForValue().set(key, socketId, DEFAULT_TTL_HOURS, TimeUnit.HOURS);
            log.debug("✅ Socket updated to {} for user {}", socketId, userId);
        } catch (Exception e) {
            log.error("❌ Error updating socket {} for user {} in Redis: {}", socketId, userId, e.getMessage(), e);
        }
    }

    /**
     * Get socket ID for a user
     */
    public String getUserSocket(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("❌ Cannot get socket with null or empty userId");
            return null;
        }

        try {
            String key = getUserSocketKey(userId);
            Object socketObj = redisTemplate.opsForValue().get(key);
            
            if (socketObj != null) {
                String socketId = socketObj.toString();
                log.debug("✅ Socket {} retrieved for user {}", socketId, userId);
                return socketId;
            } else {
                log.debug("📭 No socket found for user {}", userId);
                return null;
            }
        } catch (Exception e) {
            log.error("❌ Error retrieving socket for user {} from Redis: {}", userId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Delete socket ID for a user
     */
    public boolean deleteUserSocket(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("❌ Cannot delete socket with null or empty userId");
            return false;
        }

        try {
            String key = getUserSocketKey(userId);
            Boolean deleted = redisTemplate.delete(key);
            
            if (Boolean.TRUE.equals(deleted)) {
                log.debug("✅ Socket deleted for user {}", userId);
                return true;
            } else {
                log.debug("📭 No socket found for user {} to delete", userId);
                return false;
            }
        } catch (Exception e) {
            log.error("❌ Error deleting socket for user {} from Redis: {}", userId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get all user IDs currently stored in Redis
     */
    public Set<String> getAllUserIds() {
        try {
            Set<String> keys = redisTemplate.keys(USER_KEY_PREFIX + "*");
            if (keys != null) {
                return keys.stream()
                        .map(key -> key.substring(USER_KEY_PREFIX.length()))
                        .collect(Collectors.toSet());
            }
            return Set.of();
        } catch (Exception e) {
            log.error("❌ Error getting all user IDs from Redis: {}", e.getMessage(), e);
            return Set.of();
        }
    }

    /**
     * Generate Redis key for user socket
     */
    private String getUserSocketKey(String userId) {
        return USER_SOCKET_PREFIX + ":" + userId;
    }

    // ===== CHAT ROOM OPERATIONS =====

    /**
     * Add a ChatRoom to Redis
     */
    public void addChatRoom(ChatRoom chatRoom) {
        if (chatRoom == null || chatRoom.getConversationId() == null) {
            log.warn("❌ Cannot add null chat room or room with null conversation ID");
            return;
        }

        try {
            String key = getRoomKey(chatRoom.getConversationId());
            redisTemplate.opsForValue().set(key, chatRoom, DEFAULT_TTL_HOURS, TimeUnit.HOURS);
            log.debug("✅ Chat room {} added to Redis with TTL {} hours", chatRoom.getConversationId(), DEFAULT_TTL_HOURS);
        } catch (Exception e) {
            log.error("❌ Error adding chat room {} to Redis: {}", chatRoom.getConversationId(), e.getMessage(), e);
        }
    }

    /**
     * Update a ChatRoom in Redis
     */
    public void updateChatRoom(ChatRoom chatRoom) {
        if (chatRoom == null || chatRoom.getConversationId() == null) {
            log.warn("❌ Cannot update null chat room or room with null conversation ID");
            return;
        }

        try {
            String key = getRoomKey(chatRoom.getConversationId());
            redisTemplate.opsForValue().set(key, chatRoom, DEFAULT_TTL_HOURS, TimeUnit.HOURS);
            log.debug("✅ Chat room {} updated in Redis", chatRoom.getConversationId());
        } catch (Exception e) {
            log.error("❌ Error updating chat room {} in Redis: {}", chatRoom.getConversationId(), e.getMessage(), e);
        }
    }

    /**
     * Get a ChatRoom from Redis by conversation ID
     */
    public ChatRoom getChatRoom(String conversationId) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            log.warn("❌ Cannot get chat room with null or empty conversation ID");
            return null;
        }

        try {
            String key = getRoomKey(conversationId);
            Object roomObj = redisTemplate.opsForValue().get(key);
            
            if (roomObj instanceof ChatRoom) {
                ChatRoom chatRoom = (ChatRoom) roomObj;
                log.debug("✅ Chat room {} retrieved from Redis", conversationId);
                return chatRoom;
            } else {
                log.debug("📭 Chat room {} not found in Redis", conversationId);
                return null;
            }
        } catch (Exception e) {
            log.error("❌ Error retrieving chat room {} from Redis: {}", conversationId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Delete a ChatRoom from Redis
     */
    public boolean deleteChatRoom(String conversationId) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            log.warn("❌ Cannot delete chat room with null or empty conversation ID");
            return false;
        }

        try {
            String key = getRoomKey(conversationId);
            Boolean deleted = redisTemplate.delete(key);
            
            if (Boolean.TRUE.equals(deleted)) {
                log.debug("✅ Chat room {} deleted from Redis", conversationId);
                return true;
            } else {
                log.debug("📭 Chat room {} was not found in Redis for deletion", conversationId);
                return false;
            }
        } catch (Exception e) {
            log.error("❌ Error deleting chat room {} from Redis: {}", conversationId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get all chat room IDs currently stored in Redis
     */
    public Set<String> getAllChatRoomIds() {
        try {
            Set<String> keys = redisTemplate.keys(ROOM_KEY_PREFIX + "*");
            if (keys != null) {
                return keys.stream()
                        .map(key -> key.substring(ROOM_KEY_PREFIX.length()))
                        .collect(Collectors.toSet());
            }
            return Set.of();
        } catch (Exception e) {
            log.error("❌ Error getting all chat room IDs from Redis: {}", e.getMessage(), e);
            return Set.of();
        }
    }

    /**
     * Get all ChatRooms currently stored in Redis
     */
    public Set<ChatRoom> getAllChatRooms() {
        try {
            Set<String> roomIds = getAllChatRoomIds();
            Set<ChatRoom> chatRooms = new HashSet<>();
            
            for (String roomId : roomIds) {
                ChatRoom room = getChatRoom(roomId);
                if (room != null) {
                    chatRooms.add(room);
                }
            }
            
            return chatRooms;
        } catch (Exception e) {
            log.error("❌ Error getting all chat rooms from Redis: {}", e.getMessage(), e);
            return Set.of();
        }
    }

    /**
     * Generate Redis key for chat room
     */
    private String getRoomKey(String conversationId) {
        return ROOM_KEY_PREFIX + conversationId;
    }

    // ===== TENANT USER POOL OPERATIONS =====

    /**
     * Add a user to a tenant pool
     */
    public void addUserToTenantPool(String tenantId, String userId) {
        if (tenantId == null || tenantId.trim().isEmpty() || userId == null || userId.trim().isEmpty()) {
            log.warn("❌ Cannot add user to tenant pool with null or empty tenant ID or user ID");
            return;
        }

        try {
            String key = getTenantPoolKey(tenantId);
            redisTemplate.opsForSet().add(key, userId);
            redisTemplate.expire(key, DEFAULT_TTL_HOURS, TimeUnit.HOURS);
            log.debug("✅ User {} added to tenant pool {}", userId, tenantId);
        } catch (Exception e) {
            log.error("❌ Error adding user {} to tenant pool {} in Redis: {}", userId, tenantId, e.getMessage(), e);
        }
    }

    /**
     * Remove a user from a tenant pool
     */
    public boolean removeUserFromTenantPool(String tenantId, String userId) {
        if (tenantId == null || tenantId.trim().isEmpty() || userId == null || userId.trim().isEmpty()) {
            log.warn("❌ Cannot remove user from tenant pool with null or empty tenant ID or user ID");
            return false;
        }

        try {
            String key = getTenantPoolKey(tenantId);
            Long removed = redisTemplate.opsForSet().remove(key, userId);
            
            if (removed != null && removed > 0) {
                log.debug("✅ User {} removed from tenant pool {}", userId, tenantId);
                return true;
            } else {
                log.debug("📭 User {} was not found in tenant pool {} for removal", userId, tenantId);
                return false;
            }
        } catch (Exception e) {
            log.error("❌ Error removing user {} from tenant pool {} in Redis: {}", userId, tenantId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Remove a user from all tenant pools
     */
    public void removeUserFromAllTenantPools(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("❌ Cannot remove user from all tenant pools with null or empty user ID");
            return;
        }

        try {
            Set<String> tenantIds = getAllTenantIds();
            for (String tenantId : tenantIds) {
                removeUserFromTenantPool(tenantId, userId);
            }
            log.debug("✅ User {} removed from all tenant pools", userId);
        } catch (Exception e) {
            log.error("❌ Error removing user {} from all tenant pools in Redis: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Get all users in a tenant pool
     */
    public Set<String> getTenantPoolUsers(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            log.warn("❌ Cannot get tenant pool users with null or empty tenant ID");
            return Set.of();
        }

        try {
            String key = getTenantPoolKey(tenantId);
            Set<Object> members = redisTemplate.opsForSet().members(key);
            
            if (members != null) {
                return members.stream()
                        .map(Object::toString)
                        .collect(Collectors.toSet());
            }
            return Set.of();
        } catch (Exception e) {
            log.error("❌ Error getting users for tenant pool {} from Redis: {}", tenantId, e.getMessage(), e);
            return Set.of();
        }
    }

    /**
     * Get all tenant IDs that have user pools
     */
    public Set<String> getAllTenantIds() {
        try {
            Set<String> keys = redisTemplate.keys(TENANT_POOL_PREFIX + "*");
            if (keys != null) {
                return keys.stream()
                        .map(key -> key.substring(TENANT_POOL_PREFIX.length()))
                        .collect(Collectors.toSet());
            }
            return Set.of();
        } catch (Exception e) {
            log.error("❌ Error getting all tenant IDs from Redis: {}", e.getMessage(), e);
            return Set.of();
        }
    }

    /**
     * Check if tenant pool is empty
     */
    public boolean isTenantPoolEmpty(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            return true;
        }

        try {
            String key = getTenantPoolKey(tenantId);
            Long size = redisTemplate.opsForSet().size(key);
            return size == null || size == 0;
        } catch (Exception e) {
            log.error("❌ Error checking if tenant pool {} is empty in Redis: {}", tenantId, e.getMessage(), e);
            return true;
        }
    }

    /**
     * Get size of tenant pool
     */
    public long getTenantPoolSize(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            return 0;
        }

        try {
            String key = getTenantPoolKey(tenantId);
            Long size = redisTemplate.opsForSet().size(key);
            return size != null ? size : 0;
        } catch (Exception e) {
            log.error("❌ Error getting tenant pool {} size from Redis: {}", tenantId, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Generate Redis key for tenant pool
     */
    private String getTenantPoolKey(String tenantId) {
        return TENANT_POOL_PREFIX + tenantId;
    }

    // ===== USER CLIENT COUNT OPERATIONS (using user data) =====

    /**
     * Get current client count for a user from user data
     */
    public int getUserClientCount(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("❌ Cannot get client count with null or empty user ID");
            return 0;
        }

        try {
            ChatUser user = getUser(userId);
            if (user != null) {
                int count = user.getCurrentClientCount();
                if (count < 0) {
                    log.warn("⚠️ NEGATIVE COUNT DETECTED: User {} has negative client count: {}, correcting to 0", userId, count);
                    user.setCurrentClientCount(0);
                    updateUser(user);
                    return 0;
                }
                return count;
            } else {
                log.debug("📭 User {} not found in Redis for client count", userId);
                return 0;
            }
        } catch (Exception e) {
            log.error("❌ Error getting client count for user {} from Redis: {}", userId, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Increment client count for a user with validation
     */
    public void incrementUserClientCount(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("❌ Cannot increment client count with null or empty user ID");
            return;
        }

        try {
            ChatUser user = getUser(userId);
            if (user != null) {
                int currentCount = user.getCurrentClientCount();
                
                if (currentCount < 0) {
                    log.warn("⚠️ NEGATIVE COUNT CORRECTION: User {} had negative count {}, resetting to 0 before increment", userId, currentCount);
                    currentCount = 0;
                }
                
                user.setCurrentClientCount(currentCount + 1);
                updateUser(user);
                
                log.debug("✅ Client count incremented for user {} from {} to {}", userId, currentCount, currentCount + 1);
            } else {
                log.warn("⚠️ User {} not found in Redis for client count increment", userId);
            }
        } catch (Exception e) {
            log.error("❌ Error incrementing client count for user {} in Redis: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Decrement client count for a user with validation
     */
    public void decrementUserClientCount(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("❌ Cannot decrement client count with null or empty user ID");
            return;
        }

        try {
            ChatUser user = getUser(userId);
            if (user != null) {
                int currentCount = user.getCurrentClientCount();
                
                if (currentCount <= 0) {
                    log.warn("⚠️ INVALID DECREMENT: User {} already has count {} (≤0), cannot decrement further", userId, currentCount);
                    if (currentCount < 0) {
                        user.setCurrentClientCount(0);
                        updateUser(user);
                        log.warn("⚠️ CORRECTED: User {} count was negative ({}), reset to 0", userId, currentCount);
                    }
                    return;
                }
                
                int newCount = currentCount - 1;
                user.setCurrentClientCount(newCount);
                updateUser(user);
                
                log.debug("✅ Client count decremented for user {} from {} to {}", userId, currentCount, newCount);
            } else {
                log.warn("⚠️ User {} not found in Redis for client count decrement", userId);
            }
        } catch (Exception e) {
            log.error("❌ Error decrementing client count for user {} in Redis: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Reset client count for a user (useful for cleanup/recovery)
     */
    public void resetUserClientCount(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("❌ Cannot reset client count with null or empty user ID");
            return;
        }

        try {
            ChatUser user = getUser(userId);
            if (user != null) {
                int oldCount = user.getCurrentClientCount();
                user.setCurrentClientCount(0);
                user.getActiveConversations().clear();
                updateUser(user);
                
                log.info("🔄 RESET: User {} client count reset from {} to 0", userId, oldCount);
            } else {
                log.debug("📭 User {} not found in Redis for client count reset", userId);
            }
        } catch (Exception e) {
            log.error("❌ Error resetting client count for user {} in Redis: {}", userId, e.getMessage(), e);
        }
    }

    // ===== CONVERSATION RECOVERY OPERATIONS =====

    /**
     * Get all active chat rooms with their user mappings (for recovery after restart)
     */
    public Map<String, String> getAllActiveConversationUserMappings() {
        try {
            Map<String, String> conversationUserMap = new HashMap<>();
            Set<String> roomIds = getAllChatRoomIds();
            
            for (String conversationId : roomIds) {
                ChatRoom room = getChatRoom(conversationId);
                if (room != null && room.isActive()) {
                    conversationUserMap.put(conversationId, room.getUserId());
                }
            }
            
            log.info("✅ Retrieved {} active conversation-user mappings from Redis", conversationUserMap.size());
            return conversationUserMap;
        } catch (Exception e) {
            log.error("❌ Error getting active conversation-user mappings from Redis: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    /**
     * Get all active conversations grouped by user (for recovery after restart)
     */
    public Map<String, Set<String>> getAllUserActiveConversations() {
        try {
            Map<String, Set<String>> userConversationsMap = new HashMap<>();
            Set<String> roomIds = getAllChatRoomIds();
            
            for (String conversationId : roomIds) {
                ChatRoom room = getChatRoom(conversationId);
                if (room != null && room.isActive()) {
                    String userId = room.getUserId();
                    userConversationsMap.computeIfAbsent(userId, k -> new HashSet<>()).add(conversationId);
                }
            }
            
            log.info("✅ Retrieved active conversations for {} users from Redis", userConversationsMap.size());
            return userConversationsMap;
        } catch (Exception e) {
            log.error("❌ Error getting user active conversations from Redis: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    /**
     * Check if a conversation is still active in Redis
     */
    public boolean isConversationActive(String conversationId) {
        try {
            ChatRoom room = getChatRoom(conversationId);
            return room != null && room.isActive();
        } catch (Exception e) {
            log.error("❌ Error checking if conversation {} is active: {}", conversationId, e.getMessage(), e);
            return false;
        }
    }
} 