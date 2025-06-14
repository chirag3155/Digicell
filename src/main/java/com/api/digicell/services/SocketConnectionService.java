package com.api.digicell.services;

import com.api.digicell.config.SocketConfig;
import com.corundumstudio.socketio.SocketIOClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SocketConnectionService {
    private final SocketConfig socketConfig;
    private String chatModuleSocketId;
    private final Map<String, String> userSocketMap;
    private Map<String, String> userSocketIds = new ConcurrentHashMap<>();

    public SocketConnectionService(SocketConfig socketConfig) {
        this.socketConfig = socketConfig;
        this.userSocketMap = new ConcurrentHashMap<>();
        this.chatModuleSocketId = null;
    }

    public void handleConnection(SocketIOClient socketClient) {
        String clientType = socketClient.getHandshakeData().getSingleUrlParam(socketConfig.PARAM_CLIENT_TYPE);
        
        // Reject connection if no parameters are provided
        if (clientType == null || clientType.trim().isEmpty()) {
            log.warn("Connection rejected: No parameters provided. SocketId: {}", socketClient.getSessionId());
            socketClient.disconnect();
            return;
        }

        if (socketConfig.PARAM_CHAT_MODULE.equals(clientType)) {
            handleChatModuleConnection(socketClient);
        } else if (socketConfig.PARAM_AGENT.equals(clientType)) {
            handleUserConnection(socketClient, clientType);
        } else {
            log.warn("Connection rejected: Invalid clientType: {}. SocketId: {}", clientType, socketClient.getSessionId());
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

        String socketId = socketClient.getSessionId().toString();
        userSocketMap.put(socketId, userId);
        userSocketIds.put(userId, socketId);
        log.info("User connected successfully. SocketId: {}, UserId: {}, Type: {}", 
                socketId, userId, clientType);
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
        } else {
            String userId = userSocketMap.remove(socketId);
            if (userId != null) {
                userSocketIds.remove(userId);
                log.info("User disconnected. SocketId: {}, UserId: {}", socketId, userId);
            }
        }
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
} 