package com.api.digicell.chat;

import com.api.digicell.model.ChatUser;
import com.api.digicell.model.ChatMessage;
import com.api.digicell.model.ChatRoom;
import com.api.digicell.entities.UserAccount;
import com.api.digicell.entities.UserAccountStatus;
import com.api.digicell.entities.Client;
import com.api.digicell.entities.Conversation;
import com.api.digicell.entities.UserOrgPermissions;
import com.api.digicell.entities.Organization;
import com.api.digicell.services.UserAccountService;
import com.api.digicell.repository.ClientRepository;
import com.api.digicell.repository.ConversationRepository;
import com.api.digicell.repository.UserRepository;
import com.api.digicell.repository.UserOrgPermissionsRepository;
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
import org.springframework.scheduling.TaskScheduler;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class ChatModule {
    private final SocketIOServer server;
    private final Map<String, String> clientUserMapping;
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
    private final UserOrgPermissionsRepository userOrgPermissionsRepository;
    private final TaskScheduler taskScheduler;
    private static final int MAX_CLIENTS_PER_USER = 5;

    // SSL Configuration properties
    @Value("${socket.ssl.enabled:false}")
    private boolean sslEnabled;
    
    @Value("${socket.ssl.key-store:}")
    private String keyStorePath;
    
    @Value("${socket.ssl.key-store-password:}")
    private String keyStorePassword;

    // Tenant-aware user pools for efficient assignment
    private final Map<String, Set<String>> tenantUserPools = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> userClientCounts = new ConcurrentHashMap<>();

    public ChatModule(UserAccountService userAccountService, SocketConfig socketConfig, SocketConnectionService connectionService, 
                     ClientRepository clientRepository, ConversationRepository conversationRepository, UserRepository userRepository, Environment environment, UserOrgPermissionsRepository userOrgPermissionsRepository, TaskScheduler taskScheduler) {
        
        log.info("🚀 CHATMODULE CONSTRUCTOR STARTED - Initializing chat module with dependencies...");
        
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
        
        log.info("⚙️ CREATING SOCKET.IO CONFIGURATION - Setting up server configuration...");
        // Create configuration with SSL if enabled
        Configuration config = new Configuration();
        config.setHostname(socketConfig.getHost());
        config.setPort(socketConfig.getPort());
        config.setPingTimeout(socketConfig.getPingTimeout());
        config.setPingInterval(socketConfig.getPingInterval());
        config.setAllowCustomRequests(true);
        config.setUpgradeTimeout(10000);
        config.setTransports(Transport.WEBSOCKET, Transport.POLLING);

        log.info("📡 CONFIGURING TRANSPORT SETTINGS - WebSocket and Polling enabled");
        
        log.info("🔐 EVALUATING SSL CONFIGURATION - Checking if SSL should be enabled...");
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
            log.info("🔒 SSL CONFIGURATION ENABLED - Starting SSL setup process...");
            try {
                log.info("📂 LOADING SSL KEYSTORE - KeyStore path: {}", actualKeyStorePath);
                
                log.info("🛠️ PROCESSING KEYSTORE PATH - Removing file: prefix if present...");
                // Remove "file:" prefix if present and create File object
                String cleanPath = actualKeyStorePath.replace("file:", "");
                
                log.info("📁 CREATING FILE OBJECT - Creating file object for path: {}", cleanPath);
                java.io.File keystoreFile = new java.io.File(cleanPath);
                
                log.info("🔍 VALIDATING KEYSTORE FILE - Checking if file exists...");
                if (!keystoreFile.exists()) {
                    log.error("SSL Configuration FAILED: Keystore file does not exist at: {}", keystoreFile.getAbsolutePath());
                    log.warn("SSL will be DISABLED - Server will run with HTTP only");
                } else {
                    log.info("Keystore file found: {} (size: {} bytes)", keystoreFile.getAbsolutePath(), keystoreFile.length());
                    
                    // For netty-socketio 2.0.11, SSL configuration
                    // Note: Don't use try-with-resources here as netty-socketio needs the stream to remain open
                    FileInputStream keystoreStream = new FileInputStream(keystoreFile);
                    config.setKeyStore(keystoreStream);
                    config.setKeyStorePassword(actualKeyStorePassword);
                    log.info("SSL keystore and password configured successfully");
                    
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

        log.info("🖥️ CREATING SOCKET.IO SERVER - Initializing server with configuration...");
        this.server = new SocketIOServer(config);
        
        log.info("🗂️ INITIALIZING DATA STRUCTURES - Creating concurrent hash maps...");
        this.clientUserMapping = new ConcurrentHashMap<>();
        this.userRooms = new ConcurrentHashMap<>();
        this.userMap = new ConcurrentHashMap<>();
        this.chatRooms = new ConcurrentHashMap<>();
        
        log.info("🔗 ASSIGNING DEPENDENCIES - Linking service dependencies...");
        this.userAccountService = userAccountService;
        this.socketConfig = socketConfig;
        this.connectionService = connectionService;
        this.clientRepository = clientRepository;
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
        this.environment = environment;
        this.userOrgPermissionsRepository = userOrgPermissionsRepository;
        this.taskScheduler = taskScheduler;

        log.info("🎧 INITIALIZING SOCKET LISTENERS - Setting up event handlers...");
        initializeSocketListeners();
        
        log.info("✅ CHATMODULE CONSTRUCTOR COMPLETED - Chat module fully initialized");
    }

    private void initializeSocketListeners() {
        log.info("🔧 Initializing socket event listeners...");
        
        server.addConnectListener(socketClient -> {
            log.info("📥 NEW CONNECTION ATTEMPT - Starting connection handling...");
            String remoteAddress = socketClient.getRemoteAddress().toString();
            String sessionId = socketClient.getSessionId().toString();
            boolean isSecure = socketClient.getHandshakeData().getUrl().startsWith("https://") || 
                              socketClient.getHandshakeData().getUrl().startsWith("wss://");
            
            log.info("📡 Connection details captured - IP: {}, SessionId: {}, Secure: {}", remoteAddress, sessionId, isSecure);
            log.info("🔄 Delegating to SocketConnectionService for connection validation...");
            connectionService.handleConnection(socketClient);
            log.info("✅ Connection handling completed for session: {}", sessionId);
        });

        server.addDisconnectListener(socketClient -> {
            log.info("📤 DISCONNECTION EVENT - Starting disconnect handling...");
            String socketId = socketClient.getSessionId().toString();
            log.info("🔍 Looking up user for disconnecting socket: {}", socketId);
            String userId = connectionService.getUserIdBySocketId(socketId);
            
            // Check if this is a user disconnection with active conversations
            if (userId != null) {
                log.info("👤 User {} identified for disconnection, checking active conversations...", userId);
                Set<String> activeConversations = connectionService.getUserActiveConversations(userId);
                if (activeConversations != null && !activeConversations.isEmpty()) {
                    log.info("🔔 User {} has {} active conversations, scheduling disconnection notifications...", 
                            userId, activeConversations.size());
                    // Send notifications after a short delay to allow for immediate reconnection
                    scheduleDisconnectionNotifications(userId, activeConversations);
                } else {
                    log.info("📭 User {} has no active conversations, no notifications needed", userId);
                }
            } else {
                log.info("❓ No user found for disconnecting socket: {}", socketId);
            }
            
            log.info("🔄 Delegating to SocketConnectionService for connection cleanup...");
            connectionService.removeConnection(socketId);
            log.info("✅ Disconnect handling completed for socket: {}", socketId);
        });

        log.info("📋 Setting up EVENT_AGENT_REQUEST listener...");
        server.addEventListener(socketConfig.EVENT_AGENT_REQUEST, Map.class, (socketClient, data, ackSender) -> {
            log.info("🎯 EVENT_AGENT_REQUEST RECEIVED - Starting request processing...");
            try {
                log.info("🔍 Extracting request data from incoming event...");
                String clientId = (String) data.get("client_id");
                String conversationId = (String) data.get("conversation_id");
                String summary = (String) data.get("summary");
                String history = (String) data.get("history");
                String timestamp = (String) data.get("timestamp");
                String assistantId = (String) data.get("assistant_id");
                String tenantId = (String) data.get("tenant_id");
                
                log.info("📊 Basic request data extracted - Client: {}, Conversation: {}, Assistant: {}, Tenant: {}", 
                        clientId, conversationId, assistantId, tenantId);
                
                log.info("🔍 Extracting customer details from nested object...");
                // Extract customer details from nested object
                Map<String, Object> customerDetails = (Map<String, Object>) data.get("customer_details");
                String clientName = null;
                String clientEmail = null;
                String clientPhone = null;
                String clientLabel = null;
                
                if (customerDetails != null) {
                    log.info("📝 Customer details object found, extracting fields...");
                    clientName = (String) customerDetails.get("name");
                    clientEmail = (String) customerDetails.get("email");
                    clientPhone = (String) customerDetails.get("phoneNumber");
                    clientLabel = (String) customerDetails.get("label");
                    log.info("👤 Customer details extracted - Name: {}, Email: {}, Phone: {}, Label: {}", 
                            clientName, clientEmail, clientPhone, clientLabel);
                } else {
                    log.warn("⚠️ No customer details found in request data");
                }

                log.info("📈 Current system status - Active Rooms: {}, Total Online Users: {}", chatRooms.size(), userMap.size());
                
                log.info("🔄 Delegating to handleUserRequest for conversation assignment...");
                handleUserRequest(socketClient, clientId, conversationId, summary, history, timestamp, clientName, clientEmail, clientPhone, clientLabel,tenantId);
                log.info("✅ EVENT_AGENT_REQUEST processing completed");
            } catch (Exception e) {
                log.error("❌ Error in EVENT_AGENT_REQUEST processing: {}", e.getMessage(), e);
            }
        });

        log.info("📋 Setting up EVENT_MESSAGE_REQ listener...");
        server.addEventListener(socketConfig.EVENT_MESSAGE_REQ, ChatMessageRequest.class, (socketClient, messageRequest, ackSender) -> {
            log.info("💬 EVENT_MESSAGE_REQ RECEIVED - Starting message processing...");
            String conversationId = messageRequest.getConversationId();
        
            log.info("📝 Message request details - Conversation: {}, Client: {}, Content length: {}, Timestamp: {}", 
                    conversationId,
                    messageRequest.getUserId(), 
                    messageRequest.getTranscript() != null ? messageRequest.getTranscript().length() : 0,
                    messageRequest.getTimestamp());
            
            log.info("🔍 Looking up chat room for conversation: {}", conversationId);
            // Store the message
            ChatRoom chatRoom = chatRooms.get(conversationId);
            if (chatRoom != null) {
                log.info("✅ Chat room found - Room User: {}, Room Client: {}", chatRoom.getUserId(), chatRoom.getClientId());
                
                log.info("💾 Creating and storing message in chat room...");
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
                log.info("✅ Message stored in chat room, Total messages: {}", chatRoom.getMessages().size());
                
                log.info("🔍 Looking up user socket for message forwarding - UserId: {}", chatRoom.getUserId());
                // Get the user socket ID and send directly to the user
                String userSocketId = connectionService.getUserSocketId(chatRoom.getUserId());
                log.info("🔗 Socket lookup result - UserId: {}, SocketId: {}", chatRoom.getUserId(), userSocketId);
                
                if (userSocketId != null) {
                    log.info("🎯 Attempting direct socket delivery to user...");
                    SocketIOClient userSocketClient = server.getClient(UUID.fromString(userSocketId));
                    if (userSocketClient != null) {
                        log.info("📤 Sending message directly to user socket: {}", userSocketId);
                        userSocketClient.sendEvent(socketConfig.EVENT_MESSAGE_REQ_AGENT, messageRequest);
                        log.info("✅ Message delivered directly to user socket");
                    } else {
                        log.warn("⚠️ Socket client not found, falling back to room operation");
                        server.getRoomOperations(conversationId).sendEvent(socketConfig.EVENT_MESSAGE_REQ_AGENT, messageRequest);
                        log.info("📡 Message sent via room operation fallback");
                    }
                } else {
                    log.warn("⚠️ User socket not found, using room operation (room clients: {})", 
                            server.getRoomOperations(conversationId).getClients().size());
                    server.getRoomOperations(conversationId).sendEvent(socketConfig.EVENT_MESSAGE_REQ_AGENT, messageRequest);
                    log.info("📡 Message sent via room operation");
                }
            } else {
                log.error("❌ No chat room found for conversation: {}", conversationId);
            }
            log.info("✅ EVENT_MESSAGE_REQ processing completed");
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
                    
                    // Update atomic client count
                    decrementUserClientCount(userId);
                    
                    // Remove the chat room
                    chatRooms.remove(conversationId);
                    
                    // Remove conversation from tracking
                    connectionService.removeUserConversation(userId, conversationId);

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
                    
                    // Update atomic client count
                    decrementUserClientCount(userId);
                    
                    // Remove the chat room
                    chatRooms.remove(conversationId);
                    
                    // Remove conversation from tracking
                    connectionService.removeUserConversation(userId, conversationId);

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
        log.info("📋 Setting up EVENT_PING listener...");
        server.addEventListener(SocketConfig.EVENT_PING, UserPingRequest.class, (socketClient, pingRequest, ackSender) -> {
            log.info("🏓 EVENT_PING RECEIVED - Starting ping processing...");
            try {
                String userId = pingRequest.getUserId();
                String socketId = socketClient.getSessionId().toString();
                log.info("📍 Ping details - UserId: {}, SocketId: {}", userId, socketId);
                
                log.info("🔍 Verifying socket registration for user...");
                // Verify this socket is actually connected with this user ID
                String connectedUserId = connectionService.getUserIdBySocketId(socketId);
                if (connectedUserId == null || !connectedUserId.equals(userId)) {
                    log.warn("⚠️ Ping from unregistered user: {} (socket: {}), rejecting", userId, socketId);
                    return;
                }
                log.info("✅ Socket registration verified for user: {}", userId);

                log.info("🔍 Checking if user exists in userMap...");
                // Check if user is already in queue
                ChatUser user = userMap.get(userId);

                if (user == null) {
                    log.info("👤 New user detected, creating user object and adding to system...");
                    // User not in queue, create new user and add to queue
                    user = new ChatUser(userId);
                    user.setOfflineRequested(false);  // Set as online
                    userMap.put(userId, user);
                    log.info("✅ User object created and added to userMap");
                    
                    log.info("🏢 Adding user to relevant tenant pools...");
                    // Add user to relevant tenant pools for efficient assignment
                    addUserToTenantPools(userId);
                    log.info("✅ User added to tenant pools");
                    
                    log.info("userMap: {}", userMap);
                    log.info("Attempting to set user {} to ONLINE", userId);
                    // Update user status in database
                    try {
                        log.info("💾 Updating user status to ONLINE in database...");
                        Long userIdLong = Long.parseLong(userId);
                        userAccountService.setUserONLINE(userIdLong);
                        log.info("✅ User {} status set to ONLINE in database", userId);
                    } catch (NumberFormatException e) {
                        log.error("❌ Invalid user ID format for database update: {}", userId);
                    }
                } else {
                    log.info("🔄 Existing user found, updating ping time...");
                    // User already in queue, just update ping time
                    user.updatePingTime();
                    log.info("✅ Ping time updated for user: {}", userId);
                }

                log.info("📤 Sending PONG response to user...");
                // Send pong response
                socketClient.sendEvent(SocketConfig.EVENT_PONG, "pong");
                log.info("✅ PONG sent to user: {}", userId);
                log.info("✅ EVENT_PING processing completed for user: {}", userId);
            } catch (Exception e) {
                log.error("❌ Error in EVENT_PING processing: {}", e.getMessage(), e);
            }
        });

        server.addEventListener(socketConfig.EVENT_OFFLINE_REQ, UserPingRequest.class, (socketClient, offlineRequest, ackSender) -> {
            log.info("📴 EVENT_OFFLINE_REQ RECEIVED - Starting offline request processing...");
            try {
                if (offlineRequest == null) {
                    log.error("❌ OFFLINE REQUEST VALIDATION FAILED - Received null offline request object");
                    return;
                }
                
                String userId = offlineRequest.getUserId();
                if (userId == null || userId.trim().isEmpty()) {
                    log.error("❌ OFFLINE REQUEST VALIDATION FAILED - Missing or empty userId in offline request");
                    return;
                }
                
                log.info("👤 Offline request details - UserId: {}, SocketId: {}", userId, socketClient.getSessionId());
                handleOfflineRequest(userId, socketClient);
                log.info("✅ EVENT_OFFLINE_REQ processing completed for user: {}", userId);
            } catch (Exception e) {
                log.error("❌ Error in EVENT_OFFLINE_REQ processing: {}", e.getMessage(), e);
            }
        });
    }

    private void handleUserRequest(SocketIOClient socketClient, String clientId, String conversationId, String summary, String history, String timestamp, String clientName, String clientEmail, String clientPhone, String clientLabel,String tenantId) {
        log.info("🎯 HANDLE_USER_REQUEST STARTED - Processing conversation assignment...");
        log.info("📋 Request details - Conversation: {}, Client: {}, Tenant: {}", conversationId, clientId, tenantId);
        log.info("👤 Customer info - Name: {}, Email: {}, Phone: {}", clientName, clientEmail, clientPhone);
        
        log.info("🔍 Checking for duplicate conversation ID...");
        // Check if this conversation ID already exists
        if (chatRooms.containsKey(conversationId)) {
            log.warn("⚠️ DUPLICATE CONVERSATION DETECTED - Conversation {} already exists, rejecting request", conversationId);
            ChatRoom existingRoom = chatRooms.get(conversationId);
            log.info("📋 Existing room details - User: {}, Client: {}, Active: {}", 
                    existingRoom.getUserId(), existingRoom.getClientId(), existingRoom.isActive());
            return;
        }
        log.info("✅ Conversation ID is unique, proceeding with assignment");
        
        log.info("💾 Starting database operations...");
        
        // Store/Update client data in database
        try {
            log.info("👤 Saving/updating client data in database...");
            log.info("📝 Client data - ID: {}, Name: {}, Email: {}, Phone: {}", clientId, clientName, clientEmail, clientPhone);
            saveOrUpdateClientData(clientId, clientName, clientEmail, clientPhone);
            log.info("✅ Client data saved successfully for clientId: {}", clientId);
        } catch (Exception e) {
            log.error("❌ Error saving client data for clientId {}: {}", clientId, e.getMessage(), e);
        }
        
        log.info("🔍 Looking for available user - Current users: {}, Max clients per user: {}", userMap.size(), MAX_CLIENTS_PER_USER);
        
        // Debug: Log current system state
        log.info("🔍 SYSTEM DEBUG - Current online users: {}", userMap.keySet());
        log.info("🏢 TENANT POOLS DEBUG - Available tenant pools: {}", tenantUserPools.keySet());
        tenantUserPools.forEach((tenant, users) -> {
            log.info("   Tenant '{}' has {} users: {}", tenant, users.size(), users);
        });
        
        log.info("🎯 Starting efficient tenant-aware user lookup...");
        // Use efficient tenant-aware assignment instead of blocking queue processing
        ChatUser user = findUserForTenantEfficiently(tenantId);
        
        if (user != null && user.getCurrentClientCount() < MAX_CLIENTS_PER_USER) {
            log.info("✅ Available user found for assignment");
            log.info("🎯 Assigning user {} to conversation {} (current clients: {}/{})", 
                    user.getUserId(), conversationId, user.getCurrentClientCount(), MAX_CLIENTS_PER_USER);
                    
            log.info("🏠 Creating chat room for conversation...");
            // Create and store the chat room using conversationId as the key
            ChatRoom chatRoom = new ChatRoom(conversationId, user.getUserId(), clientId, summary, history);
            chatRooms.put(conversationId, chatRoom);
            log.info("✅ Chat room created and stored - Room ID: {}", conversationId);
            
            log.info("📊 Tracking conversation for user preservation...");
            // Track this conversation for the user to preserve it during reconnections
            connectionService.addUserConversation(user.getUserId(), conversationId);
            log.info("✅ Conversation tracking added for user: {}", user.getUserId());

            log.info("📈 Updating user client counts...");
            // Add client to user's room
            user.addClient(clientId);
            user.setCurrentClientCount(user.getCurrentClientCount() + 1);
            
            // Update atomic client count for efficient tracking
            incrementUserClientCount(user.getUserId());
            log.info("✅ User client count updated - User: {}, New count: {}", user.getUserId(), user.getCurrentClientCount());
            
            log.info("🔗 Looking up user socket for room joining...");
            // Store socket ID mapping
            String userSocketId = connectionService.getUserSocketId(user.getUserId());
            log.info("🔍 Socket mapping check - UserId: {}, SocketId: {}", user.getUserId(), userSocketId);
            
            if (userSocketId != null) {
                log.info("🎯 Attempting to get socket client for user...");
                SocketIOClient userSocketClient = server.getClient(UUID.fromString(userSocketId));
                log.info("🔌 Socket client lookup result - SocketId: {}, Client found: {}", userSocketId, userSocketClient != null);
                
                if (userSocketClient != null) {
                    log.info("🏠 Joining user to conversation room...");
                    // Join the user to the conversation room for message routing
                    userSocketClient.joinRoom(conversationId);
                    log.info("✅ User {} joined room: {}", user.getUserId(), conversationId);
                    
                    log.info("🔄 Checking for preserved conversations to restore...");
                    // Check if user has any preserved conversations to restore
                    restoreUserConversations(user.getUserId(), userSocketClient);
                    
                    log.info("📤 Sending reconnection notifications...");
                    // Send reconnection notifications if user had active conversations
                    sendUserReconnectionNotifications(user.getUserId());
                    
                    log.info("📝 Preparing user info response...");
                    // Prepare user info data
                    ClientInfoResponse userInfo = new ClientInfoResponse();
                    userInfo.setStatus("online");
                    userInfo.setUserId(user.getUserId());
                    userInfo.setConversationId(conversationId);
                    userInfo.setClientName(clientName);
                    userInfo.setClientLabel(clientLabel);
                    userInfo.setClientEmail(clientEmail);
                    userInfo.setClientPhone(clientPhone);
                    
                    log.info("📤 Sending acknowledgment to chat module...");
                    // Send acknowledgment to chat module
                    socketClient.sendEvent(socketConfig.EVENT_AGENT_ACK, userInfo);
                    log.info("✅ Acknowledgment sent to chat module");
                    
                    log.info("📤 Sending notification to assigned user...");
                    // Notify the user with the same user info format
                    userSocketClient.sendEvent(socketConfig.EVENT_NEW_CLIENT_REQ, userInfo);
                    log.info("✅ Notification sent to user");
                    
                    log.info("📊 Current room statistics - Conversation {}: {} members, Total clients: {}", 
                            conversationId, server.getRoomOperations(conversationId).getClients().size(), 
                            server.getAllClients().size());
                    
                    // Store conversation data in database
                    try {
                        log.info("💾 Saving conversation data to database...");
                        saveConversationData(conversationId, user.getUserId(), clientId);
                        log.info("✅ Conversation data saved successfully");
                    } catch (Exception e) {
                        log.error("❌ Error saving conversation data for conversationId {}: {}", conversationId, e.getMessage(), e);
                    }
                    
                    log.info("✅ User assignment completed successfully - User: {}, Conversation: {}", user.getUserId(), conversationId);
                } else {
                    log.error("❌ User socket client not found for socket ID: {}", userSocketId);
                }
            } else {
                log.error("❌ No socket ID found for user: {}", user.getUserId());
            }
        } else {
            log.warn("❌ NO USER AVAILABLE for assignment");
            // No user available
            log.warn("💔 Assignment failed - Queue empty: {}, User limit reached: {}", 
                    userMap.isEmpty(), user != null ? "Yes (current: " + user.getCurrentClientCount() + ")" : "N/A");
            
            log.info("📝 Preparing unavailable response...");
            ClientInfoResponse userInfo = new ClientInfoResponse();
            userInfo.setStatus("unavailable");
            userInfo.setUserId("");
            userInfo.setConversationId(conversationId);
            userInfo.setClientName("");
            userInfo.setClientLabel("");
            
            log.info("📤 Sending unavailable response to chat module for client: {}", clientId);
            socketClient.sendEvent(socketConfig.EVENT_AGENT_ACK, userInfo);
            log.info("✅ Unavailable response sent to chat module");
        }
        log.info("✅ HANDLE_USER_REQUEST COMPLETED for conversation: {}", conversationId);
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
        log.info("📴 HANDLE_OFFLINE_REQUEST STARTED - Processing offline request...");
        log.info("👤 Offline request details - UserId: {}, SocketId: {}", userId, socketClient.getSessionId());
        
        log.info("🔍 LOOKING UP USER - Checking if user exists in userMap...");
        ChatUser user = userMap.get(userId);
        
        if (user != null) {
            log.info("✅ USER FOUND - User exists in system, checking active chats...");
            log.info("📊 User current status - ClientCount: {}, OfflineRequested: {}", user.getCurrentClientCount(), user.isOfflineRequested());
            
            // Check if user has any active chats using active conversations instead of userRooms
            Set<String> activeConversations = connectionService.getUserActiveConversations(userId);
            log.info("🔍 CHECKING ACTIVE CONVERSATIONS - Found {} active conversations", activeConversations != null ? activeConversations.size() : 0);
            
            if (activeConversations == null || activeConversations.isEmpty()) {
                log.info("✅ NO ACTIVE CHATS - User can go offline safely");
                
                log.info("📊 UPDATING USER STATUS - Setting status to OFFLINE...");
                // No active chats, can go offline
                updateUserStatus(userId, UserAccountStatus.OFFLINE);
                
                log.info("🏢 REMOVING FROM TENANT POOLS - Cleaning up user assignments...");
                // Remove from tenant pools
                removeUserFromTenantPools(userId);
                
                log.info("✅ OFFLINE REQUEST COMPLETED - User {} status updated to OFFLINE and removed from tenant pools", userId);
            } else {
                log.warn("⚠️ OFFLINE REQUEST REJECTED - User {} has {} active chats, cannot go offline yet", userId, activeConversations.size());
                log.info("📋 Active conversations: {}", activeConversations);
                // TODO: Implement notification to user UI about pending chats
                
                // Send response to user about pending chats
                Map<String, Object> response = Map.of(
                    "status", "offline_rejected",
                    "reason", "active_conversations",
                    "active_conversations", activeConversations.size(),
                    "message", "Cannot go offline while having active conversations"
                );
                socketClient.sendEvent("offline_response", response);
                log.info("📤 Sent offline rejection response to user: {}", userId);
            }
        } else {
            log.warn("❌ USER NOT FOUND - User {} not found in userMap, cannot process offline request", userId);
            
            // Send error response to user
            Map<String, Object> response = Map.of(
                "status", "error", 
                "reason", "user_not_found",
                "message", "User not found in system"
            );
            socketClient.sendEvent("offline_response", response);
            log.info("📤 Sent error response to user: {}", userId);
        }
        
        log.info("✅ HANDLE_OFFLINE_REQUEST COMPLETED for user: {}", userId);
    }

    private void updateUserStatus(String userId, UserAccountStatus status) {
        log.info("📊 UPDATE_USER_STATUS STARTED - Updating user status...");
        log.info("👤 Status update details - UserId: {}, NewStatus: {}", userId, status);
        
        try {
            log.info("🏗️ CREATING STATUS DTO - Building status data transfer object...");
            UserAccountStatusDTO statusDTO = new UserAccountStatusDTO(status);
            
            log.info("💾 UPDATING DATABASE - Persisting user status change...");
            userAccountService.updateUserStatus(Long.parseLong(userId), statusDTO);
            log.info("✅ STATUS UPDATED SUCCESSFULLY - User {} status updated to {}", userId, status);
        } catch (Exception e) {
            log.error("❌ STATUS UPDATE FAILED - Error updating user {} status: {}", userId, e.getMessage(), e);
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
    
    /**
     * Check if user belongs to the specified tenant ID
     */
    private boolean isUserBelongsToTenant(String userId, String tenantId) {
        log.info("🏢 TENANT VALIDATION STARTED - Checking user tenant access...");
        log.info("🔍 Validation details - UserId: {}, TenantId: {}", userId, tenantId);
        
        // If tenantId is null or empty, allow any user (backward compatibility)
        if (tenantId == null || tenantId.trim().isEmpty()) {
            log.debug("ℹ️ NO TENANT RESTRICTION - No tenant ID specified, allowing user {}", userId);
            return true;
        }
        
        try {
            log.info("🔢 PARSING USER ID - Converting string to long: {}", userId);
            Long userIdLong = Long.parseLong(userId);
            
            log.info("👤 FETCHING USER ACCOUNT - Looking up user by ID...");
            UserAccount userAccount = userRepository.findById(userIdLong).orElse(null);
            
            if (userAccount == null) {
                log.warn("❌ USER NOT FOUND - User {} not found in database, denying access to tenant {}", userId, tenantId);
                return false;
            }
            
            log.info("🏢 CHECKING ORGANIZATION TENANTS - Validating user organization access...");
            // Check if user belongs to any organization with the matching tenant ID
            boolean belongs = checkUserOrganizationTenants(userAccount, tenantId);
            
            log.debug("User {} tenant check: requestedTenant={}, belongs={}", 
                    userId, tenantId, belongs);
            
            return belongs;
            
        } catch (NumberFormatException e) {
            log.error("Invalid user ID format: {}", userId);
            return false;
        } catch (Exception e) {
            log.error("Error checking tenant for user {}: {}", userId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Check if user belongs to any organization with the specified tenant ID
     * Uses Set to avoid checking duplicate organizations for efficiency
     */
    private boolean checkUserOrganizationTenants(UserAccount userAccount, String targetTenantId) {
        try {
            Set<Organization> userOrganizations = userOrgPermissionsRepository.findDistinctOrganizationsByUserAccount(userAccount);
            
            if (userOrganizations == null || userOrganizations.isEmpty()) {
                log.debug("User {} has no organizations", userAccount.getUserId());
                return false;
            }
            
            for (Organization org : userOrganizations) {
                String orgTenantId = org.getTenantId();
                if (targetTenantId.equals(orgTenantId)) {
                    log.debug("User {} belongs to organization with matching tenant {}", 
                             userAccount.getUserId(), targetTenantId);
                    return true;
                }
            }
            
            log.debug("User {} has {} organizations but none match tenant {}", 
                     userAccount.getUserId(), userOrganizations.size(), targetTenantId);
            return false;
            
        } catch (Exception e) {
            log.error("Error checking user {} organization tenants: {}", userAccount.getUserId(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Schedule disconnection notifications after a brief delay to allow for quick reconnections
     */
    private void scheduleDisconnectionNotifications(String userId, Set<String> activeConversations) {
        log.info("⏰ SCHEDULING DISCONNECTION NOTIFICATIONS - User: {}, Conversations: {}", userId, activeConversations.size());
        
        // Schedule notification after 10 seconds to allow for immediate reconnection
        taskScheduler.schedule(() -> {
            log.info("🔔 Executing scheduled disconnection notifications - User: {}", userId);
            
            // Check if user is still disconnected
            String currentSocketId = connectionService.getUserSocketId(userId);
            if (currentSocketId == null) {
                log.info("📤 User {} still disconnected, sending notifications...", userId);
                
                // Send disconnect notification to all clients in active conversations
                for (String conversationId : activeConversations) {
                    log.info("📢 Sending disconnect notification for conversation: {}", conversationId);
                    
                    Map<String, Object> notification = Map.of(
                        "type", "AGENT_DISCONNECTED_NOTIFICATION",
                        "message", "Agent has been disconnected unexpectedly",
                        "conversationId", conversationId,
                        "userId", userId,
                        "timestamp", System.currentTimeMillis()
                    );
                    
                    server.getRoomOperations(conversationId).sendEvent("notification", notification);
                    log.info("✅ Disconnect notification sent for conversation: {}", conversationId);
                }
            } else {
                log.info("🔄 User {} has reconnected, skipping disconnect notifications", userId);
            }
        }, Instant.now().plusSeconds(10));
        
        log.info("✅ Disconnection notifications scheduled for user: {}", userId);
    }
    
    /**
     * Send reconnection notifications when user comes back online
     */
    private void sendUserReconnectionNotifications(String userId) {
        log.info("🔔 SENDING RECONNECTION NOTIFICATIONS - User: {}", userId);
        
        Set<String> activeConversations = connectionService.getUserActiveConversations(userId);
        if (activeConversations != null && !activeConversations.isEmpty()) {
            log.info("📤 Sending reconnection notifications for {} conversations", activeConversations.size());
            
            for (String conversationId : activeConversations) {
                log.info("📢 Sending reconnection notification for conversation: {}", conversationId);
                
                Map<String, Object> notification = Map.of(
                    "type", "AGENT_RECONNECTED_NOTIFICATION",
                    "message", "Agent has reconnected and is back online",
                    "conversationId", conversationId,
                    "userId", userId,
                    "timestamp", System.currentTimeMillis()
                );
                
                server.getRoomOperations(conversationId).sendEvent("notification", notification);
                log.info("✅ Reconnection notification sent for conversation: {}", conversationId);
            }
        } else {
            log.info("📭 No active conversations found for user: {}", userId);
        }
        
        log.info("✅ Reconnection notifications completed for user: {}", userId);
    }
     
    /**
     * Restore user conversations when they reconnect within the preservation window
     */
    private void restoreUserConversations(String userId, SocketIOClient userSocketClient) {
        Set<String> preservedConversations = connectionService.getUserActiveConversations(userId);
        
        if (preservedConversations != null && !preservedConversations.isEmpty()) {
            log.info("🔄 Restoring {} preserved conversations for reconnected user {}: {}", 
                    preservedConversations.size(), userId, preservedConversations);
            
            // Re-join all preserved conversation rooms
            for (String conversationId : preservedConversations) {
                try {
                    userSocketClient.joinRoom(conversationId);
                    log.info("🏠 User {} rejoined preserved room: {}", userId, conversationId);
                    
                    // Send notification that conversation is restored
                    ClientInfoResponse restoreInfo = new ClientInfoResponse();
                    restoreInfo.setStatus("restored");
                    restoreInfo.setUserId(userId);
                    restoreInfo.setConversationId(conversationId);
                    
                    // Find the chat room to get client details
                    ChatRoom chatRoom = chatRooms.get(conversationId);
                    if (chatRoom != null) {
                        restoreInfo.setClientName("Conversation Restored");
                        restoreInfo.setClientLabel("active");
                        userSocketClient.sendEvent(socketConfig.EVENT_NEW_CLIENT_REQ, restoreInfo);
                        log.info("📤 Sent conversation restore notification to user {} for conversation {}", 
                                userId, conversationId);
                    }
                } catch (Exception e) {
                    log.error("❌ Error restoring conversation {} for user {}: {}", 
                            conversationId, userId, e.getMessage(), e);
                }
            }
        } else {
            log.debug("No preserved conversations to restore for user {}", userId);
        }
    }

    public void start() {
        log.info("🚀 START METHOD CALLED - Initiating Socket.IO server startup...");
        try {
            log.info("⚡ STARTING SERVER - Socket.IO server on port {} with SSL enabled...", socketConfig.getPort());
            server.start();
            log.info("✅ SERVER STARTED SUCCESSFULLY - Chat module running on port {}", socketConfig.getPort());
        } catch (Exception e) {
            log.error("❌ SERVER STARTUP FAILED - Socket.IO server failed to start: {}", e.getMessage(), e);
            throw new RuntimeException("Socket.IO server startup failed", e);
        }
    }

    public void stop() {
        log.info("🛑 STOP METHOD CALLED - Initiating Socket.IO server shutdown...");
        server.stop();
        log.info("✅ SERVER STOPPED SUCCESSFULLY - Chat module shutdown completed");
    }
    
    /**
     * Save or update client data in the database.
     * Handles nullable fields appropriately.
     */
    private void saveOrUpdateClientData(String clientId, String clientName, String clientEmail, String clientPhone) {
        log.info("💾 SAVE_OR_UPDATE_CLIENT_DATA STARTED - Processing client data...");
        log.info("📝 Client data received - ID: {}, Name: {}, Email: {}, Phone: {}", clientId, clientName, clientEmail, clientPhone);
        
        if (clientId == null || clientId.trim().isEmpty()) {
            log.warn("❌ VALIDATION FAILED - Cannot save client data: clientId is null or empty");
            return;
        }
        
        try {
            log.info("🔍 CHECKING EXISTING CLIENT - Looking up client by email...");
            // Check if client already exists
            Client existingClient = clientRepository.findById(clientId).orElse(null);
            
            if (existingClient != null) {
                log.info("✅ EXISTING CLIENT FOUND - Updating client data if needed...");
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
                    log.info("💾 SAVING CLIENT UPDATES - Persisting updated client data...");
                    clientRepository.save(existingClient);
                    log.info("✅ Updated existing client data for clientId: {}", clientId);
                } else {
                    log.debug("ℹ️ No updates needed for existing client: {}", clientId);
                }
            } else {
                log.info("🆕 CREATING NEW CLIENT - No existing client found, creating new record...");
                // Create new client
                log.info("🏗️ BUILDING CLIENT OBJECT - Creating client with sanitized data...");
                Client newClient = Client.builder()
                    .clientId(clientId)
                    .name(clientName != null && !clientName.trim().isEmpty() ? clientName.trim() : "Unknown")
                    .email(clientEmail != null && !clientEmail.trim().isEmpty() ? clientEmail.trim() : "unknown@example.com")
                    .phone(clientPhone != null && !clientPhone.trim().isEmpty() ? clientPhone.trim() : "N/A")
                    .isAssigned(true)
                    .build();
                
                log.info("💾 SAVING NEW CLIENT - Persisting new client to database...");
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
        log.info("💬 SAVE_CONVERSATION_DATA STARTED - Processing conversation data...");
        log.info("📋 Conversation details - ConversationId: {}, UserId: {}, ClientId: {}", conversationId, userId, clientId);
        
        if (conversationId == null || conversationId.trim().isEmpty()) {
            log.warn("❌ VALIDATION FAILED - Cannot save conversation data: conversationId is null or empty");
            return;
        }
        
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("❌ VALIDATION FAILED - Cannot save conversation data: userId is null or empty");
            return;
        }
        
        if (clientId == null || clientId.trim().isEmpty()) {
            log.warn("❌ VALIDATION FAILED - Cannot save conversation data: clientId is null or empty");
            return;
        }
        
        try {
            log.info("🔍 CHECKING EXISTING CONVERSATION - Looking up conversation by ID...");
            // Check if conversation already exists
            Conversation existingConversation = conversationRepository.findById(conversationId).orElse(null);
            
            if (existingConversation != null) {
                log.debug("ℹ️ CONVERSATION EXISTS - Conversation {} already exists in database", conversationId);
                return;
            }
            
            log.info("🔍 FETCHING RELATED ENTITIES - Looking up client and user records...");
            // Get client and user entities
            log.info("👤 FETCHING CLIENT - Looking up client by ID: {}", clientId);
            Client client = clientRepository.findById(clientId).orElse(null);
            if (client == null) {
                log.error("❌ CLIENT NOT FOUND - Client not found with id: {} when saving conversation", clientId);
                return;
            }
            
            log.info("👥 FETCHING USER - Looking up user account by ID: {}", userId);
            UserAccount userAccount = userRepository.findById(Long.parseLong(userId)).orElse(null);
            if (userAccount == null) {
                log.error("❌ USER NOT FOUND - User not found with id: {} when saving conversation", userId);
                return;
            }
            
            log.info("🏗️ CREATING CONVERSATION OBJECT - Building new conversation record...");
            // Create new conversation with only essential fields
            Conversation conversation = new Conversation();
            conversation.setConversationId(conversationId);
            conversation.setClient(client);
            conversation.setUserAccount(userAccount);
            
            log.info("💾 SAVING CONVERSATION - Persisting conversation to database...");
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
    
    /**
     * Efficiently find user for tenant without blocking other tenant requests
     * Uses pre-computed tenant pools for O(1) lookup
     */
    private ChatUser findUserForTenantEfficiently(String tenantId) {
        log.info("🔍 FINDING USER FOR TENANT - Tenant: {}", tenantId);
        
        if (tenantId == null || tenantId.trim().isEmpty()) {
            log.warn("⚠️ Invalid tenant ID provided: '{}'", tenantId);
            return null;
        }
        
        log.info("🏢 Looking up tenant user pool...");
        Set<String> tenantUsers = tenantUserPools.get(tenantId);
        log.info("📊 Tenant pool status - Tenant: {}, Pool size: {}", tenantId, tenantUsers != null ? tenantUsers.size() : 0);
        
        if (tenantUsers == null || tenantUsers.isEmpty()) {
            log.info("📭 No users found in tenant pool for: {}", tenantId);
            return null;
        }
        
        log.info("🔍 Scanning tenant users for availability...");
        // Find first available user in this tenant with capacity
        for (String userId : tenantUsers) {
            log.debug("⚡ Checking user capacity - User: {}", userId);
            AtomicInteger userCount = userClientCounts.get(userId);
            int currentCount = userCount != null ? userCount.get() : 0;
            
            log.debug("📊 User {} current clients: {}/{}", userId, currentCount, MAX_CLIENTS_PER_USER);
            
            if (currentCount < MAX_CLIENTS_PER_USER) {
                log.info("✅ Available user found - User: {}, Current clients: {}/{}", userId, currentCount, MAX_CLIENTS_PER_USER);
                ChatUser user = userMap.get(userId);
                if (user != null) {
                    log.info("🎯 Returning user for assignment: {}", userId);
                    return user;
                } else {
                    log.warn("⚠️ User object not found in userMap for userId: {}", userId);
                }
            }
        }
        
        log.info("❌ No available users in tenant pool for: {}", tenantId);
        return null;
    }
    
    /**
     * Add user to tenant pools when they come online
     * Called during ping processing
     */
    private void addUserToTenantPools(String userId) {
        log.info("🏢 ADDING USER TO TENANT POOLS - User: {}", userId);
        try {
            log.info("💾 Fetching user account from database...");
            Optional<UserAccount> userAccountOpt = userRepository.findById(Long.parseLong(userId));
            
            if (userAccountOpt.isPresent()) {
                log.info("✅ User account found for userId: {}", userId);
                UserAccount userAccount = userAccountOpt.get();
                
                log.info("🔍 Looking up user organization permissions...");
                Set<UserOrgPermissions> permissions = userOrgPermissionsRepository.findByUser(userAccount);
                log.info("📊 Organization permissions found: {}", permissions.size());
                
                if (permissions.isEmpty()) {
                    log.warn("⚠️ No organization permissions found for user: {}", userId);
                    return;
                }
                
                log.info("🏢 Processing tenant associations...");
                for (UserOrgPermissions permission : permissions) {
                    Organization org = permission.getOrganization();
                    if (org != null && org.getTenantId() != null) {
                        String tenantId = org.getTenantId();
                        log.info("🔗 Adding user {} to tenant pool: {}", userId, tenantId);
                        
                        tenantUserPools.computeIfAbsent(tenantId, k -> ConcurrentHashMap.newKeySet()).add(userId);
                        log.info("✅ User added to tenant pool - Tenant: {}, Pool size now: {}", 
                                tenantId, tenantUserPools.get(tenantId).size());
                    } else {
                        log.warn("⚠️ Organization or tenant ID is null for permission: {}", permission.getId());
                    }
                }
                
                log.info("📊 Initializing client count tracking for user: {}", userId);
                // Initialize client count tracking
                userClientCounts.put(userId, new AtomicInteger(0));
                log.info("✅ Client count tracking initialized for user: {}", userId);
                
            } else {
                log.warn("❌ User account not found for userId: {}", userId);
            }
        } catch (NumberFormatException e) {
            log.error("❌ Invalid user ID format: {}", userId);
        } catch (Exception e) {
            log.error("❌ Error adding user to tenant pools: {}", e.getMessage(), e);
        }
        log.info("✅ TENANT POOL ADDITION COMPLETED for user: {}", userId);
    }
    
    /**
     * Remove user from all tenant pools when they go offline
     */
    private void removeUserFromTenantPools(String userId) {
        tenantUserPools.values().forEach(pool -> pool.remove(userId));
        userClientCounts.remove(userId);
        log.debug("Removed user {} from all tenant pools", userId);
    }
    
    /**
     * Update user client count atomically
     */
    private void incrementUserClientCount(String userId) {
        log.debug("📈 Incrementing client count for user: {}", userId);
        AtomicInteger count = userClientCounts.computeIfAbsent(userId, k -> new AtomicInteger(0));
        int newCount = count.incrementAndGet();
        log.debug("✅ User {} client count incremented to: {}", userId, newCount);
    }
    
    /**
     * Decrement user client count atomically
     */
    private void decrementUserClientCount(String userId) {
        log.debug("📉 Decrementing client count for user: {}", userId);
        AtomicInteger count = userClientCounts.get(userId);
        if (count != null) {
            int newCount = count.decrementAndGet();
            log.debug("✅ User {} client count decremented to: {}", userId, newCount);
            if (newCount < 0) {
                log.warn("⚠️ Negative client count detected for user {}, resetting to 0", userId);
                count.set(0);
            }
        } else {
            log.warn("⚠️ No client count tracking found for user: {}", userId);
        }
    }
} 