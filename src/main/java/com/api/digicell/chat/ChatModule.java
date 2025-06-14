package com.api.digicell.chat;

import com.api.digicell.model.ChatAgent;
import com.api.digicell.model.ChatMessage;
import com.api.digicell.model.ChatRoom;
import com.api.digicell.entities.Agent;
import com.api.digicell.entities.AgentStatus;
import com.api.digicell.services.AgentService;
import com.api.digicell.dto.AgentStatusDTO;
import com.api.digicell.dto.ChatMessageRequest;
import com.api.digicell.dto.AgentMessageResponse;
import com.api.digicell.dto.ChatModuleMessageResponse;
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
    private final Map<String, String> clientAgentMapping;
    private final PriorityBlockingQueue<ChatAgent> agentQueue;
    private final Map<String, Set<String>> agentRooms;
    private final Map<String, ChatAgent> agentMap;
    private final Map<String, ChatRoom> chatRooms;
    private final AgentService agentService;
    private final SocketConfig socketConfig;
    private final SocketConnectionService connectionService;
    private static final int MAX_CLIENTS_PER_AGENT = 5;

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
        this.clientAgentMapping = new ConcurrentHashMap<>();
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
        server.addConnectListener(socketClient -> connectionService.handleConnection(socketClient));

        server.addDisconnectListener(socketClient -> {
            String socketId = socketClient.getSessionId().toString();
            connectionService.removeConnection(socketId);
            log.debug("Socket client disconnected: {}", socketId);
        });

        server.addEventListener(socketConfig.EVENT_AGENT_REQUEST, Map.class, (socketClient, data, ackSender) -> {
            try {
                String clientId = (String) data.get("client_id");
                String conversationId = (String) data.get("conversation_id");
                String summary = (String) data.get("summary");
                List<Map<String, Object>> history = (List<Map<String, Object>>) data.get("history");
                String timestamp = (String) data.get("timestamp");

                log.info("Received agent request from chat module - Client: {}, Conversation: {}", clientId, conversationId);
                handleAgentRequest(socketClient, clientId, conversationId, summary, history, timestamp);
            } catch (Exception e) {
                log.error("Error handling agent request: {}", e.getMessage(), e);
            }
        });

        server.addEventListener(socketConfig.EVENT_MESSAGE_REQ, ChatMessageRequest.class, (socketClient, messageRequest, ackSender) -> {
            String conversationId = messageRequest.getConversationId();
            log.debug("Message request received for conversation {}: {}", conversationId, messageRequest);
            
            // Store the message
            ChatRoom chatRoom = chatRooms.get(conversationId);
            if (chatRoom != null) {
                ChatMessage message = new ChatMessage();
                message.setConversationId(conversationId);
                message.setClientId(messageRequest.getClientId());
                message.setContent(messageRequest.getTranscript());
                message.setTimestamp(LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(Long.parseLong(messageRequest.getTimestamp())),
                    ZoneOffset.UTC
                ));
                message.setRole("user");
                chatRoom.addMessage(message);
                
                // Forward to agent with the same format
                server.getRoomOperations(conversationId).sendEvent(socketConfig.EVENT_MESSAGE_REQ_AGENT, messageRequest);
            }
        });

        server.addEventListener(socketConfig.EVENT_MESSAGE_RESP_AGENT, AgentMessageResponse.class, (socketClient, agentResponse, ackSender) -> {
            String conversationId = agentResponse.getConversationId();
            log.debug("Agent response received for conversation {}: {}", conversationId, agentResponse);
            
            // Store the message
            ChatRoom chatRoom = chatRooms.get(conversationId);
            if (chatRoom != null) {
                ChatMessage message = new ChatMessage();
                message.setConversationId(conversationId);
                message.setClientId(agentResponse.getClientId());
                message.setContent(agentResponse.getMessage());
                message.setTimestamp(LocalDateTime.parse(
                    agentResponse.getTimestamp().replace("Z", ""),
                    DateTimeFormatter.ISO_DATE_TIME
                ));
                message.setRole("agent");
                chatRoom.addMessage(message);
                
                // Forward to chat module with DivineMessage format
                String chatModuleSocketId = connectionService.getChatModuleSocketId();
                if (chatModuleSocketId != null) {
                    ChatModuleMessageResponse chatModuleResponse = new ChatModuleMessageResponse();
                    chatModuleResponse.setConversationId(conversationId);
                    chatModuleResponse.setClientId(agentResponse.getClientId());
                    chatModuleResponse.setTimestamp(agentResponse.getTimestamp());
                    chatModuleResponse.setMessage(agentResponse.getMessage());
                    
                    server.getClient(UUID.fromString(chatModuleSocketId))
                          .sendEvent(socketConfig.EVENT_MESSAGE_RESP, chatModuleResponse);
                }
            }
        });

        // Listen for agent close requests
        server.addEventListener(socketConfig.EVENT_CLOSE_AGENT, Map.class, (socketClient, data, ackSender) -> {
            String agentId = data.get("agent_id").toString();
            String conversationId = data.get("conversation_id").toString();
            String clientId = data.get("client_id").toString();
            String timestamp = data.get("timestamp").toString();
            
            // Find the chat room
            ChatRoom chatRoom = findChatRoomByConversationId(conversationId);
            if (chatRoom != null) {
                // Get the agent
                ChatAgent agent = agentMap.get(agentId);
                if (agent != null) {
                    // Remove client from agent's room
                    agent.removeClient(clientId);
                    agent.setCurrentClientCount(agent.getCurrentClientCount() - 1);
                    
                    // If this was the last client, clean up the room
                    if (agent.getCurrentClientCount() == 0) {
                        // Remove the chat room
                        chatRooms.remove(conversationId);
                        log.info("Chat conversation {} removed as last client {} left", conversationId, clientId);
                    } else {
                        log.info("Client {} left conversation {}, {} clients remaining", clientId, conversationId, agent.getCurrentClientCount());
                    }
                } else {
                    log.warn("Agent {} not found for conversation {}", agentId, conversationId);
                }
            } else {
                log.warn("No chat room found for conversation {}", conversationId);
            }
            
            // Get the chat module socket ID for this conversation
            String chatModuleSocketId = connectionService.getChatModuleSocketId();
            if (chatModuleSocketId != null) {
                SocketIOClient chatModuleSocketClient = server.getClient(UUID.fromString(chatModuleSocketId));
                if (chatModuleSocketClient != null) {
                    // Prepare close request data
                    Map<String, String> closeRequest = new HashMap<>();
                    closeRequest.put("conversation_id", conversationId);
                    closeRequest.put("client_id", clientId);
                    closeRequest.put("timestamp", timestamp);
                    
                    // Send close event to chat module
                    chatModuleSocketClient.sendEvent(socketConfig.EVENT_CLOSE, closeRequest);
                    log.info("Close event sent to chat module for conversation {} and client {}", conversationId, clientId);
                } else {
                    log.warn("Chat module socket client not found for socket ID: {}", chatModuleSocketId);
                }
            } else {
                log.warn("No chat module socket ID found for conversation: {}", conversationId);
            }
        });

        // Listen for client close/disconnect events
        server.addEventListener(socketConfig.EVENT_CLIENT_CLOSE, Map.class, (socketClient, data, ackSender) -> {
            String conversationId = data.get("conversation_id").toString();
            String clientId = data.get("client_id").toString();
            
            // Find the chat room
            ChatRoom chatRoom = findChatRoomByConversationId(conversationId);
            if (chatRoom != null) {
                String agentId = chatRoom.getAgentId();
                
                // Get the agent
                ChatAgent agent = agentMap.get(agentId);
                if (agent != null) {
                    // Remove client from agent's room
                    agent.removeClient(clientId);
                    agent.setCurrentClientCount(agent.getCurrentClientCount() - 1);
                    
                    // If this was the last client, clean up the room
                    if (agent.getCurrentClientCount() == 0) {
                        // Remove the chat room
                        chatRooms.remove(conversationId);
                        log.info("Chat conversation {} removed as last client {} left", conversationId, clientId);
                        
                        // Notify the agent that the chat is closed
                        String agentSocketId = connectionService.getAgentSocketId(agentId);
                        if (agentSocketId != null) {
                            SocketIOClient agentSocketClient = server.getClient(UUID.fromString(agentSocketId));
                            if (agentSocketClient != null) {
                                Map<String, String> closeInfo = new HashMap<>();
                                closeInfo.put("conversation_id", conversationId);
                                closeInfo.put("client_id", clientId);
                                
                                agentSocketClient.sendEvent(socketConfig.EVENT_CLOSE, closeInfo);
                                log.info("Notified agent {} about chat closure for conversation {}", agentId, conversationId);
                            }
                        }
                    } else {
                        log.info("Client {} left conversation {}, {} clients remaining", clientId, conversationId, agent.getCurrentClientCount());
                    }
                } else {
                    log.warn("Agent {} not found for conversation {}", agentId, conversationId);
                }
            } else {
                log.warn("No chat room found for conversation {}", conversationId);
            }
        });

        // Handle ping from agent
        server.addEventListener(SocketConfig.EVENT_PING, Map.class, (socketClient, data, ackSender) -> {
            try {
                String agentId = data.get("agent_id").toString();
                String socketId = socketClient.getSessionId().toString();
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
                socketClient.sendEvent(SocketConfig.EVENT_PONG, "pong");
            } catch (Exception e) {
                log.error("Error handling ping: {}", e.getMessage());
            }
        });

        server.addEventListener(socketConfig.EVENT_OFFLINE_REQ, String.class, (socketClient, agentId, ackSender) -> {
            handleOfflineRequest(agentId, socketClient);
        });
    }

    private void handleAgentRequest(SocketIOClient socketClient, String clientId, String conversationId, String summary, List<Map<String, Object>> history, String timestamp) {
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

        if (agent != null && agent.getCurrentClientCount() < MAX_CLIENTS_PER_AGENT) {
            // Create and store the chat room using conversationId as the key
            ChatRoom chatRoom = new ChatRoom(conversationId, agent.getAgentId(), clientId, summary, history);
            chatRooms.put(conversationId, chatRoom);
            
            // Add client to agent's room
            agent.addClient(clientId);
            agent.setCurrentClientCount(agent.getCurrentClientCount() + 1);
            
            // Store socket ID mapping
            String agentSocketId = connectionService.getAgentSocketId(agent.getAgentId());
            if (agentSocketId != null) {
                SocketIOClient agentSocketClient = server.getClient(UUID.fromString(agentSocketId));
                if (agentSocketClient != null) {
                    // Prepare agent info data
                    Map<String, String> agentInfo = new HashMap<>();
                    agentInfo.put("status", "available");
                    agentInfo.put("agent_id", agent.getAgentId());
                    agentInfo.put("conversation_id", conversationId);
                    agentInfo.put("agent_name", agent.getAgentName());
                    agentInfo.put("agent_label", agent.getAgentLabel());
                    
                    // Send acknowledgment to chat module
                    socketClient.sendEvent(socketConfig.EVENT_AGENT_ACK, agentInfo);
                    
                    // Notify the agent with the same agent info format
                    agentSocketClient.sendEvent(socketConfig.EVENT_NEW_CLIENT_REQ, agentInfo);
                    
                    log.info("Agent {} assigned to client {} for conversation {}", agent.getAgentId(), clientId, conversationId);
                } else {
                    log.warn("Agent socket client not found for socket ID: {}", agentSocketId);
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
            
            socketClient.sendEvent(socketConfig.EVENT_AGENT_ACK, agentInfo);
            log.warn("No agent available for client {}", clientId);
        }
    }

    private void handleChatClosure(String conversationId) {
        // Get the chat room
        ChatRoom chatRoom = chatRooms.get(conversationId);
        if (chatRoom != null) {
            // Close the chat room
            chatRoom.close();
            log.debug("Chat conversation {} closed at {}", conversationId, chatRoom.getEndTime());
            
            // Update agent's client count
            String agentId = chatRoom.getAgentId();
            if (agentId != null) {
                ChatAgent agent = agentMap.get(agentId);
                if (agent != null) {
                    agent.setCurrentClientCount(agent.getCurrentClientCount() - 1);
                    if (agent.getCurrentClientCount() == 0) {
                        // Agent has no more active chats
                        updateAgentStatus(agentId, AgentStatus.OFFLINE);
                    }
                }
            }
        }

        // Notify both parties
        server.getRoomOperations(conversationId).sendEvent(socketConfig.EVENT_CLOSE);
    }

    private void handleOfflineRequest(String agentId, SocketIOClient socketClient) {
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

    private String findAgentIdByConversationId(String conversationId) {
        ChatRoom chatRoom = chatRooms.get(conversationId);
        return chatRoom != null ? chatRoom.getAgentId() : null;
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