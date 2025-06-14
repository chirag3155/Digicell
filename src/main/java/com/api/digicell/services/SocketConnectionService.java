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
    private Map<String, String> agentSocketIds = new ConcurrentHashMap<>();

    public SocketConnectionService(SocketConfig socketConfig) {
        this.socketConfig = socketConfig;
        this.agentSocketMap = new ConcurrentHashMap<>();
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
            handleAgentConnection(socketClient, clientType);
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

    private void handleAgentConnection(SocketIOClient socketClient, String clientType) {
        String agentId = socketClient.getHandshakeData().getSingleUrlParam("agentId");
        if (agentId == null || agentId.trim().isEmpty()) {
            log.warn("Agent connection rejected: Missing agentId. SocketId: {}", socketClient.getSessionId());
            socketClient.disconnect();
            return;
        }

        String socketId = socketClient.getSessionId().toString();
        agentSocketMap.put(socketId, agentId);
        agentSocketIds.put(agentId, socketId);
        log.info("Agent connected successfully. SocketId: {}, AgentId: {}, Type: {}", 
                socketId, agentId, clientType);
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
                agentSocketIds.remove(agentId);
                log.info("Agent disconnected. SocketId: {}, AgentId: {}", socketId, agentId);
            }
        }
    }

    public boolean isChatModuleConnected() {
        return chatModuleSocketId != null;
    }

    public String getAgentSocketId(String agentId) {
        return agentSocketIds.get(agentId);
    }

    public void setAgentSocketId(String agentId, String socketId) {
        agentSocketIds.put(agentId, socketId);
    }

    public void removeAgentSocketId(String agentId) {
        agentSocketIds.remove(agentId);
    }
} 