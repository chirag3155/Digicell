package com.api.digicell.chat;

import com.api.digicell.model.ChatUser;
import com.api.digicell.model.ChatMessage;
import com.api.digicell.model.ChatRoom;
import com.api.digicell.entities.UserAccount;
import com.api.digicell.entities.UserAccountStatus;
import com.api.digicell.entities.Client;
import com.api.digicell.entities.Conversation;
import com.api.digicell.services.UserAccountService;
import com.api.digicell.repository.ClientRepository;
import com.api.digicell.repository.ConversationRepository;
import com.api.digicell.repository.UserRepository;
import com.api.digicell.dto.UserAccountStatusDTO;
import com.api.digicell.dto.ChatMessageRequest;
import com.api.digicell.dto.UserMessageResponse;
import com.api.digicell.dto.ChatModuleMessageResponse;
import com.api.digicell.dto.UserCloseRequest;
import com.api.digicell.dto.UserPingRequest;
import com.api.digicell.dto.ClientInfoResponse;
import com.api.digicell.dto.ChatCloseRequest;
import com.api.digicell.dto.UserCloseNotification;
import com.api.digicell.config.SocketConfig;
import com.api.digicell.services.SocketConnectionService;
import com.corundumstudio.socketio.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
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
    private final ClientRepository clientRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final Environment environment;
    private static final int MAX_CLIENTS_PER_USER = 5;

    // SSL Configuration properties
    @Value("${socket.ssl.enabled:false}")
    private boolean sslEnabled;
    
    @Value("${socket.ssl.key-store:}")
    private String keyStorePath;
    
    @Value("${socket.ssl.key-store-password:}")
    private String keyStorePassword;

    public ChatModule(UserAccountService userAccountService, SocketConfig socketConfig, SocketConnectionService connectionService, 
                     ClientRepository clientRepository, ConversationRepository conversationRepository, UserRepository userRepository, Environment environment) {
        
        // Debug: Log the SSL property values at startup
        log.info("=== SSL CONFIGURATION DEBUG ===");
        log.info("Active Spring Profiles: {}", String.join(",", environment.getActiveProfiles()));
        log.info("Default Profiles: {}", String.join(",", environment.getDefaultProfiles()));
        log.info("SSL Enabled: {}", sslEnabled);
        log.info("SSL KeyStore Path: '{}'", keyStorePath);
        log.info("SSL KeyStore Password: '{}'", keyStorePassword != null ? "***SET***" : "null");
        
        // Test direct property resolution
        log.info("=== DIRECT PROPERTY RESOLUTION ===");
        log.info("socket.ssl.enabled = '{}'", environment.getProperty("socket.ssl.enabled"));
        log.info("socket.ssl.key-store = '{}'", environment.getProperty("socket.ssl.key-store"));
        log.info("socket.ssl.key-store-password = '{}'", environment.getProperty("socket.ssl.key-store-password") != null ? "***SET***" : "null");
        log.info("==============================");
        
        // Create configuration with SSL if enabled
        Configuration config = new Configuration();
        config.setHostname(socketConfig.getHost());
        config.setPort(socketConfig.getPort());
        config.setPingTimeout(socketConfig.getPingTimeout());
        config.setPingInterval(socketConfig.getPingInterval());
        config.setAllowCustomRequests(true);
        config.setUpgradeTimeout(10000);
        config.setTransports(Transport.WEBSOCKET, Transport.POLLING);

        // Configure SSL if enabled - Use Environment directly as fallback (safe version)
        boolean actualSslEnabled = sslEnabled || "true".equals(environment.getProperty("socket.ssl.enabled"));
        String actualKeyStorePath = keyStorePath != null ? keyStorePath : environment.getProperty("socket.ssl.key-store");
        String actualKeyStorePassword = keyStorePassword != null ? keyStorePassword : environment.getProperty("socket.ssl.key-store-password");
        
        log.info("=== FINAL SSL VALUES ===");
        log.info("Final SSL Enabled: {}", actualSslEnabled);
        log.info("Final KeyStore Path: '{}'", actualKeyStorePath);
        log.info("Final KeyStore Password: '{}'", actualKeyStorePassword != null ? "***SET***" : "null");
        log.info("========================");
        
        if (actualSslEnabled && actualKeyStorePath != null && !actualKeyStorePath.trim().isEmpty()) {
            try {
                log.info("Configuring SSL for Socket.IO server - KeyStore: {}", actualKeyStorePath);
                
                // Remove "file:" prefix if present and create File object
                String cleanPath = actualKeyStorePath.replace("file:", "");
                java.io.File keystoreFile = new java.io.File(cleanPath);
                
                if (!keystoreFile.exists()) {
                    log.error("SSL Configuration FAILED: Keystore file does not exist at: {}", keystoreFile.getAbsolutePath());
                    log.warn("SSL will be DISABLED - Server will run with HTTP only");
                } else {
                    log.info("Keystore file found: {} (size: {} bytes)", keystoreFile.getAbsolutePath(), keystoreFile.length());
                    
                    // For netty-socketio 2.0.11, SSL configuration
                    try (FileInputStream keystoreStream = new FileInputStream(keystoreFile)) {
                        config.setKeyStore(keystoreStream);
                        config.setKeyStorePassword(actualKeyStorePassword);
                        log.info("SSL keystore and password configured successfully");
                    }
                    
                    log.info("SSL configured successfully for Socket.IO server on port {}", socketConfig.getPort());
                }
            } catch (Exception e) {
                log.error("Failed to configure SSL for Socket.IO server: {}", e.getMessage(), e);
                log.error("SSL will be DISABLED - Server will run with HTTP only");
                // Continue without SSL rather than failing
            }
        } else {
            log.info("SSL disabled for Socket.IO server on port {} (enabled: {}, keystore: '{}')", 
                    socketConfig.getPort(), actualSslEnabled, actualKeyStorePath);
        }

        this.server = new SocketIOServer(config);
        this.clientUserMapping = new ConcurrentHashMap<>();
        this.userQueue = new PriorityBlockingQueue<>(100, new ChatUser.UserComparator());
        this.userRooms = new ConcurrentHashMap<>();
        this.userMap = new ConcurrentHashMap<>();
        this.chatRooms = new ConcurrentHashMap<>();
        this.userAccountService = userAccountService;
        this.socketConfig = socketConfig;
        this.connectionService = connectionService;
        this.clientRepository = clientRepository;
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
        this.environment = environment;

        initializeSocketListeners();
    }

    private void initializeSocketListeners() {
        server.addConnectListener(socketClient -> {
            String remoteAddress = socketClient.getRemoteAddress().toString();
            String sessionId = socketClient.getSessionId().toString();
            boolean isSecure = socketClient.getHandshakeData().getUrl().startsWith("https://") || 
                              socketClient.getHandshakeData().getUrl().startsWith("wss://");
            
            log.info("Socket client connected - IP: {}, SessionId: {}, Secure: {}", remoteAddress, sessionId, isSecure);
            connectionService.handleConnection(socketClient);
        });

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
                String history = (String) data.get("history");
                String timestamp = (String) data.get("timestamp");
                
                // Extract client data for database storage
                String clientName = (String) data.get("name");
                String clientEmail = (String) data.get("email");
                String clientPhone = (String) data.get("phone");
                String clientLabel = (String) data.get("label");

                log.info("Received user request from chat module - Client: {}, Conversation: {}", clientId, conversationId);
                log.info("Client Data - Name: {}, Email: {}, Phone: {}", clientName, clientEmail, clientPhone);
                log.info("Current Active Rooms: {}, User Queue Size: {}", chatRooms.size(), userQueue.size());
                
                handleUserRequest(socketClient, clientId, conversationId, summary, history, timestamp, clientName, clientEmail, clientPhone, clientLabel);
            } catch (Exception e) {
                log.error("Error handling user request: {}", e.getMessage(), e);
            }
        });

        server.addEventListener(socketConfig.EVENT_MESSAGE_REQ, ChatMessageRequest.class, (socketClient, messageRequest, ackSender) -> {
            String conversationId = messageRequest.getConversationId();
        
            log.info("Message Details - Client: {}, Content: {}, Timestamp: {}", 
                    messageRequest.getUserId(), 
                    messageRequest.getTranscript() != null ? messageRequest.getTranscript().substring(0, Math.min(100, messageRequest.getTranscript().length())) + "..." : "null",
                    messageRequest.getTimestamp());
            
            // Store the message
            ChatRoom chatRoom = chatRooms.get(conversationId);
            if (chatRoom != null) {
                log.info("Chat room found for conversation: {}, Room User: {}", conversationId, chatRoom.getUserId());
                
                ChatMessage message = new ChatMessage();
                message.setConversationId(conversationId);
                // chat module is sending the user_id as client_id
                message.setClientId(messageRequest.getUserId());
                message.setContent(messageRequest.getTranscript());
                message.setTimestamp(LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(Long.parseLong(messageRequest.getTimestamp())),
                    ZoneOffset.UTC
                ));
                message.setRole("client");
                chatRoom.addMessage(message);
                
                log.info("Forwarding message to user in room: {}", conversationId);
                // Forward to user with the same format
                log.info("Forwarding details to user with message: {}", messageRequest);
                
                // Get the user socket ID and send directly to the user
                String userSocketId = connectionService.getUserSocketId(chatRoom.getUserId());
                log.info("🔍 Looking up user socket - UserId: {}, SocketId: {}", chatRoom.getUserId(), userSocketId);
                
                if (userSocketId != null) {
                    SocketIOClient userSocketClient = server.getClient(UUID.fromString(userSocketId));
                    if (userSocketClient != null) {
                        userSocketClient.sendEvent(socketConfig.EVENT_MESSAGE_REQ_AGENT, messageRequest);
                        log.info("📤 Message sent directly to user socket: {}", userSocketId);
                    } else {
                        log.warn("Socket client not found, using room operation");
                        server.getRoomOperations(conversationId).sendEvent(socketConfig.EVENT_MESSAGE_REQ_AGENT, messageRequest);
                    }
                } else {
                    log.warn("User socket not found, using room operation (clients: {})", 
                            server.getRoomOperations(conversationId).getClients().size());
                    server.getRoomOperations(conversationId).sendEvent(socketConfig.EVENT_MESSAGE_REQ_AGENT, messageRequest);
                }
            } else {
                log.warn("No chat room found for conversation: {}", conversationId);
            }
        });

        server.addEventListener(socketConfig.EVENT_MESSAGE_RESP_AGENT, UserMessageResponse.class, (socketClient, userResponse, ackSender) -> {
            String conversationId = userResponse.getConversationId();
            
            log.info("User Response Details - Client: {}, Message: {}, Timestamp: {}", 
                    userResponse.getClientId(),
                    userResponse.getMessage() != null ? userResponse.getMessage().substring(0, Math.min(100, userResponse.getMessage().length())) + "..." : "null",
                    userResponse.getTimestamp());
            
            log.info("Active Chat Rooms ({} total):", chatRooms.size());
            chatRooms.forEach((roomId, chatRoom) -> {
                log.info("   Room: {} -> User: {}, Client: {}, Active: {}", roomId, chatRoom.getUserId(), chatRoom.getClientId(), chatRoom.isActive());
            });

            // Store the message
            ChatRoom chatRoom = chatRooms.get(conversationId);
            if (chatRoom != null) {
                log.info("✅ Chat room found for user response - Conversation: {}, Room User: {}", conversationId, chatRoom.getUserId());
                
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
                
                log.info("💾 Message stored in chat room, Total messages: {}", chatRoom.getMessages().size());
                
                // Forward to chat module with DivineMessage format
                String chatModuleSocketId = connectionService.getChatModuleSocketId();
                if (chatModuleSocketId != null) {
                    ChatModuleMessageResponse chatModuleResponse = new ChatModuleMessageResponse();
                    chatModuleResponse.setConversationId(conversationId);
                    chatModuleResponse.setClientId(userResponse.getClientId());
                    chatModuleResponse.setTimestamp(userResponse.getTimestamp());
                    chatModuleResponse.setMessage(userResponse.getMessage());
                    
                    log.info("📤 Sending response to chat module - Socket: {}, Message: {}", 
                            chatModuleSocketId, chatModuleResponse.getMessage() != null ? chatModuleResponse.getMessage().substring(0, Math.min(50, chatModuleResponse.getMessage().length())) + "..." : "null");
                    server.getClient(UUID.fromString(chatModuleSocketId))
                          .sendEvent(socketConfig.EVENT_MESSAGE_RESP, chatModuleResponse);
                } else {
                    log.warn("Chat module socket not found");
                }
            } else {
                log.warn("No chat room found for conversation: {}", conversationId);
            }
        });

        // Listen for user close requests
        server.addEventListener(socketConfig.EVENT_CLOSE_AGENT, UserCloseRequest.class, (socketClient, closeRequest, ackSender) -> {
            String userId = closeRequest.getUserId();
            String conversationId = closeRequest.getConversationId();
            String clientId = closeRequest.getClientId();
            String timestamp = closeRequest.getTimestamp();
            
            log.info("EVENT_CLOSE_AGENT - User: {}, Conversation: {}, Client: {}", userId, conversationId, clientId);
            
            // Find the chat room
            ChatRoom chatRoom = findChatRoomByConversationId(conversationId);
            if (chatRoom != null) {
                log.info("✅ Chat room found for close request - Room User: {}, Room Client: {}", chatRoom.getUserId(), chatRoom.getClientId());
                
                // Get the user
                ChatUser user = userMap.get(userId);
                if (user != null) {
                    log.info("👤 User found - Current client count: {}", user.getCurrentClientCount());
                    
                    // Remove client from user's room
                    user.removeClient(clientId);
                    user.setCurrentClientCount(user.getCurrentClientCount() - 1);
                    
                    // Remove the chat room
                    chatRooms.remove(conversationId);

                    log.info("Chat room removed - User {} now has {} active clients", userId, user.getCurrentClientCount());
                } else {
                    log.warn("User {} not found in userMap", userId);
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
                    ChatCloseRequest chatCloseRequest = new ChatCloseRequest();
                    chatCloseRequest.setConversationId(conversationId);
                    chatCloseRequest.setClientId(clientId);
                    chatCloseRequest.setTimestamp(timestamp);
                    
                    log.info("📤 Sending close event to chat module - Socket: {}, Conversation: {}", chatModuleSocketId, conversationId);
                    // Send close event to chat module
                    chatModuleSocketClient.sendEvent(socketConfig.EVENT_CLOSE, chatCloseRequest);
                } else {
                    log.warn("Chat module socket client not found");
                }
            } else {
                log.warn("Chat module socket ID not found");
            }
        });

        // Listen for client close/disconnect events
        server.addEventListener(socketConfig.EVENT_CLIENT_CLOSE, Map.class, (socketClient, data, ackSender) -> {
            String conversationId = null;
            String clientId = null;
            try {
                conversationId = data.get("conversation_id").toString();
                clientId = data.get("client_id").toString();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
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
                    
                    // Remove the chat room
                    chatRooms.remove(conversationId);

                    log.info("Chat conversation {} removed as client {} closes", conversationId, clientId);

                    log.info("user.getCurrentClientCount() {}: {}", user.getCurrentClientCount());
                    // If this was the last client, clean up the room
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

    private void handleUserRequest(SocketIOClient socketClient, String clientId, String conversationId, String summary, String history, String timestamp, String clientName, String clientEmail, String clientPhone, String clientLabel) {
        log.info("🔄 Processing user request - Conversation: {}, Client: {}", conversationId, clientId);
        
        // Check if this conversation ID already exists
        if (chatRooms.containsKey(conversationId)) {
            log.warn("⚠️ DUPLICATE REQUEST detected for conversation ID: {}. Ignoring duplicate request.", conversationId);
            ChatRoom existingRoom = chatRooms.get(conversationId);
            log.info("📋 Existing room details - User: {}, Client: {}, Active: {}", 
                    existingRoom.getUserId(), existingRoom.getClientId(), existingRoom.isActive());
            return;
        }
        
        log.info("💾 Starting database operations for conversation: {}", conversationId);
        
        // Store/Update client data in database
        try {
            log.info("👤 Saving client data - ID: {}, Name: {}, Email: {}, Phone: {}", clientId, clientName, clientEmail, clientPhone);
            saveOrUpdateClientData(clientId, clientName, clientEmail, clientPhone);
            log.info("✅ Client data saved successfully for clientId: {}", clientId);
        } catch (Exception e) {
            log.error("❌ Error saving client data for clientId {}: {}", clientId, e.getMessage(), e);
        }
        
        log.info("🔍 Looking for available user - Queue size: {}, Max clients per user: {}", userQueue.size(), MAX_CLIENTS_PER_USER);
        
        // Get the next available user that hasn't requested offline
        ChatUser user = null;
        
        while (!userQueue.isEmpty()) {
            ChatUser peekedUser = userQueue.peek();
            String userId = peekedUser.getUserId();
            
            log.info("🔎 Checking user: {} - Offline requested: {}, Current clients: {}", 
                    userId, peekedUser.isOfflineRequested(), peekedUser.getCurrentClientCount());
            
            if (peekedUser.isOfflineRequested()) {
                // Skip this user and try the next one
                userQueue.poll();
                log.info("⏭️ User {} is offline requested, skipping to next user", userId);
            } else {
                user = peekedUser;
                log.info("✅ Found available user: {} with {} current clients", userId, user.getCurrentClientCount());
                break;
            }
        }

        if (user != null && user.getCurrentClientCount() < MAX_CLIENTS_PER_USER) {
            log.info("🎯 Assigning user {} to conversation {} (client count: {}/{})", 
                    user.getUserId(), conversationId, user.getCurrentClientCount(), MAX_CLIENTS_PER_USER);
            // Create and store the chat room using conversationId as the key
            ChatRoom chatRoom = new ChatRoom(conversationId, user.getUserId(), clientId, summary, history);
            chatRooms.put(conversationId, chatRoom);

            log.info("Chat room {} on available as for client req for user {}", conversationId, chatRoom);
            
            // Add client to user's room
            user.addClient(clientId);
            user.setCurrentClientCount(user.getCurrentClientCount() + 1);
            
            // Store socket ID mapping
            String userSocketId = connectionService.getUserSocketId(user.getUserId());
            log.info("🔗 Socket mapping check - UserId: {}, SocketId: {}", user.getUserId(), userSocketId);
            
            if (userSocketId != null) {
                SocketIOClient userSocketClient = server.getClient(UUID.fromString(userSocketId));
                log.info("🔌 Socket client lookup - SocketId: {}, Client found: {}", userSocketId, userSocketClient != null);
                
                if (userSocketClient != null) {
                    // Join the user to the conversation room for message routing
                    userSocketClient.joinRoom(conversationId);
                    log.info("🏠 User {} joined room: {}", user.getUserId(), conversationId);
                    // Prepare user info data
                    ClientInfoResponse userInfo = new ClientInfoResponse();
                    userInfo.setStatus("online");
                    userInfo.setUserId(user.getUserId());
                    userInfo.setConversationId(conversationId);
                    userInfo.setClientName(clientName);
                    userInfo.setClientLabel(clientLabel);
                    userInfo.setClientEmail(clientEmail);
                    userInfo.setClientPhone(clientPhone);
                    
                    // Send acknowledgment to chat module
                    log.info("sending acknowledgment to chat module for user {}", userInfo);
                    socketClient.sendEvent(socketConfig.EVENT_AGENT_ACK, userInfo);
                    
                    // Notify the user with the same user info format
                    log.info("sending acknowledgment to user with user info {}", userInfo);
                    userSocketClient.sendEvent(socketConfig.EVENT_NEW_CLIENT_REQ, userInfo);
                    
                    log.info("User {} assigned to client {} for conversation {}", user.getUserId(), clientId, conversationId);
                    
                    // Debug: Show current room memberships
                    log.info("🏠 Room memberships for conversation {}: {}", conversationId, 
                            server.getRoomOperations(conversationId).getClients().size());
                    log.info("📊 Total connected clients: {}", server.getAllClients().size());
                    
                    // Store conversation data in database
                    try {
                        log.info("💾 Saving conversation data - Conversation: {}, User: {}, Client: {}", conversationId, user.getUserId(), clientId);
                        saveConversationData(conversationId, user.getUserId(), clientId);
                        log.info("✅ Conversation data saved successfully");
                    } catch (Exception e) {
                        log.error("❌ Error saving conversation data for conversationId {}: {}", conversationId, e.getMessage(), e);
                    }
                } else {
                    log.warn("User socket client not found for socket ID: {}", userSocketId);
                }
            } else {
                log.warn("No socket ID found for user: {}", user.getUserId());
            }
        } else {
            // No user available
            log.warn("❌ NO USER AVAILABLE - Queue empty: {}, User limit reached: {}", 
                    userQueue.isEmpty(), user != null ? "Yes (current: " + user.getCurrentClientCount() + ")" : "N/A");
            
            ClientInfoResponse userInfo = new ClientInfoResponse();
            userInfo.setStatus("unavailable");
            userInfo.setUserId("");
            userInfo.setConversationId(conversationId);
            userInfo.setClientName("");
            userInfo.setClientLabel("");
            
            log.info("📤 Sending unavailable response to chat module for client: {}", clientId);
            socketClient.sendEvent(socketConfig.EVENT_AGENT_ACK, userInfo);
        }
    }

    // private void handleChatClosure(String conversationId) {
    //     // Get the chat room
    //     ChatRoom chatRoom = chatRooms.get(conversationId);
    //     if (chatRoom != null) {
    //         // Close the chat room
    //         chatRoom.close();
    //         log.debug("Chat conversation {} closed at {}", conversationId, chatRoom.getEndTime());
            
    //         // Update user's client count
    //         String userId = chatRoom.getUserId();
    //         if (userId != null) {
    //             ChatUser user = userMap.get(userId);
    //             if (user != null) {
    //                 user.setCurrentClientCount(user.getCurrentClientCount() - 1);
    //                 if (user.getCurrentClientCount() == 0) {
    //                     // User has no more active chats
    //                     updateUserStatus(userId, UserAccountStatus.OFFLINE);
    //                 }
    //             }
    //         }
    //     }

    //     // Notify both parties
    //     server.getRoomOperations(conversationId).sendEvent(socketConfig.EVENT_CLOSE);
    // }

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
    
    /**
     * Save or update client data in the database.
     * Handles nullable fields appropriately.
     */
    private void saveOrUpdateClientData(String clientId, String clientName, String clientEmail, String clientPhone) {
        if (clientId == null || clientId.trim().isEmpty()) {
            log.warn("Cannot save client data: clientId is null or empty");
            return;
        }
        
        try {
            // Check if client already exists
            Client existingClient = clientRepository.findById(clientId).orElse(null);
            
            if (existingClient != null) {
                // Update existing client if new data is provided
                boolean updated = false;
                
                if (clientName != null && !clientName.trim().isEmpty() && !clientName.equals(existingClient.getName())) {
                    existingClient.setName(clientName.trim());
                    updated = true;
                }
                
                if (clientEmail != null && !clientEmail.trim().isEmpty() && !clientEmail.equals(existingClient.getEmail())) {
                    existingClient.setEmail(clientEmail.trim());
                    updated = true;
                }
                
                if (clientPhone != null && !clientPhone.trim().isEmpty() && !clientPhone.equals(existingClient.getPhone())) {
                    existingClient.setPhone(clientPhone.trim());
                    updated = true;
                }
                
                if (updated) {
                    clientRepository.save(existingClient);
                    log.info("Updated existing client data for clientId: {}", clientId);
                } else {
                    log.debug("No updates needed for existing client: {}", clientId);
                }
            } else {
                // Create new client
                Client newClient = Client.builder()
                    .clientId(clientId)
                    .name(clientName != null && !clientName.trim().isEmpty() ? clientName.trim() : "Unknown")
                    .email(clientEmail != null && !clientEmail.trim().isEmpty() ? clientEmail.trim() : "unknown@example.com")
                    .phone(clientPhone != null && !clientPhone.trim().isEmpty() ? clientPhone.trim() : "N/A")
                    .isAssigned(true)
                    .build();
                
                clientRepository.save(newClient);
                log.info("Created new client with clientId: {}", clientId);
            }
        } catch (Exception e) {
            log.error("Error saving/updating client data for clientId {}: {}", clientId, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Save conversation data in the database.
     * Links the conversation with the client and user.
     */
    private void saveConversationData(String conversationId, String userId, String clientId) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            log.warn("Cannot save conversation data: conversationId is null or empty");
            return;
        }
        
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("Cannot save conversation data: userId is null or empty");
            return;
        }
        
        if (clientId == null || clientId.trim().isEmpty()) {
            log.warn("Cannot save conversation data: clientId is null or empty");
            return;
        }
        
        try {
            // Check if conversation already exists
            Conversation existingConversation = conversationRepository.findById(conversationId).orElse(null);
            
            if (existingConversation != null) {
                log.debug("Conversation {} already exists in database", conversationId);
                return;
            }
            
            // Get client and user entities
            Client client = clientRepository.findById(clientId).orElse(null);
            if (client == null) {
                log.error("Client not found with id: {} when saving conversation", clientId);
                return;
            }
            
            UserAccount userAccount = userRepository.findById(Long.parseLong(userId)).orElse(null);
            if (userAccount == null) {
                log.error("User not found with id: {} when saving conversation", userId);
                return;
            }
            
            // Create new conversation with only essential fields
            Conversation conversation = new Conversation();
            conversation.setConversationId(conversationId);
            conversation.setClient(client);
            conversation.setUserAccount(userAccount);
            
            conversationRepository.save(conversation);
            log.info("Created new conversation with conversationId: {} for user: {} and client: {}", 
                    conversationId, userId, clientId);
            
        } catch (NumberFormatException e) {
            log.error("Invalid userId format: {} when saving conversation", userId);
        } catch (Exception e) {
            log.error("Error saving conversation data for conversationId {}: {}", conversationId, e.getMessage(), e);
            throw e;
        }
    }
} 