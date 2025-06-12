package com.api.digicell.chat;

import com.api.digicell.model.ChatAgent;
import com.api.digicell.model.ChatMessage;
import com.api.digicell.model.ChatRoom;
import com.api.digicell.entities.Agent;
import com.api.digicell.entities.AgentStatus;
import com.api.digicell.services.AgentService;
import com.api.digicell.dto.AgentStatusDTO;
import com.api.digicell.config.SocketConfig;
import com.api.digicell.services.SocketConnectionService;
import com.corundumstudio.socketio.*;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

@Slf4j
@Component
public class ChatModule {
    private final SocketIOServer server;
    private final Map<String, String> userAgentMapping;
    private final PriorityBlockingQueue<ChatAgent> agentQueue;
    private final Map<String, Set<String>> agentRooms;
    private final Map<String, ChatAgent> agentMap;
    private final Map<String, ChatRoom> chatRooms;
    private final AgentService agentService;
    private final SocketConfig socketConfig;
    private final SocketConnectionService connectionService;
    private static final int MAX_USERS_PER_AGENT = 5;

    public ChatModule(AgentService agentService, SocketConfig socketConfig, SocketConnectionService connectionService) {
        Configuration config = new Configuration();
        config.setHostname(socketConfig.getHost());
        config.setPort(socketConfig.getPort());
        config.setPingTimeout(socketConfig.getPingTimeout());
        config.setPingInterval(socketConfig.getPingInterval());
        config.setAllowCustomRequests(true);
        config.setUpgradeTimeout(10000);
        config.setTransports(Transport.WEBSOCKET, Transport.POLLING);

        this.server = new SocketIOServer(config);
        this.userAgentMapping = new ConcurrentHashMap<>();
        this.agentQueue = new PriorityBlockingQueue<>(100, new ChatAgent.AgentComparator());
        this.agentRooms = new ConcurrentHashMap<>();
        this.agentMap = new ConcurrentHashMap<>();
        this.chatRooms = new ConcurrentHashMap<>();
        this.agentService = agentService;
        this.socketConfig = socketConfig;
        this.connectionService = connectionService;

        initializeSocketListeners();
    }

    private void initializeSocketListeners() {
        server.addConnectListener(client -> connectionService.handleConnection(client));

        server.addDisconnectListener(client -> {
            String socketId = client.getSessionId().toString();
            connectionService.removeConnection(socketId);
            log.debug("Client disconnected: {}", socketId);
        });

        server.addEventListener(socketConfig.EVENT_AGENT_REQUEST, String.class, (client, userId, ackSender) -> {
            log.debug("User {} requesting agent", userId);
            handleAgentRequest(userId, client);
        });

        server.addEventListener(socketConfig.EVENT_MESSAGE_REQ, ChatMessage.class, (client, message, ackSender) -> {
            String roomId = message.getRoomId();
            log.debug("Message request received in room {}: {}", roomId, message);
            
            // Store the message
            ChatRoom chatRoom = chatRooms.get(roomId);
            if (chatRoom != null) {
                message.setTimestamp(LocalDateTime.now());
                message.setRole("user");
                chatRoom.addMessage(message);
            }
            
            // Forward to agent
            server.getRoomOperations(roomId).sendEvent(socketConfig.EVENT_MESSAGE_REQ_AGENT, message);
        });

        server.addEventListener(socketConfig.EVENT_MESSAGE_RESP_AGENT, ChatMessage.class, (client, message, ackSender) -> {
            String roomId = message.getRoomId();
            log.debug("Agent response received in room {}: {}", roomId, message);
            
            // Store the message
            ChatRoom chatRoom = chatRooms.get(roomId);
            if (chatRoom != null) {
                message.setTimestamp(LocalDateTime.now());
                message.setRole("agent");
                chatRoom.addMessage(message);
                
                // Forward to chat module
                String chatModuleSocketId = connectionService.getChatModuleSocketId();
                if (chatModuleSocketId != null) {
                    server.getClient(UUID.fromString(chatModuleSocketId))
                          .sendEvent(socketConfig.EVENT_MESSAGE_RESP, message);
                }
            }
        });

        server.addEventListener(socketConfig.EVENT_CLOSE_AGENT, String.class, (client, roomId, ackSender) -> {
            log.debug("Agent closing chat for room: {}", roomId);
            handleChatClosure(roomId);
        });

        server.addEventListener(socketConfig.EVENT_CLIENT_CLOSE, String.class, (client, roomId, ackSender) -> {
            log.debug("Client closing chat for room: {}", roomId);
            handleChatClosure(roomId);
        });

        server.addEventListener(socketConfig.EVENT_PING, String.class, (client, agentId, ackSender) -> {
            try {
                // Update agent status to online
                Agent agent = agentService.setAgentAvailable(Long.parseLong(agentId));
                
                // Create or update ChatAgent and add to queue
                ChatAgent chatAgent = agentMap.computeIfAbsent(agentId, id -> {
                    ChatAgent newAgent = new ChatAgent(id);
                    newAgent.setCurrentUserCount(0);
                    return newAgent;
                });
                
                // Remove from queue if exists and add back to ensure proper ordering
                agentQueue.remove(chatAgent);
                agentQueue.offer(chatAgent);
                
                // Emit response to UI
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Agent " + agentId + " status updated to ONLINE");
                response.put("status", "success");
                client.sendEvent(socketConfig.EVENT_PING_RESPONSE, response);
                
                log.debug("Agent {} status updated to ONLINE and added to queue", agentId);
            } catch (Exception e) {
                log.error("Error updating agent {} status: {}", agentId, e.getMessage(), e);
                // Send error response to UI
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("message", "Failed to update agent status");
                errorResponse.put("status", "error");
                errorResponse.put("error", e.getMessage());
                client.sendEvent(socketConfig.EVENT_PING_RESPONSE, errorResponse);
            }
        });

        server.addEventListener(socketConfig.EVENT_OFFLINE_REQ, String.class, (client, agentId, ackSender) -> {
            handleOfflineRequest(agentId, client);
        });
    }

    private void handleAgentRequest(String userId, SocketIOClient client) {
        ChatAgent agent = agentQueue.poll();
        if (agent != null && agent.getCurrentUserCount() < MAX_USERS_PER_AGENT) {
            String roomId = UUID.randomUUID().toString();
            
            // Create and store the chat room
            ChatRoom chatRoom = new ChatRoom(roomId, agent.getAgentId(), userId);
            chatRooms.put(roomId, chatRoom);
            
            // Update mappings
            userAgentMapping.put(userId, agent.getAgentId());
            agent.setCurrentUserCount(agent.getCurrentUserCount() + 1);
            agentQueue.offer(agent);
            agentRooms.computeIfAbsent(agent.getAgentId(), k -> new HashSet<>()).add(roomId);
            
            // Join both the user and agent to the room
            client.joinRoom(roomId);
            
            // Prepare acknowledgment data
            Map<String, Object> ackData = new HashMap<>();
            ackData.put("status", socketConfig.STATUS_SUCCESS);
            ackData.put("roomId", roomId);
            ackData.put("agentId", agent.getAgentId());

            // Notify chat module
            String chatModuleSocketId = connectionService.getChatModuleSocketId();
            if (chatModuleSocketId != null) {
                server.getClient(UUID.fromString(chatModuleSocketId))
                      .sendEvent(socketConfig.EVENT_AGENT_ACK, ackData);
            }
            
            // Notify agent
            server.getRoomOperations(roomId).sendEvent(socketConfig.EVENT_NEW_CLIENT_REQ, ackData);
            
            log.debug("Created room {} and assigned agent {} to user {}", roomId, agent.getAgentId(), userId);
        } else {
            // Notify chat module of unavailability
            String chatModuleSocketId = connectionService.getChatModuleSocketId();
            if (chatModuleSocketId != null) {
                Map<String, Object> ackData = new HashMap<>();
                ackData.put("status", socketConfig.STATUS_UNAVAILABLE);
                ackData.put("message", "No agent available");
                server.getClient(UUID.fromString(chatModuleSocketId))
                      .sendEvent(socketConfig.EVENT_AGENT_ACK, ackData);
            }
            
            log.warn("No agent available for user: {}", userId);
        }
    }

    private void handleChatClosure(String roomId) {
        // Get the chat room
        ChatRoom chatRoom = chatRooms.get(roomId);
        if (chatRoom != null) {
            // Close the chat room
            chatRoom.close();
            log.debug("Chat room {} closed at {}", roomId, chatRoom.getEndTime());
        }

        // Notify both parties
        server.getRoomOperations(roomId).sendEvent(socketConfig.EVENT_CLOSE);
        
        // Update agent's user count
        String agentId = findAgentIdByRoomId(roomId);
        if (agentId != null) {
            ChatAgent agent = agentMap.get(agentId);
            if (agent != null) {
                agent.setCurrentUserCount(agent.getCurrentUserCount() - 1);
                if (agent.getCurrentUserCount() == 0) {
                    // Agent has no more active chats
                    updateAgentStatus(agentId, AgentStatus.OFFLINE);
                }
            }
        }
    }

    private void handleOfflineRequest(String agentId, SocketIOClient client) {
        ChatAgent agent = agentMap.get(agentId);
        if (agent != null) {
            // Check if agent has any active chats
            Set<String> agentActiveRooms = agentRooms.get(agentId);
            if (agentActiveRooms == null || agentActiveRooms.isEmpty()) {
                // No active chats, can go offline
                updateAgentStatus(agentId, AgentStatus.OFFLINE);
                log.info("Agent {} status updated to OFFLINE", agentId);
            } else {
                // Has active chats, can't go offline yet
                log.warn("Agent {} has active chats, cannot go offline", agentId);
                // TODO: Implement notification to agent UI about pending chats
            }
        }
    }

    private void updateAgentStatus(String agentId, AgentStatus status) {
        try {
            AgentStatusDTO statusDTO = new AgentStatusDTO(status);
            agentService.updateAgentStatus(Long.parseLong(agentId), statusDTO);
            log.info("Agent {} status updated to {}", agentId, status);
        } catch (Exception e) {
            log.error("Error updating agent {} status: {}", agentId, e.getMessage(), e);
        }
    }

    private String findAgentIdByRoomId(String roomId) {
        for (Map.Entry<String, Set<String>> entry : agentRooms.entrySet()) {
            if (entry.getValue().contains(roomId)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public void start() {
        server.start();
        log.info("Chat module started on port {}", socketConfig.getPort());
    }

    public void stop() {
        server.stop();
        log.info("Chat module stopped");
    }
} 