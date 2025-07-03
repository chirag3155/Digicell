package com.api.digicell.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class SocketConfig {
    // Connection Parameters
    public static final String PARAM_CLIENT_TYPE = "clientType";
    public static final String PARAM_CHAT_MODULE = "chat_module";
    public static final String PARAM_AGENT = "agent";

    // Status Codes
    public static final int STATUS_SUCCESS = 201;
    public static final int STATUS_UNAVAILABLE = 501;

    // Event Names
    public static final String EVENT_AGENT_REQUEST = "request_agent";
    public static final String EVENT_AGENT_ACK = "agent_acknowledgement";
    public static final String EVENT_NEW_CLIENT_REQ = "new_client_req";
    
    // Message Events
    public static final String EVENT_MESSAGE_REQ ="conversation_message";
    public static final String EVENT_MESSAGE_REQ_AGENT = "message_req_agent";
    public static final String EVENT_MESSAGE_RESP_AGENT = "message_resp_agent";
    public static final String EVENT_MESSAGE_RESP = "agent_response";
    
    // Chat Closure Events
    public static final String EVENT_CLOSE_AGENT = "close_agent";
    public static final String EVENT_CLIENT_CLOSE = "client_close";
    public static final String EVENT_CLOSE = "close_chat";
    
    // Agent Status Events
    public static final String EVENT_PING = "ping";
    public static final String EVENT_PONG = "pong";
    public static final String EVENT_PING_RESPONSE = "ping_response";
    public static final String EVENT_OFFLINE_REQ = "offline_req";

    @Value("${socket.host}")
    private String host;

    @Value("${socket.port}")
    private int port;

    @Value("${socket.ping-timeout}")
    private int pingTimeout;

    @Value("${socket.ping-interval}")
    private int pingInterval;

    @Value("${socket.agent-ping-interval}")
    private int agentPingInterval;
} 