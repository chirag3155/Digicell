package com.api.digicell.services;

import com.api.digicell.model.ChatUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class RedisUserService {

    private static final String USER_KEY_PREFIX = "chat:user:";
    private static final String USER_SOCKET_PREFIX = "chat:users:socket";
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
                log.debug("✅ User {} retrieved from Redis", userId);
                return user;
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
     * Generate Redis key for user socket
     */
    private String getUserSocketKey(String userId) {
        return USER_SOCKET_PREFIX + ":" + userId;
    }
} 