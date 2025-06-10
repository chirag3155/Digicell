package com.api.digicell.chat;

import com.api.digicell.model.ChatAgent;
import com.api.digicell.entities.Agent;
import com.api.digicell.entities.AgentStatus;
import com.api.digicell.services.AgentService;
import com.api.digicell.dto.AgentStatusDTO;
import com.corundumstudio.socketio.*;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
    private final Map<String, ChatAgent> agentMap; // Track all agents including those on break
    private final AgentService agentService;
    private static final int MAX_USERS_PER_AGENT = 5;

    public ChatModule(AgentService agentService) {
        Configuration config = new Configuration();
        config.setHostname("localhost");
        config.setPort(9092);
        config.setPingTimeout(60000);
        config.setPingInterval(25000);

        this.server = new SocketIOServer(config);
        this.userAgentMapping = new ConcurrentHashMap<>();
        this.agentQueue = new PriorityBlockingQueue<>(100, new ChatAgent.AgentComparator());
        this.agentRooms = new ConcurrentHashMap<>();
        this.agentMap = new ConcurrentHashMap<>();
        this.agentService = agentService;

        initializeSocketListeners();
    }

    private void initializeSocketListeners() {
        server.addConnectListener(client -> {
            log.debug("Client connected: {}", client.getSessionId());
        });

        server.addEventListener("join_room", String.class, (client, roomId, ackSender) -> {
            log.debug("Client {} joining room: {}", client.getSessionId(), roomId);
            client.joinRoom(roomId);
        });

        server.addEventListener("leave_room", String.class, (client, roomId, ackSender) -> {
            log.debug("Client {} leaving room: {}", client.getSessionId(), roomId);
            client.leaveRoom(roomId);
        });

        server.addEventListener("message", ChatMessage.class, (client, message, ackSender) -> {
            String roomId = message.getRoomId();
            log.debug("Message received in room {}: {}", roomId, message);
            server.getRoomOperations(roomId).sendEvent("message", message);
        });

        server.addEventListener("request_agent", String.class, (client, userId, ackSender) -> {
            log.debug("User {} requesting agent", userId);
            assignAgentToUser(userId, client);
        });

        server.addEventListener("close_chat", String.class, (client, roomId, ackSender) -> {
            log.debug("Closing chat for room: {}", roomId);
            handleChatClosure(roomId);
        });

        server.addEventListener("submit_feedback", FeedbackMessage.class, (client, feedback, ackSender) -> {
            log.debug("Feedback received for room: {}", feedback.getRoomId());
            handleFeedback(feedback);
        });

        // Add new listeners for break management
        server.addEventListener("request_break", String.class, (client, agentId, ackSender) -> {
            log.debug("Agent {} requesting break", agentId);
            handleBreakRequest(agentId);
        });

        server.addEventListener("agent_available", String.class, (client, agentId, ackSender) -> {
            log.debug("Agent {} marking self as available", agentId);
            handleAgentAvailable(agentId);
        });
    }

    private void handleBreakRequest(String agentId) {
        ChatAgent agent = agentMap.get(agentId);
        if (agent == null) {
            log.error("Agent {} not found in agent map", agentId);
            return;
        }

        // Mark agent as applied for break
        agent.setAppliedForBreak(true);
        log.debug("Agent {} marked for break", agentId);

        // Check if agent has active chats
        Set<String> activeRooms = agentRooms.getOrDefault(agentId, Collections.emptySet());
        if (activeRooms.isEmpty()) {
            // No active chats, can go on break immediately
            updateAgentStatus(agentId, AgentStatus.BREAK);
            log.info("Agent {} has no active chats, going on break", agentId);
        } else {
            log.info("Agent {} has {} active chats, will go on break when completed", 
                    agentId, activeRooms.size());
        }
    }

    private void handleAgentAvailable(String agentId) {
        try {
            // Update agent status in database
            Agent agent = agentService.setAgentAvailable(Long.parseLong(agentId));
            log.info("Agent {} status updated to AVAILABLE in database", agentId);

            // Get or create ChatAgent
            ChatAgent chatAgent = agentMap.computeIfAbsent(agentId, k -> {
                ChatAgent newAgent = new ChatAgent();
                newAgent.setAgentId(agentId);
                newAgent.setAppliedForBreak(false);
                newAgent.setCurrentUserCount(0);
                return newAgent;
            });

            // Reset break status
            chatAgent.setAppliedForBreak(false);
            
            // Add to queue if not already present
            if (!agentQueue.contains(chatAgent)) {
                agentQueue.offer(chatAgent);
                log.info("Agent {} added back to queue", agentId);
            }

        } catch (Exception e) {
            log.error("Error handling agent available event for agent {}: {}", agentId, e.getMessage(), e);
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

    private void assignAgentToUser(String userId, SocketIOClient client) {
        ChatAgent agent = agentQueue.poll();
        if (agent != null && !agent.isAppliedForBreak() && agent.getCurrentUserCount() < MAX_USERS_PER_AGENT) {
            String roomId = UUID.randomUUID().toString();
            userAgentMapping.put(userId, agent.getAgentId());
            
            agent.setCurrentUserCount(agent.getCurrentUserCount() + 1);
            agentQueue.offer(agent);
            
            agentRooms.computeIfAbsent(agent.getAgentId(), k -> new HashSet<>()).add(roomId);
            
            client.joinRoom(roomId);
            server.getRoomOperations(roomId).sendEvent("agent_assigned", agent.getAgentId());
            
            log.debug("Assigned agent {} to user {} in room {}", agent.getAgentId(), userId, roomId);
        } else {
            client.sendEvent("no_agent_available");
            log.warn("No agent available for user: {}", userId);
        }
    }

    private void handleChatClosure(String roomId) {
        // Remove room from active connections but keep mapping until feedback
        server.getRoomOperations(roomId).sendEvent("request_feedback");
        log.debug("Chat closed for room: {}", roomId);

        // Check if this was the last active chat for an agent on break
        String agentId = findAgentIdByRoomId(roomId);
        if (agentId != null) {
            ChatAgent agent = agentMap.get(agentId);
            if (agent != null && agent.isAppliedForBreak()) {
                Set<String> activeRooms = agentRooms.getOrDefault(agentId, Collections.emptySet());
                if (activeRooms.isEmpty()) {
                    // No more active chats, agent can go on break
                    updateAgentStatus(agentId, AgentStatus.BREAK);
                    log.info("Agent {} has completed all chats, going on break", agentId);
                }
            }
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

    private void handleFeedback(FeedbackMessage feedback) {
        String roomId = feedback.getRoomId();
        String userId = feedback.getUserId();
        
        // Process feedback and remove mapping
        userAgentMapping.remove(userId);
        
        // Update agent's user count
        String agentId = feedback.getAgentId();
        agentRooms.getOrDefault(agentId, Collections.emptySet()).remove(roomId);
        
        log.debug("Feedback processed for room: {}", roomId);
    }

    public void start() {
        server.start();
        log.info("Chat module started on port 9092");
    }

    public void stop() {
        server.stop();
        log.info("Chat module stopped");
    }
} 