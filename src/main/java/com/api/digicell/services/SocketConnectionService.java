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
    private final Map<String, String> agentSocketMap;

    public SocketConnectionService(SocketConfig socketConfig) {
        this.socketConfig = socketConfig;
        this.agentSocketMap = new ConcurrentHashMap<>();
        this.chatModuleSocketId = null;
    }

    public void handleConnection(SocketIOClient client) {
        String clientType = client.getHandshakeData().getSingleUrlParam(socketConfig.PARAM_CLIENT_TYPE);
        
        // Reject connection if no parameters are provided
        if (clientType == null || clientType.trim().isEmpty()) {
            log.warn("Connection rejected: No parameters provided. SocketId: {}", client.getSessionId());
            client.disconnect();
            return;
        }

        if (socketConfig.PARAM_CHAT_MODULE.equals(clientType)) {
            handleChatModuleConnection(client);
        } else if (socketConfig.PARAM_AGENT.equals(clientType)) {
            handleAgentConnection(client, clientType);
        } else {
            log.warn("Connection rejected: Invalid clientType: {}. SocketId: {}", clientType, client.getSessionId());
            client.disconnect();
        }
    }

    private void handleChatModuleConnection(SocketIOClient client) {
        String newSocketId = client.getSessionId().toString();
        log.info("Chat module connection attempt. Previous socketId: {}, New socketId: {}", chatModuleSocketId, newSocketId);
        
        // If there's an existing connection, check if it's the same client reconnecting
        if (chatModuleSocketId != null) {
            // If it's the same client (same socket ID), just update the connection
            if (chatModuleSocketId.equals(newSocketId)) {
                log.info("Chat module reconnected with same socket ID: {}", newSocketId);
                return;
            }
            
            // If it's a different client, reject the new connection
            log.warn("Chat module already connected with different socket ID. Rejecting new connection. Previous: {}, New: {}", 
                    chatModuleSocketId, newSocketId);
            client.disconnect();
            return;
        }

        // New connection
        chatModuleSocketId = newSocketId;
        log.info("Chat module connected successfully. SocketId: {}", chatModuleSocketId);
    }

    private void handleAgentConnection(SocketIOClient client, String clientType) {
        String agentId = client.getHandshakeData().getSingleUrlParam("agentId");
        if (agentId == null || agentId.trim().isEmpty()) {
            log.warn("Agent connection rejected: Missing agentId. SocketId: {}", client.getSessionId());
            client.disconnect();
            return;
        }

        agentSocketMap.put(client.getSessionId().toString(), agentId);
        log.info("Agent connected successfully. SocketId: {}, AgentId: {}, Type: {}", 
                client.getSessionId(), agentId, clientType);
    }

    public String getChatModuleSocketId() {
        return chatModuleSocketId;
    }

    public String getAgentIdBySocketId(String socketId) {
        return agentSocketMap.get(socketId);
    }

    public void removeConnection(String socketId) {
        if (socketId.equals(chatModuleSocketId)) {
            log.info("Chat module disconnected. SocketId: {}", socketId);
            chatModuleSocketId = null;
        } else {
            String agentId = agentSocketMap.remove(socketId);
            if (agentId != null) {
                log.info("Agent disconnected. SocketId: {}, AgentId: {}", socketId, agentId);
            }
        }
    }

    public boolean isChatModuleConnected() {
        return chatModuleSocketId != null;
    }
} 