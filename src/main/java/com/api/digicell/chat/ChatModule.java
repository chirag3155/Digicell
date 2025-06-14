package com.api.digicell.chat;

import com.api.digicell.model.ChatUser;
import com.api.digicell.model.ChatMessage;
import com.api.digicell.model.ChatRoom;
import com.api.digicell.entities.UserAccount;
import com.api.digicell.entities.UserAccountStatus;
import com.api.digicell.services.UserAccountService;
import com.api.digicell.dto.UserAccountStatusDTO;
import com.api.digicell.dto.ChatMessageRequest;
import com.api.digicell.dto.UserMessageResponse;
import com.api.digicell.dto.ChatModuleMessageResponse;
import com.api.digicell.dto.UserCloseRequest;
import com.api.digicell.dto.UserPingRequest;
import com.api.digicell.dto.UserInfoResponse;
import com.api.digicell.dto.ChatCloseRequest;
import com.api.digicell.dto.UserCloseNotification;
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
    private final Map<String, String> clientUserMapping;
    private final PriorityBlockingQueue<ChatUser> userQueue;
    private final Map<String, Set<String>> userRooms;
    private final Map<String, ChatUser> userMap;
    private final Map<String, ChatRoom> chatRooms;
    private final UserAccountService userAccountService;
    private final SocketConfig socketConfig;
    private final SocketConnectionService connectionService;
    private static final int MAX_CLIENTS_PER_USER = 5;

    public ChatModule(UserAccountService userAccountService, SocketConfig socketConfig, SocketConnectionService connectionService) {
        Configuration config = new Configuration();
        config.setHostname(socketConfig.getHost());
        config.setPort(socketConfig.getPort());
        config.setPingTimeout(socketConfig.getPingTimeout());
        config.setPingInterval(socketConfig.getPingInterval());
        config.setAllowCustomRequests(true);
        config.setUpgradeTimeout(10000);
        config.setTransports(Transport.WEBSOCKET, Transport.POLLING);

        this.server = new SocketIOServer(config);
        this.clientUserMapping = new ConcurrentHashMap<>();
        this.userQueue = new PriorityBlockingQueue<>(100, new ChatUser.UserComparator());
        this.userRooms = new ConcurrentHashMap<>();
        this.userMap = new ConcurrentHashMap<>();
        this.chatRooms = new ConcurrentHashMap<>();
        this.userAccountService = userAccountService;
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

                log.info("Received user request from chat module - Client: {}, Conversation: {}", clientId, conversationId);
                handleUserRequest(socketClient, clientId, conversationId, summary, history, timestamp);
            } catch (Exception e) {
                log.error("Error handling user request: {}", e.getMessage(), e);
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
                
                // Forward to user with the same format
                server.getRoomOperations(conversationId).sendEvent(socketConfig.EVENT_MESSAGE_REQ_AGENT, messageRequest);
            }
        });

        server.addEventListener(socketConfig.EVENT_MESSAGE_RESP_AGENT, UserMessageResponse.class, (socketClient, userResponse, ackSender) -> {
            String conversationId = userResponse.getConversationId();
            log.debug("User response received for conversation {}: {}", conversationId, userResponse);
            
            // Store the message
            ChatRoom chatRoom = chatRooms.get(conversationId);
            if (chatRoom != null) {
                ChatMessage message = new ChatMessage();
                message.setConversationId(conversationId);
                message.setClientId(userResponse.getClientId());
                message.setContent(userResponse.getMessage());
                message.setTimestamp(LocalDateTime.parse(
                    userResponse.getTimestamp().replace("Z", ""),
                    DateTimeFormatter.ISO_DATE_TIME
                ));
                message.setRole("user");
                chatRoom.addMessage(message);
                
                // Forward to chat module with DivineMessage format
                String chatModuleSocketId = connectionService.getChatModuleSocketId();
                if (chatModuleSocketId != null) {
                    ChatModuleMessageResponse chatModuleResponse = new ChatModuleMessageResponse();
                    chatModuleResponse.setConversationId(conversationId);
                    chatModuleResponse.setClientId(userResponse.getClientId());
                    chatModuleResponse.setTimestamp(userResponse.getTimestamp());
                    chatModuleResponse.setMessage(userResponse.getMessage());
                    
                    server.getClient(UUID.fromString(chatModuleSocketId))
                          .sendEvent(socketConfig.EVENT_MESSAGE_RESP, chatModuleResponse);
                }
            }
        });

        // Listen for user close requests
        server.addEventListener(socketConfig.EVENT_CLOSE_AGENT, UserCloseRequest.class, (socketClient, closeRequest, ackSender) -> {
            String userId = closeRequest.getUserId();
            String conversationId = closeRequest.getConversationId();
            String clientId = closeRequest.getClientId();
            String timestamp = closeRequest.getTimestamp();
            
            // Find the chat room
            ChatRoom chatRoom = findChatRoomByConversationId(conversationId);
            if (chatRoom != null) {
                // Get the user
                ChatUser user = userMap.get(userId);
                if (user != null) {
                    // Remove client from user's room
                    user.removeClient(clientId);
                    user.setCurrentClientCount(user.getCurrentClientCount() - 1);
                    
                    // If this was the last client, clean up the room
                    if (user.getCurrentClientCount() == 0) {
                        // Remove the chat room
                        chatRooms.remove(conversationId);
                        log.info("Chat conversation {} removed as last client {} left", conversationId, clientId);
                    } else {
                        log.info("Client {} left conversation {}, {} clients remaining", clientId, conversationId, user.getCurrentClientCount());
                    }
                } else {
                    log.warn("User {} not found for conversation {}", userId, conversationId);
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
                    ChatCloseRequest closeRequest = new ChatCloseRequest();
                    closeRequest.setConversationId(conversationId);
                    closeRequest.setClientId(clientId);
                    closeRequest.setTimestamp(timestamp);
                    
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
                String userId = chatRoom.getUserId();
                
                // Get the user
                ChatUser user = userMap.get(userId);
                if (user != null) {
                    // Remove client from user's room
                    user.removeClient(clientId);
                    user.setCurrentClientCount(user.getCurrentClientCount() - 1);
                    
                    // If this was the last client, clean up the room
                    if (user.getCurrentClientCount() == 0) {
                        // Remove the chat room
                        chatRooms.remove(conversationId);
                        log.info("Chat conversation {} removed as last client {} left", conversationId, clientId);
                        
                        // Notify the user that the chat is closed
                        String userSocketId = connectionService.getUserSocketId(userId);
                        if (userSocketId != null) {
                            SocketIOClient userSocketClient = server.getClient(UUID.fromString(userSocketId));
                            if (userSocketClient != null) {
                                UserCloseNotification closeInfo = new UserCloseNotification();
                                closeInfo.setConversationId(conversationId);
                                closeInfo.setClientId(clientId);
                                
                                userSocketClient.sendEvent(socketConfig.EVENT_CLOSE, closeInfo);
                                log.info("Notified user {} about chat closure for conversation {}", userId, conversationId);
                            }
                        }
                    } else {
                        log.info("Client {} left conversation {}, {} clients remaining", clientId, conversationId, user.getCurrentClientCount());
                    }
                } else {
                    log.warn("User {} not found for conversation {}", userId, conversationId);
                }
            } else {
                log.warn("No chat room found for conversation {}", conversationId);
            }
        });

        // Handle ping from user
        server.addEventListener(SocketConfig.EVENT_PING, UserPingRequest.class, (socketClient, pingRequest, ackSender) -> {
            try {
                String userId = pingRequest.getUserId();
                String socketId = socketClient.getSessionId().toString();
                log.info("Received ping from user: {}", userId);
                
                // Verify this socket is actually connected with this user ID
                String connectedUserId = connectionService.getUserIdBySocketId(socketId);
                if (connectedUserId == null || !connectedUserId.equals(userId)) {
                    log.warn("Received ping from unregistered user: {} (socket: {})", userId, socketId);
                    return;
                }

                // Check if user is already in queue
                ChatUser user = userQueue.stream()
                        .filter(u -> u.getUserId().equals(userId))
                        .findFirst()
                        .orElse(null);

                if (user == null) {
                    // User not in queue, create new user and add to queue
                    user = new ChatUser(userId);
                    user.setOfflineRequested(false);  // Set as online
                    userQueue.add(user);
                    userMap.put(userId, user);
                    
                    // Update user status in database
                    try {
                        Long userIdLong = Long.parseLong(userId);
                        userAccountService.setUserONLINE(userIdLong);
                        log.info("User {} added to queue and set ONLINE", userId);
                    } catch (NumberFormatException e) {
                        log.error("Invalid user ID format: {}", userId);
                    }
                } else {
                    // User already in queue, just update ping time
                    user.updatePingTime();
                    log.debug("Updated ping time for user: {}", userId);
                }

                // Send pong response
                socketClient.sendEvent(SocketConfig.EVENT_PONG, "pong");
            } catch (Exception e) {
                log.error("Error handling ping: {}", e.getMessage());
            }
        });

        server.addEventListener(socketConfig.EVENT_OFFLINE_REQ, UserPingRequest.class, (socketClient, offlineRequest, ackSender) -> {
            handleOfflineRequest(offlineRequest.getUserId(), socketClient);
        });
    }

    private void handleUserRequest(SocketIOClient socketClient, String clientId, String conversationId, String summary, List<Map<String, Object>> history, String timestamp) {
        // Get the next available user that hasn't requested offline
        ChatUser user = null;
        
        while (!userQueue.isEmpty()) {
            ChatUser peekedUser = userQueue.peek();
            String userId = peekedUser.getUserId();
        
            
            if (peekedUser.isOfflineRequested()) {
                // Skip this user and try the next one
                userQueue.poll();
                
                log.debug("User {} is offline requested, skipping", userId);
            } else {
                user = peekedUser;
                break;
            }
        }

        if (user != null && user.getCurrentClientCount() < MAX_CLIENTS_PER_USER) {
            // Create and store the chat room using conversationId as the key
            ChatRoom chatRoom = new ChatRoom(conversationId, user.getUserId(), clientId, summary, history);
            chatRooms.put(conversationId, chatRoom);
            
            // Add client to user's room
            user.addClient(clientId);
            user.setCurrentClientCount(user.getCurrentClientCount() + 1);
            
            // Store socket ID mapping
            String userSocketId = connectionService.getUserSocketId(user.getUserId());
            if (userSocketId != null) {
                SocketIOClient userSocketClient = server.getClient(UUID.fromString(userSocketId));
                if (userSocketClient != null) {
                    // Prepare user info data
                    UserInfoResponse userInfo = new UserInfoResponse();
                    userInfo.setStatus("online");
                    userInfo.setUserId(user.getUserId());
                    userInfo.setConversationId(conversationId);
                    userInfo.setUserName(user.getUserName());
                    userInfo.setUserLabel(user.getUserLabel());
                    
                    // Send acknowledgment to chat module
                    socketClient.sendEvent(socketConfig.EVENT_AGENT_ACK, userInfo);
                    
                    // Notify the user with the same user info format
                    userSocketClient.sendEvent(socketConfig.EVENT_NEW_CLIENT_REQ, userInfo);
                    
                    log.info("User {} assigned to client {} for conversation {}", user.getUserId(), clientId, conversationId);
                } else {
                    log.warn("User socket client not found for socket ID: {}", userSocketId);
                }
            } else {
                log.warn("No socket ID found for user: {}", user.getUserId());
            }
        } else {
            // No user available
            UserInfoResponse userInfo = new UserInfoResponse();
            userInfo.setStatus("unavailable");
            userInfo.setUserId("");
            userInfo.setConversationId(conversationId);
            userInfo.setUserName("");
            userInfo.setUserLabel("");
            
            socketClient.sendEvent(socketConfig.EVENT_AGENT_ACK, userInfo);
            log.warn("No user available for client {}", clientId);
        }
    }

    private void handleChatClosure(String conversationId) {
        // Get the chat room
        ChatRoom chatRoom = chatRooms.get(conversationId);
        if (chatRoom != null) {
            // Close the chat room
            chatRoom.close();
            log.debug("Chat conversation {} closed at {}", conversationId, chatRoom.getEndTime());
            
            // Update user's client count
            String userId = chatRoom.getUserId();
            if (userId != null) {
                ChatUser user = userMap.get(userId);
                if (user != null) {
                    user.setCurrentClientCount(user.getCurrentClientCount() - 1);
                    if (user.getCurrentClientCount() == 0) {
                        // User has no more active chats
                        updateUserStatus(userId, UserAccountStatus.OFFLINE);
                    }
                }
            }
        }

        // Notify both parties
        server.getRoomOperations(conversationId).sendEvent(socketConfig.EVENT_CLOSE);
    }

    private void handleOfflineRequest(String userId, SocketIOClient socketClient) {
        ChatUser user = userMap.get(userId);
        if (user != null) {
            // Check if user has any active chats
            Set<String> userActiveRooms = userRooms.get(userId);
            if (userActiveRooms == null || userActiveRooms.isEmpty()) {
                // No active chats, can go offline
                updateUserStatus(userId, UserAccountStatus.OFFLINE);
                log.info("User {} status updated to OFFLINE", userId);
            } else {
                // Has active chats, can't go offline yet
                log.warn("User {} has active chats, cannot go offline", userId);
                // TODO: Implement notification to user UI about pending chats
            }
        }
    }

    private void updateUserStatus(String userId, UserAccountStatus status) {
        try {
            UserAccountStatusDTO statusDTO = new UserAccountStatusDTO(status);
            userAccountService.updateUserStatus(Long.parseLong(userId), statusDTO);
            log.info("User {} status updated to {}", userId, status);
        } catch (Exception e) {
            log.error("Error updating user {} status: {}", userId, e.getMessage(), e);
        }
    }

    private String findUserIdByConversationId(String conversationId) {
        ChatRoom chatRoom = chatRooms.get(conversationId);
        return chatRoom != null ? chatRoom.getUserId() : null;
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