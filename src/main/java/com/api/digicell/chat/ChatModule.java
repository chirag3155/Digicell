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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

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

        server.addEventListener(socketConfig.EVENT_AGENT_REQUEST, Map.class, (client, data, ackSender) -> {
            try {
                String userId = (String) data.get("user_id");
                String conversationId = (String) data.get("conversation_id");
                String summary = (String) data.get("summary");
                List<Map<String, Object>> history = (List<Map<String, Object>>) data.get("history");
                String timestamp = (String) data.get("timestamp");

                log.info("Received agent request from chat module - User: {}, Conversation: {}", userId, conversationId);
                handleAgentRequest(client, userId, conversationId, summary, history, timestamp);
            } catch (Exception e) {
                log.error("Error handling agent request: {}", e.getMessage(), e);
            }
        });

        server.addEventListener(socketConfig.EVENT_MESSAGE_REQ, Map.class, (client, messageData, ackSender) -> {
            String roomId = messageData.get("conversation_id").toString();
            log.debug("Message request received in room {}: {}", roomId, messageData);
            
            // Store the message
            ChatRoom chatRoom = chatRooms.get(roomId);
            if (chatRoom != null) {
                ChatMessage message = new ChatMessage();
                message.setRoomId(roomId);
                message.setUserId(messageData.get("user_id").toString());
                message.setContent(messageData.get("transcript").toString());
                message.setMessageId(messageData.get("message_id").toString());
                message.setTimestamp(LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(Long.parseLong(messageData.get("timestamp").toString())),
                    ZoneOffset.UTC
                ));
                message.setRole("user");
                chatRoom.addMessage(message);
                
                // Forward to agent with the same format
                server.getRoomOperations(roomId).sendEvent(socketConfig.EVENT_MESSAGE_REQ_AGENT, messageData);
            }
        });

        server.addEventListener(socketConfig.EVENT_MESSAGE_RESP_AGENT, Map.class, (client, messageData, ackSender) -> {
            String roomId = messageData.get("conversation_id").toString();
            log.debug("Agent response received in room {}: {}", roomId, messageData);
            
            // Store the message
            ChatRoom chatRoom = chatRooms.get(roomId);
            if (chatRoom != null) {
                ChatMessage message = new ChatMessage();
                message.setRoomId(roomId);
                message.setUserId(messageData.get("user_id").toString());
                message.setContent(messageData.get("message").toString());
                message.setMessageId(messageData.get("message_id").toString());
                message.setMessageType(messageData.get("message_type").toString());
                message.setTimestamp(LocalDateTime.parse(
                    messageData.get("timestamp").toString().replace("Z", ""),
                    DateTimeFormatter.ISO_DATE_TIME
                ));
                message.setRole("agent");
                chatRoom.addMessage(message);
                
                // Forward to chat module with DivineMessage format
                String chatModuleSocketId = connectionService.getChatModuleSocketId();
                if (chatModuleSocketId != null) {
                    Map<String, Object> divineMessage = new HashMap<>();
                    divineMessage.put("conversation_id", roomId);
                    divineMessage.put("user_id", messageData.get("user_id"));
                    divineMessage.put("timestamp", messageData.get("timestamp"));
                    divineMessage.put("message", messageData.get("message"));
                    divineMessage.put("message_type", messageData.get("message_type"));
                    divineMessage.put("message_id", messageData.get("message_id"));
                    
                    server.getClient(UUID.fromString(chatModuleSocketId))
                          .sendEvent(socketConfig.EVENT_MESSAGE_RESP, divineMessage);
                }
            }
        });

        // Listen for agent close requests
        server.addEventListener(socketConfig.EVENT_CLOSE_AGENT, Map.class, (client, data, ackSender) -> {
            String agentId = data.get("agent_id").toString();
            String conversationId = data.get("conversation_id").toString();
            String userId = data.get("user_id").toString();
            String timestamp = data.get("timestamp").toString();
            
            // Find the chat room
            ChatRoom chatRoom = findChatRoomByConversationId(conversationId);
            if (chatRoom != null) {
                String roomId = chatRoom.getRoomId();
                
                // Get the agent
                ChatAgent agent = agentMap.get(agentId);
                if (agent != null) {
                    // Remove user from agent's room
                    agent.removeUser(userId);
                    agent.setCurrentUserCount(agent.getCurrentUserCount() - 1);
                    
                    // If this was the last user, clean up the room
                    if (agent.getCurrentUserCount() == 0) {
                        // Remove the chat room
                        chatRooms.remove(roomId);
                        log.info("Chat room {} removed as last user {} left", roomId, userId);
                    } else {
                        log.info("User {} left chat room {}, {} users remaining", userId, roomId, agent.getCurrentUserCount());
                    }
                } else {
                    log.warn("Agent {} not found for chat room {}", agentId, roomId);
                }
            } else {
                log.warn("No chat room found for conversation {}", conversationId);
            }
            
            // Get the chat module socket ID for this conversation
            String chatModuleSocketId = connectionService.getChatModuleSocketId();
            if (chatModuleSocketId != null) {
                SocketIOClient chatModuleClient = server.getClient(UUID.fromString(chatModuleSocketId));
                if (chatModuleClient != null) {
                    // Prepare close request data
                    Map<String, String> closeRequest = new HashMap<>();
                    closeRequest.put("conversation_id", conversationId);
                    closeRequest.put("user_id", userId);
                    closeRequest.put("timestamp", timestamp);
                    
                    // Send close event to chat module
                    chatModuleClient.sendEvent(socketConfig.EVENT_CLOSE, closeRequest);
                    log.info("Close event sent to chat module for conversation {} and user {}", conversationId, userId);
                } else {
                    log.warn("Chat module client not found for socket ID: {}", chatModuleSocketId);
                }
            } else {
                log.warn("No chat module socket ID found for conversation: {}", conversationId);
            }
        });

        // Listen for client close/disconnect events
        server.addEventListener(socketConfig.EVENT_CLIENT_CLOSE, Map.class, (client, data, ackSender) -> {
            String conversationId = data.get("conversation_id").toString();
            String userId = data.get("user_id").toString();
            
            // Find the chat room
            ChatRoom chatRoom = findChatRoomByConversationId(conversationId);
            if (chatRoom != null) {
                String roomId = chatRoom.getRoomId();
                String agentId = chatRoom.getAgentId();
                
                // Get the agent
                ChatAgent agent = agentMap.get(agentId);
                if (agent != null) {
                    // Remove user from agent's room
                    agent.removeUser(userId);
                    agent.setCurrentUserCount(agent.getCurrentUserCount() - 1);
                    
                    // If this was the last user, clean up the room
                    if (agent.getCurrentUserCount() == 0) {
                        // Remove the chat room
                        chatRooms.remove(roomId);
                        log.info("Chat room {} removed as last user {} left", roomId, userId);
                        
                        // Notify the agent that the chat is closed
                        String agentSocketId = connectionService.getAgentSocketId(agentId);
                        if (agentSocketId != null) {
                            SocketIOClient agentClient = server.getClient(UUID.fromString(agentSocketId));
                            if (agentClient != null) {
                                Map<String, String> closeInfo = new HashMap<>();
                                closeInfo.put("conversation_id", conversationId);
                                closeInfo.put("user_id", userId);
                                closeInfo.put("room_id", roomId);
                                
                                agentClient.sendEvent(socketConfig.EVENT_CLOSE, closeInfo);
                                log.info("Notified agent {} about chat closure for room {}", agentId, roomId);
                            }
                        }
                    } else {
                        log.info("User {} left chat room {}, {} users remaining", userId, roomId, agent.getCurrentUserCount());
                    }
                } else {
                    log.warn("Agent {} not found for chat room {}", agentId, roomId);
                }
            } else {
                log.warn("No chat room found for conversation {}", conversationId);
            }
        });

        // Handle ping from agent
        server.addEventListener(SocketConfig.EVENT_PING, Map.class, (client, data, ackSender) -> {
            try {
                String agentId = data.get("agent_id").toString();
                String socketId = client.getSessionId().toString();
                log.info("Received ping from agent: {}", agentId);
                
                // Verify this socket is actually connected with this agent ID
                String connectedAgentId = connectionService.getAgentIdBySocketId(socketId);
                if (connectedAgentId == null || !connectedAgentId.equals(agentId)) {
                    log.warn("Received ping from unregistered agent: {} (socket: {})", agentId, socketId);
                    return;
                }

                // Check if agent is already in queue
                ChatAgent agent = agentQueue.stream()
                        .filter(a -> a.getAgentId().equals(agentId))
                        .findFirst()
                        .orElse(null);

                if (agent == null) {
                    // Agent not in queue, create new agent and add to queue
                    agent = new ChatAgent(agentId);
                    agent.setOfflineRequested(false);  // Set as online
                    agentQueue.add(agent);
                    agentMap.put(agentId, agent);
                    
                    // Update agent status in database
                    try {
                        Long agentIdLong = Long.parseLong(agentId);
                        agentService.setAgentAvailable(agentIdLong);
                        log.info("Agent {} added to queue and set ONLINE", agentId);
                    } catch (NumberFormatException e) {
                        log.error("Invalid agent ID format: {}", agentId);
                    }
                } else {
                    // Agent already in queue, just update ping time
                    agent.updatePingTime();
                    log.debug("Updated ping time for agent: {}", agentId);
                }

                // Send pong response
                client.sendEvent(SocketConfig.EVENT_PONG, "pong");
            } catch (Exception e) {
                log.error("Error handling ping: {}", e.getMessage());
            }
        });

        server.addEventListener(socketConfig.EVENT_OFFLINE_REQ, String.class, (client, agentId, ackSender) -> {
            handleOfflineRequest(agentId, client);
        });
    }

    private void handleAgentRequest(SocketIOClient client, String userId, String conversationId, String summary, List<Map<String, Object>> history, String timestamp) {
        // Get the next available agent that hasn't requested offline
        ChatAgent agent = null;
        
        while (!agentQueue.isEmpty()) {
            ChatAgent peekedAgent = agentQueue.peek();
            String agentId = peekedAgent.getAgentId();
        
            
            if (peekedAgent.isOfflineRequested()) {
                // Skip this agent and try the next one
                agentQueue.poll();
                
                log.debug("Agent {} is offline requested, skipping", agentId);
            } else {
                agent = peekedAgent;
                break;
            }
        }

        if (agent != null && agent.getCurrentUserCount() < MAX_USERS_PER_AGENT) {
            String roomId = UUID.randomUUID().toString();
            
            // Create and store the chat room
            ChatRoom chatRoom = new ChatRoom(roomId, agent.getAgentId(), userId, conversationId, summary, history);
            chatRooms.put(roomId, chatRoom);
            
            // Add user to agent's room
            agent.addUser(userId);
            agent.setCurrentUserCount(agent.getCurrentUserCount() + 1);
            
            // Store socket ID mapping
            String agentSocketId = connectionService.getAgentSocketId(agent.getAgentId());
            if (agentSocketId != null) {
                SocketIOClient agentClient = server.getClient(UUID.fromString(agentSocketId));
                if (agentClient != null) {
                    // Prepare agent info data
                    Map<String, String> agentInfo = new HashMap<>();
                    agentInfo.put("status", "available");
                    agentInfo.put("agent_id", agent.getAgentId());
                    agentInfo.put("conversation_id", conversationId);
                    agentInfo.put("agent_name", agent.getAgentName());
                    agentInfo.put("agent_label", agent.getAgentLabel());
                    
                    // Send acknowledgment to chat module
                    client.sendEvent(socketConfig.EVENT_AGENT_ACK, agentInfo);
                    
                    // Notify the agent with the same agent info format
                    agentClient.sendEvent(socketConfig.EVENT_NEW_CLIENT_REQ, agentInfo);
                    
                    log.info("Agent {} assigned to user {} in room {}", agent.getAgentId(), userId, roomId);
                } else {
                    log.warn("Agent client not found for socket ID: {}", agentSocketId);
                }
            } else {
                log.warn("No socket ID found for agent: {}", agent.getAgentId());
            }
        } else {
            // No agent available
            Map<String, String> agentInfo = new HashMap<>();
            agentInfo.put("status", "unavailable");
            agentInfo.put("agent_id", "");
            agentInfo.put("conversation_id", conversationId);
            agentInfo.put("agent_name", "");
            agentInfo.put("agent_label", "");
            
            client.sendEvent(socketConfig.EVENT_AGENT_ACK, agentInfo);
            log.warn("No agent available for user {}", userId);
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

    // Helper method to find chat room by conversation ID
    private ChatRoom findChatRoomByConversationId(String conversationId) {
        for (ChatRoom room : chatRooms.values()) {
            if (room.getConversationId().equals(conversationId)) {
                return room;
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