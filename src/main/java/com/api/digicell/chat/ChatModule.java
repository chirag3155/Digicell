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
import com.api.digicell.services.ZendeskService;
import com.api.digicell.services.RedisUserService;
import com.corundumstudio.socketio.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
import java.util.stream.Collectors;
import java.util.function.Function;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class ChatModule {
    private final SocketIOServer server;
    // private static Map<String, ChatUser> userMap; // COMMENTED OUT - Using Redis instead
    // private final Map<String, ChatRoom> chatRooms; // COMMENTED OUT - Using Redis instead
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
    private final ZendeskService zendeskService;
    private final RedisUserService redisUserService;
    
    /*
     * NEW BUSINESS LOGIC RULES:
     * 1. ONE USER = ONE SOCKET ID (enforced in SocketConnectionService)
     * 2. ONE USER = MAX 5 CONCURRENT CLIENT CONVERSATIONS (enforced here)
     * 3. If user connects with new socket, old socket is replaced
     * 4. If user at capacity (5 clients), new client assignment is rejected
     */

    // SSL Configuration properties
    @Value("${socket.ssl.enabled:false}")
    private boolean sslEnabled;
    
    @Value("${socket.ssl.key-store:}")
    private String keyStorePath;
    
    @Value("${socket.ssl.key-store-password:}")
    private String keyStorePassword;

    // User ping timeout configuration
    @Value("${socket.user.ping.timeout:10000}")
    private long userPingTimeoutMs;

    // COMMENTED OUT - Using Redis instead
    // private final Map<String, Set<String>> tenantUserPools = new ConcurrentHashMap<>();
    // private final Map<String, AtomicInteger> userClientCounts = new ConcurrentHashMap<>();

    public ChatModule(UserAccountService userAccountService, SocketConfig socketConfig, SocketConnectionService connectionService, 
                     ClientRepository clientRepository, ConversationRepository conversationRepository, UserRepository userRepository, Environment environment, UserOrgPermissionsRepository userOrgPermissionsRepository, TaskScheduler taskScheduler, ZendeskService zendeskService, RedisUserService redisUserService) {
        
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
                    
                    // Configure secure SSL/TLS protocols and cipher suites
                    // Support TLS 1.2 and 1.3 for better compatibility while maintaining security
                    config.setSSLProtocol("TLS");  // Allow negotiation of best available TLS version
                    
                    // Add custom exception handler for SSL issues
                    log.info("🔒 SSL Configuration Details: Primary Protocol: TLS (auto-negotiation),Supported: TLSv1.2, TLSv1.3, Deprecated protocols (TLSv1.0, TLSv1.1) are disabled for security ");
                
                    
                    log.info("SSL keystore and password configured successfully");
                    log.info("SSL configured successfully for Socket.IO server on port {} with TLS auto-negotiation", socketConfig.getPort());
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
        
        log.info("🗂️ INITIALIZING DATA STRUCTURES - Using Redis instead of in-memory maps...");

        // this.userMap = new ConcurrentHashMap<>(); // COMMENTED OUT - Using Redis instead
        // this.chatRooms = new ConcurrentHashMap<>(); // COMMENTED OUT - Using Redis instead
        
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
        this.zendeskService = zendeskService;
        this.redisUserService = redisUserService;

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
                log.info("❓ No user found for disconnecting socket: {}. This is expected if the user reconnected with a new socket.", socketId);
            }
            
            log.info("🔄 Delegating to SocketConnectionService for connection cleanup...");
            connectionService.removeConnection(socketId);
            log.info("✅ Disconnect handling completed for socket: {}", socketId);
        });

        server.addEventListener(socketConfig.EVENT_AGENT_REQUEST, Map.class, (socketClient, data, ackSender) -> {
            log.info("🎯 --------->  EVENT_AGENT_REQUEST RECEIVED - Starting request processing...");
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
                log.info("🔍 Client info from chat module: {}", history, summary, timestamp, clientName, clientEmail, clientPhone, clientLabel, tenantId);
                // log.info("📈 Current system status - Active Rooms: {}, Total Online Users: {}", chatRooms.size(), userMap.size()); // COMMENTED OUT - Using Redis instead
                try {
                    int totalOnlineUsers = redisUserService.getAllUserIds().size();
                    int totalActiveRooms = redisUserService.getAllChatRoomIds().size();
                    log.info("📈 Current system status - Active Rooms: {}, Total Online Users: {}", totalActiveRooms, totalOnlineUsers);
                } catch (Exception e) {
                    log.warn("⚠️ Could not get total online users or rooms from Redis: {}", e.getMessage());
                    log.info("📈 Current system status - Redis unavailable");
                }
                
                log.info("🔄 Delegating to handleUserRequest for conversation assignment...");
                handleUserRequest(socketClient, clientId, conversationId, summary, history, timestamp, clientName, clientEmail, clientPhone, clientLabel,tenantId);
                log.info("✅ EVENT_AGENT_REQUEST processing completed");
            } catch (Exception e) {
                log.error("❌ Error in EVENT_AGENT_REQUEST processing: {}", e.getMessage(), e);
            }
        });

        server.addEventListener(socketConfig.EVENT_MESSAGE_REQ, ChatMessageRequest.class, (socketClient, messageRequest, ackSender) -> {
            log.info("💬 --------->  EVENT_MESSAGE_REQ RECEIVED - Starting message processing...");
            String conversationId = messageRequest.getConversationId();
        
            log.info("📝 Message request details - Conversation: {}, Client: {}, Content length: {}, Timestamp: {}", 
                    conversationId,
                    messageRequest.getUserId(), 
                    messageRequest.getTranscript() != null ? messageRequest.getTranscript().length() : 0,
                    messageRequest.getTimestamp());
            
            log.info("🔍 Looking up chat room for conversation: {}", conversationId);
            // Store the message
            // ChatRoom chatRoom = chatRooms.get(conversationId); // COMMENTED OUT - Using Redis instead
            ChatRoom chatRoom = null;
            try {
                chatRoom = redisUserService.getChatRoom(conversationId);
            } catch (Exception e) {
                log.warn("⚠️ Could not get chat room {} from Redis: {}", conversationId, e.getMessage());
            }
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
                
                // Update chat room in Redis
                try {
                    redisUserService.updateChatRoom(chatRoom);
                    log.debug("✅ Updated chat room {} in Redis", conversationId);
                } catch (Exception e) {
                    log.warn("⚠️ Could not update chat room {} in Redis: {}", conversationId, e.getMessage());
                }
                
                log.info("🔍 Looking up user socket for message forwarding - UserId: {}", chatRoom.getUserId());
                // Get the user socket ID and send directly to the user
                // String userSocketId = connectionService.getUserSocketId(chatRoom.getUserId()); // COMMENTED OUT - Using Redis instead
                String userSocketId = null;
                try {
                    userSocketId = redisUserService.getUserSocket(chatRoom.getUserId());
                } catch (Exception e) {
                    log.warn("⚠️ Could not get socket for user {} from Redis: {}", chatRoom.getUserId(), e.getMessage());
                }
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
            
            log.info(" --------->  EVENT_MESSAGE_RESP_AGENT - User Response Details - Client: {}, Message: {}, Timestamp: {}", 
                    userResponse.getClientId(),
                    userResponse.getMessage() != null ? userResponse.getMessage().substring(0, Math.min(100, userResponse.getMessage().length())) + "..." : "null",
                    userResponse.getTimestamp());
            
            // Log active chat rooms from Redis
            try {
                Set<String> roomIds = redisUserService.getAllChatRoomIds();
                log.info("Active Chat Rooms ({} total):", roomIds.size());
                for (String roomId : roomIds) {
                    try {
                        ChatRoom room = redisUserService.getChatRoom(roomId);
                        if (room != null) {
                            log.info("   Room: {} -> User: {}, Client: {}, Active: {}", roomId, room.getUserId(), room.getClientId(), room.isActive());
                        }
                    } catch (Exception e) {
                        log.warn("   Could not get details for room {}: {}", roomId, e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.warn("⚠️ Could not get chat room list from Redis: {}", e.getMessage());
            }

            // Store the message
            // ChatRoom chatRoom = chatRooms.get(conversationId); // COMMENTED OUT - Using Redis instead
            ChatRoom chatRoom = null;
            try {
                chatRoom = redisUserService.getChatRoom(conversationId);
            } catch (Exception e) {
                log.warn("⚠️ Could not get chat room {} from Redis: {}", conversationId, e.getMessage());
            }
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
                
                // Update chat room in Redis
                try {
                    redisUserService.updateChatRoom(chatRoom);
                    log.debug("✅ Updated chat room {} in Redis after user response", conversationId);
                } catch (Exception e) {
                    log.warn("⚠️ Could not update chat room {} in Redis: {}", conversationId, e.getMessage());
                }
                
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
            
            log.info(" --------->  EVENT_CLOSE_AGENT - User: {}, Conversation: {}, Client: {}", userId, conversationId, clientId);
            
            // Find the chat room
            ChatRoom chatRoom = findChatRoomByConversationId(conversationId);
            if (chatRoom != null) {
                log.info("✅ Chat room found for close request - Room User: {}, Room Client: {}", chatRoom.getUserId(), chatRoom.getClientId());
                
                // Get the user
                // ChatUser user = userMap.get(userId); // COMMENTED OUT - Using Redis instead
                ChatUser user = null;
                try {
                    user = redisUserService.getUser(userId);
                } catch (Exception e) {
                    log.warn("⚠️ Could not get user {} from Redis: {}", userId, e.getMessage());
                }
                
                if (user != null) {
                    log.info("👤 User found - Current client count: {}", user.getCurrentClientCount());
                    
                    // Remove conversation from user's active conversations
                    user.removeConversation(conversationId);
                    user.setCurrentClientCount(user.getCurrentClientCount() - 1);
                    
                    // Update atomic client count
                    decrementUserClientCount(userId);
                    
                    // Remove the chat room
                    // chatRooms.remove(conversationId); // COMMENTED OUT - Using Redis instead
                    try {
                        redisUserService.deleteChatRoom(conversationId);
                        log.debug("✅ Deleted chat room {} from Redis", conversationId);
                    } catch (Exception e) {
                        log.warn("⚠️ Could not delete chat room {} from Redis: {}", conversationId, e.getMessage());
                    }
                    
                    // Remove conversation from tracking
                    connectionService.removeUserConversation(userId, conversationId);

                    // Update user in Redis
                    try {
                        redisUserService.updateUser(user);
                        log.debug("✅ Updated user {} in Redis after client removal", userId);
                    } catch (Exception e) {
                        log.warn("⚠️ Could not update user {} in Redis: {}", userId, e.getMessage());
                    }

                    log.info("Chat room removed - User {} now has {} active clients", userId, user.getCurrentClientCount());
                } else {
                    // log.warn("User {} not found in userMap", userId); // COMMENTED OUT - Using Redis instead
                    log.warn("User {} not found in Redis", userId);
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
            log.info(" --------->  EVENT_CLIENT_CLOSE - Client close event received");
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
                // ChatUser user = userMap.get(userId); // COMMENTED OUT - Using Redis instead
                ChatUser user = null;
                try {
                    user = redisUserService.getUser(userId);
                } catch (Exception e) {
                    log.warn("⚠️ Could not get user {} from Redis: {}", userId, e.getMessage());
                }
                
                if (user != null) {
                    // Remove conversation from user's active conversations
                    user.removeConversation(conversationId);
                    user.setCurrentClientCount(user.getCurrentClientCount() - 1);
                    
                    // Update atomic client count
                    decrementUserClientCount(userId);
                    
                    // Remove the chat room
                    // chatRooms.remove(conversationId); // COMMENTED OUT - Using Redis instead
                    try {
                        redisUserService.deleteChatRoom(conversationId);
                        log.debug("✅ Deleted chat room {} from Redis", conversationId);
                    } catch (Exception e) {
                        log.warn("⚠️ Could not delete chat room {} from Redis: {}", conversationId, e.getMessage());
                    }
                    
                    // Remove conversation from tracking
                    connectionService.removeUserConversation(userId, conversationId);

                    log.info("Chat conversation {} removed as client {} closes", conversationId, clientId);

                    // Update user in Redis
                    try {
                        redisUserService.updateUser(user);
                        log.debug("✅ Updated user {} in Redis after client close", userId);
                    } catch (Exception e) {
                        log.warn("⚠️ Could not update user {} in Redis: {}", userId, e.getMessage());
                    }

                    log.info("user.getCurrentClientCount() {}: {}", user.getCurrentClientCount());
                    // If this was the last client, clean up the room
                    // String userSocketId = connectionService.getUserSocketId(userId); // COMMENTED OUT - Using Redis instead
                    String userSocketId = null;
                    try {
                        userSocketId = redisUserService.getUserSocket(userId);
                    } catch (Exception e) {
                        log.warn("⚠️ Could not get socket for user {} from Redis: {}", userId, e.getMessage());
                    }
                    
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
            log.info("🏓 --------->  EVENT_PING RECEIVED - Starting ping processing...");
            try {
                String userId = pingRequest.getUserId();
                String socketId = socketClient.getSessionId().toString();
                log.info("📍 Ping details - UserId: '{}', SocketId: '{}'", userId, socketId);
                log.info("📍 Ping request object - Class: {}, UserId from object: '{}'", 
                        pingRequest.getClass().getSimpleName(), pingRequest.getUserId());
                
                // Debug socket client details
                log.info(" 🔌 Socket client details:  Remote address: {}, Session ID: {}, connected: {}, handshake params: {}", socketClient.getRemoteAddress(), socketClient.getSessionId(), socketClient.isChannelOpen(), socketClient.getHandshakeData().getUrlParams());
              
                
                
                
                log.info("📋 NEW LOGIC: User {} verified with ONE socket ID (max {} clients allowed)", userId, MAX_CLIENTS_PER_USER);
                
                log.info("🔄 Checking if user reconnected with preserved conversations...");
                // Check if this user reconnected with preserved conversations
                boolean hadPreservedConversations = connectionService.checkAndClearUserReconnectedFlag(userId);
                if (hadPreservedConversations) {
                    log.info("🔄 User {} reconnected with preserved conversations - triggering restoration", userId);
                    restoreUserConversations(userId, socketClient);
                    sendUserReconnectionNotifications(userId);
                } else {
                    log.debug("ℹ️ User {} has no preserved conversations to restore", userId);
                }

                // log.info("🔍 Checking if user exists in userMap..."); // COMMENTED OUT - Using Redis instead
                log.info("🔍 Checking if user exists in Redis...");
                // Check if user is already in Redis
                // ChatUser user = userMap.get(userId); // COMMENTED OUT - Using Redis instead
                ChatUser user = null;
                try {
                    // ✅ DEBUG: Enhanced Redis debugging
                    log.info("🔍 DEBUG: Checking Redis for user {} with detailed analysis...", userId);
                    
                    // Check if key exists first
                    boolean keyExists = redisUserService.userKeyExists(userId);
                    log.info("🔍 DEBUG: Redis key exists: {}", keyExists);
                    
                    if (keyExists) {
                        // Get raw data to see what's actually stored
                        Object rawData = redisUserService.getRawUserData(userId);
                        log.info("🔍 DEBUG: Raw data type: {}", rawData != null ? rawData.getClass().getSimpleName() : "null");
                    }
                    
                    // Try to get user normally
                    user = redisUserService.getUser(userId);
                    log.info("🔍 DEBUG: getUser() result: {}", user != null ? "ChatUser object found" : "null");
                    
                } catch (Exception e) {
                    log.warn("⚠️ Could not get user {} from Redis: {}", userId, e.getMessage());
                }

                if (user == null) {
                    // ⚠️ RECOVERY MODE: User not found in Redis - this should be rare
                    // Normal flow: go_online creates user, ping maintains user
                    // This case handles: server restart, Redis flush, network issues
                    log.warn("🚨 RECOVERY MODE: User {} not found in Redis during ping - this indicates potential issue", userId);

                    // Create new user as recovery fallback
                    user = new ChatUser(userId);
                    user.setOfflineRequested(false);  // Set as online
                    user.updatePingTime();
                    
                    // ✅ RECOVERY: Fetch user info from database since go_online data is missing
                    try {
                        Long userIdLong = Long.parseLong(userId);
                        UserAccount userAccount = userRepository.findById(userIdLong).orElse(null);
                        if (userAccount != null) {
                            // Populate fields from database as recovery
                            user.setEmail(userAccount.getEmail());
                            user.setUserName(userAccount.getUserName());
                            user.setUserLabel(userAccount.getPhoneNumber()); // Use phone as label for now
                            log.info("✅ RECOVERY: Enhanced user {} with database info - Email: {}, UserName: {}, Phone: {}", 
                                    userId, userAccount.getEmail(), userAccount.getUserName(), userAccount.getPhoneNumber());
                        } else {
                            log.error("❌ RECOVERY FAILED: User {} not found in database - orphaned user session", userId);
                        }
                    } catch (NumberFormatException e) {
                        log.error("❌ RECOVERY FAILED: Invalid user ID format for database lookup: {}", userId);
                    } catch (Exception e) {
                        log.error("❌ RECOVERY FAILED: Could not fetch user info from database: {}", e.getMessage());
                    }
                    
                    // Store new user in Redis
                    try {
                        redisUserService.addUser(user);
                        log.info("✅ RECOVERY: User {} added to Redis during ping recovery", userId);
                    } catch (Exception redisError) {
                        log.warn("⚠️ RECOVERY: Failed to add user to Redis during ping recovery: {}", redisError.getMessage());
                    }
                    
                    // Update user status in database
                    try {
                    
                        Long userIdLong = Long.parseLong(userId);
                        userAccountService.setUserONLINE(userIdLong);
                        log.info("✅ RECOVERY: User {} status set to ONLINE in database", userId);
                    } catch (NumberFormatException e) {
                        log.error("❌ RECOVERY: Invalid user ID format for database update: {}", userId);
                    } catch (Exception e) {
                        log.warn("⚠️ RECOVERY: Could not update user status in database: {}", e.getMessage());
                    }
                    
                    log.info("🔄 RECOVERY: Created fallback user object - UserId: {}, Email: {}, UserName: {}", 
                            user.getUserId(), user.getEmail(), user.getUserName());
                } else {
                    log.debug("🔄 Existing user found, updating ping time...");
                    // User already exists, just update ping time
                    user.updatePingTime();
                }

                // ✅ Common operations for both new and existing users
                // Check and populate missing user info from database if needed
                if (user.getEmail() == null || user.getUserName() == null || user.getUserLabel() == null) {
                    try {
                        Long userIdLong = Long.parseLong(userId);
                        UserAccount userAccount = userRepository.findById(userIdLong).orElse(null);
                        if (userAccount != null) {
                            // Update missing fields from database
                            if (user.getEmail() == null) {
                                user.setEmail(userAccount.getEmail());
                            }
                            if (user.getUserName() == null) {
                                user.setUserName(userAccount.getUserName());
                            }
                            if (user.getUserLabel() == null) {
                                user.setUserLabel(userAccount.getPhoneNumber()); // Use phone as label
                            }
                            log.info("✅ Updated missing user {} info from database - Email: {}, UserName: {}, Phone: {}", 
                                    userId, user.getEmail(), user.getUserName(), user.getUserLabel());
                        } else {
                            log.warn("⚠️ User {} not found in database for info update", userId);
                        }
                    } catch (NumberFormatException e) {
                        log.warn("⚠️ Invalid user ID format for database info update: {}", userId);
                    } catch (Exception e) {
                        log.warn("⚠️ Could not fetch missing user info from database: {}", e.getMessage());
                    }
                }
                
                addUserToTenantPools(userId);
                log.debug("✅ Ping processed for user: {}, email: {}, userName: {}, last ping time: {}", 
                        user.getUserId(), user.getEmail(), user.getUserName(), user.getLastPingTime());
                
                // REDIS IMPLEMENTATION - Update user ping time in Redis
                try {
                    redisUserService.updateUser(user);
                    log.debug("✅ REDIS: User {} ping time updated in Redis", userId);
                } catch (Exception redisError) {
                    log.warn("⚠️ REDIS: Failed to update user ping time in Redis: {}", redisError.getMessage());
                    // Continue with in-memory operation
                }

        
            // Send pong response
            socketClient.sendEvent(SocketConfig.EVENT_PONG, "pong");
            log.info("✅ PONG sent to user: {}", userId);
        
            } catch (Exception e) {
                log.error("❌ Error in EVENT_PING processing: {}", e.getMessage(), e);
            }
        });

        server.addEventListener("go_online", Map.class, (socketClient, data, ackSender) -> {
            log.info("👋 --------->  EVENT_GO_ONLINE RECEIVED - Starting online request processing...");
            try {
                if (data == null) {
                    log.error("❌ GO_ONLINE VALIDATION FAILED - Received null data object");
                    return;
                }
        
                Object userIdObj = data.get("user_id");
                String email = (String) data.get("email");
                String ipAddress = (String) data.get("ip_address");
        
                if (userIdObj == null) {
                    log.error("❌ GO_ONLINE VALIDATION FAILED - Missing user_id");
                    return;
                }
        
                String userId = String.valueOf(userIdObj);
                log.info("👤 Go online request details - UserId: {}, Email: {}, IP: {}", userId, email, ipAddress);
        
                // REDIS LOGIC - Get user from Redis
                // ChatUser user = userMap.get(userId); // COMMENTED OUT - Using Redis instead
                ChatUser user = null;
                try {
                    user = redisUserService.getUser(userId);
                } catch (Exception e) {
                    log.warn("⚠️ Could not get user {} from Redis: {}", userId, e.getMessage());
                }
                
                if (user == null) {
                    log.info("👤 New user detected for go_online, creating user object...");
                    user = new ChatUser(userId);
                    // Store new user in Redis
                    try {
                        redisUserService.addUser(user);
                        log.info("✅ REDIS: New user {} added to Redis during go_online", userId);
                    } catch (Exception redisError) {
                        log.warn("⚠️ REDIS: Failed to add new user to Redis during go_online: {}", redisError.getMessage());
                    }
                }
        
                // Store new info
                user.setEmail(email);
                user.setIpAddress(ipAddress);
                user.setOfflineRequested(false); // Set as online
                user.updatePingTime(); // Also update ping time as this is an activity
        
                log.info("✅ User {} details updated: email={}, ip={}", userId, user.getEmail(), user.getIpAddress());
        
                // REDIS IMPLEMENTATION - Store user data in Redis
                log.info("💾 REDIS: Storing user data in Redis...");
                try {
                    if (user != null) {
                        // We already have the user object, just store/update it in Redis
                        redisUserService.updateUser(user);
                        log.info("✅ REDIS: User {} data stored/updated in Redis", userId);
                    }
                } catch (Exception redisError) {
                    log.warn("⚠️ REDIS: Failed to store user data in Redis: {}", redisError.getMessage());
                    // Continue with in-memory operation as fallback
                }
        
                // Add user to tenant pools
                addUserToTenantPools(userId);
        
                // Update user status to ONLINE in database
                try {
                    log.info("💾 Updating user status to ONLINE in database...");
                    Long userIdLong = Long.parseLong(userId);
                    userAccountService.setUserONLINE(userIdLong);
                    log.info("✅ User {} status set to ONLINE in database", userId);
                } catch (NumberFormatException e) {
                    log.error("❌ Invalid user ID format for database update: {}", userId);
                } catch (Exception e) {
                    log.error("❌ Error setting user ONLINE: {}", e.getMessage(), e);
                }
        
                // ✅ NEW: Fetch additional user info from database to enrich ChatUser
                try {
                    Long userIdLong = Long.parseLong(userId);
                    UserAccount userAccount = userRepository.findById(userIdLong).orElse(null);
                    if (userAccount != null) {
                        // Populate additional fields from database
                        user.setUserName(userAccount.getUserName());
                        user.setUserLabel(userAccount.getPhoneNumber()); // Use phone as label for now
                        
                    } else {
                        log.warn("⚠️ User {} not found in database, using basic info only", userId);
                    }
                } catch (NumberFormatException e) {
                    log.warn("⚠️ Invalid user ID format for database lookup: {}", userId);
                } catch (Exception e) {
                    log.warn("⚠️ Could not fetch user info from database: {}", e.getMessage());
                }

                log.info("✅ User {} details updated in GO_ONLINE: email={}, ip={}, userName={}, userLabel={}", 
                        userId, user.getEmail(), user.getIpAddress(), user.getUserName(), user.getUserLabel());
            } catch (Exception e) {
                log.error("❌ Error in EVENT_GO_ONLINE processing: {}", e.getMessage(), e);
            }
        });

        server.addEventListener(socketConfig.EVENT_OFFLINE_REQ, UserPingRequest.class, (socketClient, offlineRequest, ackSender) -> {
            log.info("📴 --------->  EVENT_OFFLINE_REQ RECEIVED - Starting offline request processing...");
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
        log.info("🎯 HANDLE_USER_REQUEST STARTED ----> Request details - Conversation: {}, Client: {}, Tenant: {}", conversationId, clientId, tenantId);
        log.info("👤 Customer info - Name: {}, Email: {}, Phone: {}", clientName, clientEmail, clientPhone);
        
        log.info("🔍 Checking for duplicate conversation ID...");
        // Check if this conversation ID already exists
        // if (chatRooms.containsKey(conversationId)) { // COMMENTED OUT - Using Redis instead
        ChatRoom existingRoom = null;
        try {
            existingRoom = redisUserService.getChatRoom(conversationId);
        } catch (Exception e) {
            log.warn("⚠️ Could not check for existing room {} in Redis: {}", conversationId, e.getMessage());
        }
        
        if (existingRoom != null) {
            log.warn("⚠️ DUPLICATE CONVERSATION DETECTED - Conversation {} already exists, rejecting request", conversationId);
            // ChatRoom existingRoom = chatRooms.get(conversationId);
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
        
        // log.info("🔍 Looking for available user - Current users: {}, Max clients per user: {}", userMap.size(), MAX_CLIENTS_PER_USER); // COMMENTED OUT - Using Redis instead
        try {
            int totalOnlineUsers = redisUserService.getAllUserIds().size();
            log.info("🔍 Looking for available user - Current users: {}, Max clients per user: {}", totalOnlineUsers, MAX_CLIENTS_PER_USER);
        } catch (Exception e) {
            log.warn("⚠️ Could not get user count from Redis: {}", e.getMessage());
            log.info("🔍 Looking for available user - Max clients per user: {}", MAX_CLIENTS_PER_USER);
        }
        
        // Debug: Log current system state
        // log.info("🔍 SYSTEM DEBUG - Current online users: {}", userMap.keySet()); // COMMENTED OUT - Using Redis instead
        try {
            Set<String> onlineUserIds = redisUserService.getAllUserIds();
            log.info("🔍 SYSTEM DEBUG - Current online users: {}", onlineUserIds);
        } catch (Exception e) {
            log.warn("⚠️ Could not get online users from Redis: {}", e.getMessage());
        }
        // log.info("🏢 TENANT POOLS DEBUG - Available tenant pools: {}", tenantUserPools.keySet()); // COMMENTED OUT - Using Redis instead
        // tenantUserPools.forEach((tenant, users) -> {
        //     log.info("   Tenant '{}' has {} users: {}", tenant, users.size(), users);
        // });
        try {
            Set<String> tenantIds = redisUserService.getAllTenantIds();
            log.info("🏢 TENANT POOLS DEBUG - Available tenant pools: {}", tenantIds);
            for (String currentTenantId : tenantIds) {
                try {
                    Set<String> users = redisUserService.getTenantPoolUsers(currentTenantId);
                    log.info("   Tenant '{}' has {} users: {}", currentTenantId, users.size(), users);
                } catch (Exception e) {
                    log.warn("   Could not get users for tenant {}: {}", currentTenantId, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ Could not get tenant pools from Redis: {}", e.getMessage());
        }
        
        log.info("🎯 Starting efficient tenant-aware user lookup...");
        // Use efficient tenant-aware assignment instead of blocking queue processing
        ChatUser user = findUserForTenantEfficiently(tenantId);
        
        // NEW LOGIC: One user = One socket, Max 5 clients per user
        if (user != null) {
            int currentClientCount = user.getCurrentClientCount();
            log.info("🔍 USER CAPACITY CHECK - User: {}, Current clients: {}/{}", 
                    user.getUserId(), currentClientCount, MAX_CLIENTS_PER_USER);
            
            if (currentClientCount >= MAX_CLIENTS_PER_USER) {
                log.warn("❌ USER AT CAPACITY - User {} has {}/{} clients, cannot assign new client {}", 
                        user.getUserId(), currentClientCount, MAX_CLIENTS_PER_USER, clientId);
                        
                // Send capacity exceeded response
                ClientInfoResponse userInfo = new ClientInfoResponse();
                userInfo.setStatus("user_at_capacity");
                userInfo.setUserId("");
                userInfo.setUserName("");
                userInfo.setConversationId(conversationId);
                userInfo.setClientName("");
                userInfo.setClientLabel("");
                
                log.info("📤 Sending capacity exceeded response to chat module for client: {}", clientId);
                socketClient.sendEvent(socketConfig.EVENT_AGENT_ACK, userInfo);
                log.info("✅ Capacity exceeded response sent to chat module");
                return;
            }
            
            log.info("✅ USER HAS CAPACITY - User {} can accept new client ({}/{} slots used)", 
                    user.getUserId(), currentClientCount, MAX_CLIENTS_PER_USER);
            log.info("✅ Available user found for assignment");
            log.info("🎯 Assigning user {} to conversation {} (current clients: {}/{})", 
                    user.getUserId(), conversationId, user.getCurrentClientCount(), MAX_CLIENTS_PER_USER);
                    
            log.info("🏠 Creating chat room for conversation...");
            // Create and store the chat room using conversationId as the key
            ChatRoom chatRoom = new ChatRoom(conversationId, user.getUserId(), clientId, summary, history);
            // chatRooms.put(conversationId, chatRoom); // COMMENTED OUT - Using Redis instead
            try {
                redisUserService.addChatRoom(chatRoom);
                log.info("✅ Chat room created and stored in Redis - Room ID: {}", conversationId);
            } catch (Exception e) {
                log.warn("⚠️ Could not store chat room {} in Redis: {}", conversationId, e.getMessage());
            }
            
            log.info("📊 Tracking conversation for user preservation...");
            // Track this conversation for the user to preserve it during reconnections
            connectionService.addUserConversation(user.getUserId(), conversationId);
            log.info("✅ Conversation tracking added for user: {}", user.getUserId());

            log.info("📈 Updating user client counts...");
            // Add conversation to user's active conversations
            user.addConversation(conversationId);
            user.setCurrentClientCount(user.getCurrentClientCount() + 1);
            
                                // Update atomic client count for efficient tracking
                    incrementUserClientCount(user.getUserId());
                    
                    // Update user in Redis after assignment
                    try {
                        redisUserService.updateUser(user);
                        log.debug("✅ Updated user {} in Redis after client assignment", user.getUserId());
                    } catch (Exception e) {
                        log.warn("⚠️ Could not update user {} in Redis: {}", user.getUserId(), e.getMessage());
                    }
                    
                    log.info("✅ User client count updated - User: {}, New count: {}, Maximum: {}", user.getUserId(), user.getCurrentClientCount(), MAX_CLIENTS_PER_USER);
            
            log.info("🔗 Looking up user socket for room joining...");
            // Store socket ID mapping
            // String userSocketId = connectionService.getUserSocketId(user.getUserId()); // COMMENTED OUT - Using Redis instead
            String userSocketId = null;
            try {
                userSocketId = redisUserService.getUserSocket(user.getUserId());
            } catch (Exception e) {
                log.warn("⚠️ Could not get socket for user {} from Redis: {}", user.getUserId(), e.getMessage());
            }
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
                    
                    log.info("📝 Preparing user info response...");
                    // Prepare user info data
                    ClientInfoResponse userInfo = new ClientInfoResponse();
                    userInfo.setStatus("online");
                    userInfo.setUserId(user.getUserId());
                    userInfo.setUserName(user.getEmail());
                    userInfo.setConversationId(conversationId);
                    userInfo.setTenantId(tenantId);
                    userInfo.setHistory(history);
                    userInfo.setSummary(summary);
                    userInfo.setClientId(clientId);
                    userInfo.setClientName(clientName);
                    userInfo.setClientLabel(clientLabel);
                    userInfo.setClientEmail(clientEmail);
                    userInfo.setClientPhone(clientPhone);
            
                    
                
                    // Send acknowledgment to chat module, user and zendesk WRAPPER
                    log.info("📤 Sending acknowledgment to CHAT MODULE, USER and ZENDESK WRAPPER, {}", userInfo);
                    socketClient.sendEvent(socketConfig.EVENT_AGENT_ACK, userInfo);
            
                    
                
    
                    userSocketClient.sendEvent(socketConfig.EVENT_NEW_CLIENT_REQ, userInfo);
                
                     // Notify Zendesk
                    if (user.getEmail() != null) {
                        zendeskService.assignAgentToTicket(conversationId, user.getEmail(), summary, clientName, clientEmail, clientPhone)
                            .subscribe(); // Subscribe to trigger the call
                    } else {
                        log.warn("User email is null for userId: {}. Cannot notify Zendesk.", user.getUserId());
                    }

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
            // log.warn("💔 Assignment failed - Queue empty: {}, User limit reached: {}", userMap.isEmpty(), user != null ? "Yes (current: " + user.getCurrentClientCount() + ")" : "N/A"); // COMMENTED OUT - Using Redis instead
            try {
                boolean redisEmpty = redisUserService.getAllUserIds().isEmpty();
                log.warn("💔 Assignment failed - Redis empty: {}, User limit reached: {}", 
                        redisEmpty, user != null ? "Yes (current: " + user.getCurrentClientCount() + ")" : "N/A");
            } catch (Exception e) {
                log.warn("💔 Assignment failed - Could not check Redis status: {}", e.getMessage());
            }
            
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
        
        // log.info("🔍 LOOKING UP USER - Checking if user exists in userMap..."); // COMMENTED OUT - Using Redis instead
        log.info("🔍 LOOKING UP USER - Checking if user exists in Redis...");
        // ChatUser user = userMap.get(userId); // COMMENTED OUT - Using Redis instead
        ChatUser user = null;
        try {
            user = redisUserService.getUser(userId);
        } catch (Exception e) {
            log.warn("⚠️ Could not get user {} from Redis: {}", userId, e.getMessage());
        }
        
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
                
                Map<String, Object> response = Map.of(
                    "status", "offline_success",
                    "reason", "user_not_in_memory_forced_offline",
                    "message", "User status set to OFFLINE."
                );
                socketClient.sendEvent("offline_response", response);
                log.info("User free of chat, Sent offline success response to user: {}", userId);
                
                // log.info("✅ OFFLINE REQUEST COMPLETED - User {} status updated to OFFLINE and removed from tenant pools", userId);
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
            // log.warn("User {} not found in userMap. This can happen if the server restarted. Forcing user to OFFLINE.", userId); // COMMENTED OUT - Using Redis instead
            log.warn("User {} not found in Redis. This can happen if the server restarted. Forcing user to OFFLINE.", userId);
            
            // If user is not in memory, we can still process the offline request
            // to ensure the database status is correct.
            updateUserStatus(userId, UserAccountStatus.OFFLINE);

            // Send a success response
            Map<String, Object> response = Map.of(
                "status", "offline_success",
                "reason", "user_not_in_memory_forced_offline",
                "message", "User status set to OFFLINE."
            );
            socketClient.sendEvent("offline_response", response);
            log.info("Sent forced offline success response to user: {}", userId);
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
        // ChatRoom chatRoom = chatRooms.get(conversationId); // COMMENTED OUT - Using Redis instead
        try {
            ChatRoom chatRoom = redisUserService.getChatRoom(conversationId);
            return chatRoom != null ? chatRoom.getUserId() : null;
        } catch (Exception e) {
            log.warn("⚠️ Could not get chat room {} from Redis: {}", conversationId, e.getMessage());
            return null;
        }
    }

    // Helper method to find chat room by conversation ID
    private ChatRoom findChatRoomByConversationId(String conversationId) {
        // for (ChatRoom room : chatRooms.values()) { // COMMENTED OUT - Using Redis instead
        try {
            return redisUserService.getChatRoom(conversationId);
        } catch (Exception e) {
            log.warn("⚠️ Could not get chat room {} from Redis: {}", conversationId, e.getMessage());
            return null;
        }
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
     * Fixed to handle List return type from repository
     */
    private boolean checkUserOrganizationTenants(UserAccount userAccount, String targetTenantId) {
        try {
            // Repository returns List<Organization> - convert to Set to remove duplicates
            List<Organization> userOrganizationsList = userOrgPermissionsRepository.findDistinctOrganizationsByUserAccount(userAccount);
            
            if (userOrganizationsList == null || userOrganizationsList.isEmpty()) {
                log.debug("User {} has no organizations", userAccount.getUserId());
                return false;
            }
            
            // Convert to Set to ensure uniqueness (though DISTINCT should handle this)
            Set<Organization> userOrganizations = new HashSet<>(userOrganizationsList);
            
            for (Organization org : userOrganizations) {
                String orgTenantId = org.getTenantId();
                if (targetTenantId.equals(orgTenantId)) {
                    log.info("User {} belongs to organization with matching tenant {}", 
                             userAccount.getUserId(), targetTenantId);
                    return true;
                }
            }
            
            log.debug("User {} has {} organizations but none match tenant {}", 
                     userAccount.getUserId(), userOrganizations.size(), targetTenantId);
            return false;
            
        } catch (Exception e) {
            log.error("❌ Error checking user {} organization tenants: {}", userAccount.getUserId(), e.getMessage(), e);
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
            // String currentSocketId = connectionService.getUserSocketId(userId); // COMMENTED OUT - Using Redis instead
            String currentSocketId = null;
            try {
                currentSocketId = redisUserService.getUserSocket(userId);
            } catch (Exception e) {
                log.warn("⚠️ Could not get socket for user {} from Redis: {}", userId, e.getMessage());
            }
            
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
     * Debug method to log current system capacity and user status with load balancing insights
     */
    public void logSystemCapacity() {
        log.info("📊 SYSTEM CAPACITY STATUS WITH LOAD BALANCING:");
        // log.info("   Total users in system: {}", userMap.size()); // COMMENTED OUT - Using Redis instead
        try {
            int totalUsers = redisUserService.getAllUserIds().size();
            log.info("   Total users in system: {}", totalUsers);
        } catch (Exception e) {
            log.warn("   Could not get total users from Redis: {}", e.getMessage());
        }
        // log.info("   Active chat rooms: {}", chatRooms.size()); // COMMENTED OUT - Using Redis instead
        try {
            int totalActiveRooms = redisUserService.getAllChatRoomIds().size();
            log.info("   Active chat rooms: {}", totalActiveRooms);
        } catch (Exception e) {
            log.warn("   Could not get active chat rooms from Redis: {}", e.getMessage());
        }
        log.info("   Max clients per user: {}", MAX_CLIENTS_PER_USER);
        
        // if (!userMap.isEmpty()) { // COMMENTED OUT - Using Redis instead
        try {
            Set<String> userIds = redisUserService.getAllUserIds();
            if (!userIds.isEmpty()) {
            log.info("   📊 LOAD BALANCING ANALYSIS:");

            // COMMENTED OUT - Complex load balancing analysis requires refactoring for Redis
            /*
            // Calculate load distribution statistics
            Map<Integer, Long> loadDistribution = userMap.values().stream()
                .collect(Collectors.groupingBy(
                    user -> user.getCurrentClientCount(),
                    Collectors.counting()
                ));
            
            log.info("   📈 Load Distribution:");
            for (int i = 0; i <= MAX_CLIENTS_PER_USER; i++) {
                long count = loadDistribution.getOrDefault(i, 0L);
                if (count > 0) {
                    log.info("     {} clients: {} users", i, count);
                }
            }
            
            // Find users with minimum load for next assignment
            int minLoad = userMap.values().stream()
                .filter(user -> user.getCurrentClientCount() < MAX_CLIENTS_PER_USER)
                .mapToInt(ChatUser::getCurrentClientCount)
                .min()
                .orElse(-1);
            
            if (minLoad >= 0) {
                List<String> nextAssignmentCandidates = userMap.entrySet().stream()
                    .filter(entry -> entry.getValue().getCurrentClientCount() == minLoad)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
                
                log.info("   🎯 Next Assignment Candidates (load: {}): {}", minLoad, nextAssignmentCandidates);
            } else {
                log.info("   ❌ All users at capacity - no assignment candidates available");
            }
            */
            
            // REDIS-BASED LOAD BALANCING ANALYSIS - Reimplemented for Redis
            try {
                log.info("   📈 CALCULATING LOAD DISTRIBUTION FROM REDIS...");
                
                // Collect all user data efficiently
                Map<String, ChatUser> allUsers = new HashMap<>();
                for (String userId : userIds) {
                    try {
                        ChatUser user = redisUserService.getUser(userId);
                        if (user != null) {
                            allUsers.put(userId, user);
                        }
                    } catch (Exception e) {
                        log.warn("     Could not get user {} from Redis: {}", userId, e.getMessage());
                    }
                }
                
                if (!allUsers.isEmpty()) {
                    // Calculate load distribution statistics
                    Map<Integer, Long> loadDistribution = allUsers.values().stream()
                        .collect(Collectors.groupingBy(
                            user -> user.getCurrentClientCount(),
                            Collectors.counting()
                        ));
                    
                    log.info("   📈 Load Distribution:");
                    for (int i = 0; i <= MAX_CLIENTS_PER_USER; i++) {
                        long count = loadDistribution.getOrDefault(i, 0L);
                        if (count > 0) {
                            log.info("     {} clients: {} users", i, count);
                        }
                    }
                    
                    // Find users with minimum load for next assignment
                    int minLoad = allUsers.values().stream()
                        .filter(user -> user.getCurrentClientCount() < MAX_CLIENTS_PER_USER)
                        .mapToInt(ChatUser::getCurrentClientCount)
                        .min()
                        .orElse(-1);
                    
                    if (minLoad >= 0) {
                        List<String> nextAssignmentCandidates = allUsers.entrySet().stream()
                            .filter(entry -> entry.getValue().getCurrentClientCount() == minLoad)
                            .map(Map.Entry::getKey)
                            .collect(Collectors.toList());
                        
                        log.info("   🎯 Next Assignment Candidates (load: {}): {}", minLoad, nextAssignmentCandidates);
                    } else {
                        log.info("   ❌ All users at capacity - no assignment candidates available");
                    }
                }
            } catch (Exception e) {
                log.warn("   ⚠️ Error calculating load distribution from Redis: {}", e.getMessage());
            }
            
            /*
            log.info("   📋 Individual User Details:");
            userMap.forEach((userId, user) -> {
                int clientCount = user.getCurrentClientCount();
                String status = clientCount >= MAX_CLIENTS_PER_USER ? "AT CAPACITY" : "AVAILABLE";
                String loadLevel = clientCount == 0 ? "IDLE" : 
                                 clientCount <= 2 ? "LOW" : 
                                 clientCount <= 4 ? "MEDIUM" : "HIGH";
                log.info("     User {} → {}/{} clients ({}, {})", userId, clientCount, MAX_CLIENTS_PER_USER, status, loadLevel);
            });
            */
            
            // Simplified Redis-based logging
            log.info("   📋 Individual User Details (from Redis):");
            for (String userId : userIds) {
                try {
                    ChatUser user = redisUserService.getUser(userId);
                    if (user != null) {
                        int clientCount = user.getCurrentClientCount();
                        String status = clientCount >= MAX_CLIENTS_PER_USER ? "AT CAPACITY" : "AVAILABLE";
                        String loadLevel = clientCount == 0 ? "IDLE" : 
                                         clientCount <= 2 ? "LOW" : 
                                         clientCount <= 4 ? "MEDIUM" : "HIGH";
                        log.info("     User {} → {}/{} clients ({}, {})", userId, clientCount, MAX_CLIENTS_PER_USER, status, loadLevel);
                    }
                } catch (Exception e) {
                    log.warn("     Could not get details for user {}: {}", userId, e.getMessage());
                }
            }
        }
        
        // Log tenant-specific load balancing details
        log.info("   🏢 TENANT-SPECIFIC LOAD BALANCING:");
        // if (!tenantUserPools.isEmpty()) { // COMMENTED OUT - Using Redis instead
        try {
            Set<String> allTenantIds = redisUserService.getAllTenantIds();
            if (!allTenantIds.isEmpty()) {
                for (String tenantId : allTenantIds) {
                    Set<String> tenantUserIds = redisUserService.getTenantPoolUsers(tenantId);
                log.info("     Tenant '{}' has {} users:", tenantId, tenantUserIds.size());

                // COMMENTED OUT - Complex tenant load balancing requires refactoring for Redis
                /*
                // Calculate load distribution for this tenant
                Map<Integer, Long> tenantLoadDistribution = tenantUserIds.stream()
                    .filter(userId -> userMap.containsKey(userId))
                    .map(userId -> userMap.get(userId).getCurrentClientCount())
                    .collect(Collectors.groupingBy(
                        count -> count,
                        Collectors.counting()
                    ));
                
                // Find next assignment candidate for this tenant
                Optional<String> nextCandidate = tenantUserIds.stream()
                    .filter(userId -> userMap.containsKey(userId))
                    .filter(userId -> userMap.get(userId).getCurrentClientCount() < MAX_CLIENTS_PER_USER)
                    .min(Comparator.comparingInt(userId -> userMap.get(userId).getCurrentClientCount()));
                
                if (nextCandidate.isPresent()) {
                    String candidateId = nextCandidate.get();
                    int candidateLoad = userMap.get(candidateId).getCurrentClientCount();
                    log.info("       📊 Load distribution: {}", tenantLoadDistribution);
                    log.info("       🎯 Next assignment: User {} (load: {}/{})", candidateId, candidateLoad, 
                    MAX_CLIENTS_PER_USER);
                } else {
                    log.info("       ❌ All users at capacity for tenant '{}'", tenantId);
                }
                */
                
                
                // REDIS-BASED TENANT LOAD BALANCING ANALYSIS - Reimplemented for Redis
                try {
                    log.info("       📈 CALCULATING TENANT LOAD DISTRIBUTION FROM REDIS...");
                    
                    // Collect tenant user data efficiently
                    Map<String, ChatUser> tenantUsers = new HashMap<>();
                    for (String userId : tenantUserIds) {
                        try {
                            ChatUser user = redisUserService.getUser(userId);
                            if (user != null) {
                                tenantUsers.put(userId, user);
                            }
                        } catch (Exception e) {
                            log.warn("       Could not get tenant user {} from Redis: {}", userId, e.getMessage());
                        }
                    }
                    
                    if (!tenantUsers.isEmpty()) {
                        // Calculate load distribution for this tenant
                        Map<Integer, Long> tenantLoadDistribution = tenantUsers.values().stream()
                            .collect(Collectors.groupingBy(
                                user -> user.getCurrentClientCount(),
                                Collectors.counting()
                            ));
                        
                        // Find next assignment candidate for this tenant
                        Optional<String> nextCandidate = tenantUsers.entrySet().stream()
                            .filter(entry -> entry.getValue().getCurrentClientCount() < MAX_CLIENTS_PER_USER)
                            .min(Comparator.comparingInt(entry -> entry.getValue().getCurrentClientCount()))
                            .map(Map.Entry::getKey);
                        
                        log.info("       📊 Load distribution: {}", tenantLoadDistribution);
                        
                        if (nextCandidate.isPresent()) {
                            String candidateId = nextCandidate.get();
                            int candidateLoad = tenantUsers.get(candidateId).getCurrentClientCount();
                            log.info("       🎯 Next assignment: User {} (load: {}/{})", candidateId, candidateLoad, MAX_CLIENTS_PER_USER);
                        } else {
                            log.info("       ❌ All users at capacity for tenant '{}'", tenantId);
                        }
                    } else {
                        log.info("       📭 No valid users found in Redis for tenant '{}'", tenantId);
                    }
                } catch (Exception e) {
                    log.warn("       ⚠️ Error calculating tenant load distribution from Redis: {}", e.getMessage());
                }
                }
            } else {
                log.info("     No tenant pools configured");
            }
        } catch (Exception e) {
            log.warn("   Could not get tenant pools from Redis: {}", e.getMessage());
        }
        } catch (Exception e) {
            log.warn("   Could not get users from Redis for system capacity analysis: {}", e.getMessage());
        }
        
        // Also log connection status
        connectionService.logConnectionStatus();
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
                    
                    // Send notification that conversation is restored with COMPLETE details
                    ClientInfoResponse restoreInfo = new ClientInfoResponse();
                    restoreInfo.setStatus("restored");
                    restoreInfo.setUserId(userId);
                    restoreInfo.setConversationId(conversationId);
                    
                    // Find the chat room to get client details
                    // ChatRoom chatRoom = chatRooms.get(conversationId); // COMMENTED OUT - Using Redis instead
                    // if (chatRoom != null) {
                    //     restoreInfo.setClientName("Conversation Restored");
                    //     restoreInfo.setClientLabel("active");
                    //     userSocketClient.sendEvent(socketConfig.EVENT_NEW_CLIENT_REQ, restoreInfo);
                    //     log.info("📤 Sent conversation restore notification to user {} for conversation {}", 
                    //             userId, conversationId);
                    // }
                    // Get complete conversation details from Redis
                    try {
                        ChatRoom chatRoom = redisUserService.getChatRoom(conversationId);
                        if (chatRoom != null) {
                            // Set complete conversation details for proper restoration
                            // restoreInfo.setClientId(chatRoom.getClientId());
                            // restoreInfo.setSummary(chatRoom.getSummary());
                            // restoreInfo.setHistory(chatRoom.getHistory());
                            
                            // Get user details for complete restoration
                            // ChatUser user = redisUserService.getUser(userId);
                            // if (user != null) {
                            //     restoreInfo.setUserName(user.getEmail());
                            //     restoreInfo.setTenantId(user.getTenantId());
                            // }
                            
                            // Get client details from database for complete restoration
                            try {
                                Client client = clientRepository.findById(chatRoom.getClientId()).orElse(null);
                                if (client != null) {
                                    restoreInfo.setClientName(client.getName());
                                    restoreInfo.setClientEmail(client.getEmail());
                                    restoreInfo.setClientPhone(client.getPhone());
                                    restoreInfo.setClientLabel("active");
                                } else {
                                    // Fallback if client not found in database
                                    restoreInfo.setClientName("Conversation Restored");
                                    restoreInfo.setClientLabel("active");
                                }
                            } catch (Exception e) {
                                log.warn("⚠️ Could not get client details for restoration of conversation {}: {}", 
                                        conversationId, e.getMessage());
                                // Use fallback values
                                restoreInfo.setClientName("Conversation Restored");
                                restoreInfo.setClientLabel("active");
                            }
                            
                            userSocketClient.sendEvent(socketConfig.EVENT_NEW_CLIENT_REQ, restoreInfo);
                            log.info("📤 Sent COMPLETE conversation restore notification to user {} for conversation {} with client {}", 
                                    userId, conversationId, chatRoom.getClientId());
                        } else {
                            log.warn("⚠️ Chat room not found in Redis for conversation {}, cannot restore complete details", conversationId);
                            // Send minimal restoration info as fallback
                            restoreInfo.setClientName("Conversation Restored");
                            restoreInfo.setClientLabel("active");
                            userSocketClient.sendEvent(socketConfig.EVENT_NEW_CLIENT_REQ, restoreInfo);
                        }
                    } catch (Exception e) {
                        log.warn("⚠️ Could not get chat room {} from Redis for restore: {}", conversationId, e.getMessage());
                        // Send minimal restoration info as fallback
                        restoreInfo.setClientName("Conversation Restored");
                        restoreInfo.setClientLabel("active");
                        userSocketClient.sendEvent(socketConfig.EVENT_NEW_CLIENT_REQ, restoreInfo);
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
     * Find user for tenant with MINIMUM CLIENT COUNT load balancing
     * Ensures even distribution of workload across available agents
     */
    private ChatUser findUserForTenantEfficiently(String tenantId) {
        log.info("🔍 FINDING USER FOR TENANT WITH LOAD BALANCING - Tenant: {}", tenantId);
        
        if (tenantId == null || tenantId.trim().isEmpty()) {
            log.warn("⚠️ Invalid tenant ID provided: '{}'", tenantId);
            return null;
        }
        
        log.info("🏢 Looking up tenant user pool...");
        // Set<String> tenantUsers = tenantUserPools.get(tenantId); // COMMENTED OUT - Using Redis instead
        Set<String> tenantUsers = null;
        try {
            tenantUsers = redisUserService.getTenantPoolUsers(tenantId);
        } catch (Exception e) {
            log.warn("⚠️ Could not get tenant pool users for {} from Redis: {}", tenantId, e.getMessage());
        }
        log.info("📊 Tenant pool status - Tenant: {}, Pool size: {}", tenantId, tenantUsers != null ? tenantUsers.size() : 0);
        
        if (tenantUsers == null || tenantUsers.isEmpty()) {
            log.info("📭 No users found in tenant pool for: {}", tenantId);
            return null;
        }
        
        log.info("⚖️ LOAD BALANCING: Finding user with minimum current clients...");
        
        String bestUserId = null;
        int minClientCount = Integer.MAX_VALUE;
        
        // Track all available users for debugging
        Map<String, Integer> userLoadMap = new HashMap<>();
        
        for (String userId : tenantUsers) {
            log.debug("⚡ Analyzing user load - User: {}", userId);
            // AtomicInteger userCount = userClientCounts.get(userId); // COMMENTED OUT - Using Redis instead
            // int currentCount = userCount != null ? userCount.get() : 0;
            int currentCount = 0;
            try {
                currentCount = redisUserService.getUserClientCount(userId);
            } catch (Exception e) {
                log.warn("⚠️ Could not get client count for user {} from Redis: {}", userId, e.getMessage());
            }
            
            userLoadMap.put(userId, currentCount);
            log.debug("📊 User {} current clients: {}/{}", userId, currentCount, MAX_CLIENTS_PER_USER);
            
            // Only consider users with available capacity
            if (currentCount < MAX_CLIENTS_PER_USER) {
                // ChatUser user = userMap.get(userId);
                ChatUser user = null;
                try {
                    user = redisUserService.getUser(userId);
                } catch (Exception e) {
                    log.warn("⚠️ Could not get user {} from Redis: {}", userId, e.getMessage());
                }
                
                if (user != null) {
                    // Check if user's last ping time is under 7 seconds (recent activity)
                    long currentTime = System.currentTimeMillis();
                    long timeSinceLastPing = currentTime - user.getLastPingTime();
                    boolean isRecentlyActive = timeSinceLastPing < userPingTimeoutMs; // Configurable timeout
                    
                    log.debug("⏰ User {} ping status - Time since last ping: {}ms, Recently active: {}", 
                             userId, timeSinceLastPing, isRecentlyActive);
                    
                    // Find user with minimum client count for best load balancing, but only if recently active
                    if (currentCount < minClientCount && isRecentlyActive) {
                        minClientCount = currentCount;
                        bestUserId = userId;
                        log.debug("🎯 New best candidate - User: {}, Load: {}/{}, Recently active: {}ms ago", 
                                 userId, currentCount, MAX_CLIENTS_PER_USER, timeSinceLastPing);
                    } else if (!isRecentlyActive) {
                        log.debug("⏰ User {} skipped - Last ping {}ms ago (over 7 second threshold)", 
                                 userId, timeSinceLastPing);
                    }
                } else {
                    log.warn("⚠️ User object not found in Redis for userId: {}", userId);
                }
            } else {
                log.debug("❌ User {} at capacity ({}/{})", userId, currentCount, MAX_CLIENTS_PER_USER);
            }
        }
        
        // Log load balancing decision details
        log.info("📊 LOAD BALANCING ANALYSIS:");
        userLoadMap.forEach((userId, count) -> {
            String status = count >= MAX_CLIENTS_PER_USER ? "AT CAPACITY" : "AVAILABLE";
            log.info("   User {} → {}/{} clients ({})", userId, count, MAX_CLIENTS_PER_USER, status);
        });
        
        if (bestUserId != null) {
            ChatUser selectedUser = null;
            try {
                selectedUser = redisUserService.getUser(bestUserId);
            } catch (Exception e) {
                log.warn("⚠️ Could not get selected user {} from Redis: {}", bestUserId, e.getMessage());
                return null;
            }
            
            log.info("✅ OPTIMAL USER SELECTED - User: {}, Current load: {}/{} (minimum among available)", 
                    bestUserId, minClientCount, MAX_CLIENTS_PER_USER);
            log.info("🎯 Load balancing successful - Selected user with lowest workload");
            return selectedUser;
        } else {
            log.info("❌ NO AVAILABLE USERS - All users in tenant pool are at capacity");
            log.info("💡 Consider adding more agents for tenant: {}", tenantId);
            return null;
        }
    }
    
    /**
     * Add user to tenant pools when they come online
     * Called during ping processing
     */
    @Transactional
    private void addUserToTenantPools(String userId) {
        log.info("🏢 ADDING USER TO TENANT POOLS - User: {}", userId);
        try {
            log.info("💾 Fetching user account from database...");
            Optional<UserAccount> userAccountOpt = userRepository.findById(Long.parseLong(userId));
            
            if (userAccountOpt.isPresent()) {
                log.info("✅ User account found for userId: {}", userId);
                UserAccount userAccount = userAccountOpt.get();
                
                log.info("🔍 Looking up user tenant IDs directly...");
                List<String> tenantIds = userOrgPermissionsRepository.findDistinctTenantIdsByUser(userAccount);
                log.info("📊 Distinct tenant IDs found: {} - {}", tenantIds.size(), tenantIds);
                
                if (tenantIds.isEmpty()) {
                    log.warn("⚠️ No tenant IDs found for user: {}", userId);
                    return;
                }
                
                log.info("🏢 Processing tenant associations...");
                for (String tenantId : tenantIds) {
                    log.info("🔗 Adding user {} to tenant pool: {}", userId, tenantId);
                    
                    // tenantUserPools.computeIfAbsent(tenantId, k -> ConcurrentHashMap.newKeySet()).add(userId); // COMMENTED OUT - Using Redis instead
                    try {
                        redisUserService.addUserToTenantPool(tenantId, userId);
                        long poolSize = redisUserService.getTenantPoolSize(tenantId);
                        log.info("✅ User added to tenant pool - Tenant: {}, Pool size now: {}", tenantId, poolSize);
                    } catch (Exception e) {
                        log.warn("⚠️ Could not add user {} to tenant pool {} in Redis: {}", userId, tenantId, e.getMessage());
                    }
                }
                
                log.info("📊 Initializing client count tracking for user: {}", userId);
                // Initialize client count tracking
                // userClientCounts.put(userId, new AtomicInteger(0)); // COMMENTED OUT - Using Redis instead
                // Client count is now managed in the ChatUser object in Redis
                log.info("✅ Client count tracking initialized for user: {} (managed in Redis)", userId);
                
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
        // tenantUserPools.values().forEach(pool -> pool.remove(userId)); // COMMENTED OUT - Using Redis instead
        // userClientCounts.remove(userId); // COMMENTED OUT - Using Redis instead
        try {
            redisUserService.removeUserFromAllTenantPools(userId);
            log.debug("Removed user {} from all tenant pools", userId);
        } catch (Exception e) {
            log.warn("⚠️ Could not remove user {} from tenant pools in Redis: {}", userId, e.getMessage());
        }
    }
    
    /**
     * Update user client count atomically
     */
    private void incrementUserClientCount(String userId) {
        log.debug("📈 Incrementing client count for user: {}", userId);
        // AtomicInteger count = userClientCounts.computeIfAbsent(userId, k -> new AtomicInteger(0)); // COMMENTED OUT - Using Redis instead
        // int newCount = count.incrementAndGet();
        try {
            redisUserService.incrementUserClientCount(userId);
            int newCount = redisUserService.getUserClientCount(userId);
            log.debug("✅ User {} client count incremented to: {}", userId, newCount);
        } catch (Exception e) {
            log.warn("⚠️ Could not increment client count for user {} in Redis: {}", userId, e.getMessage());
        }
    }
    
    /**
     * Decrement user client count atomically
     */
    private void decrementUserClientCount(String userId) {
        log.debug("📉 Decrementing client count for user: {}", userId);
        // AtomicInteger count = userClientCounts.get(userId); // COMMENTED OUT - Using Redis instead
        try {
            redisUserService.decrementUserClientCount(userId);
            int newCount = redisUserService.getUserClientCount(userId);
            log.debug("✅ User {} client count decremented to: {}", userId, newCount);
            if (newCount < 0) {
                log.warn("⚠️ Negative client count detected for user {}, this should not happen with Redis implementation", userId);
            }
        } catch (Exception e) {
            log.warn("⚠️ Could not decrement client count for user {} in Redis: {}", userId, e.getMessage());
        }
    }

    public void disconnectClient(String socketId) {
        if (socketId == null || socketId.trim().isEmpty()) return;
        try {
            UUID sessionId = UUID.fromString(socketId);
            SocketIOClient client = server.getClient(sessionId);
            if (client != null) {
                log.info("🔌 Actively disconnecting client with socketId: {}", socketId);
                client.disconnect();
            } else {
                log.warn("🔍 Client to disconnect not found for socketId: {}", socketId);
            }
        } catch (IllegalArgumentException e) {
            log.error("❌ Invalid format for socketId UUID: {}", socketId, e);
        }
    }
} 