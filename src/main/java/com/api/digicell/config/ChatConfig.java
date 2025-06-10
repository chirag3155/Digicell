package com.api.digicell.config;

import com.api.digicell.chat.ChatModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Configuration
public class ChatConfig {
    private final ChatModule chatModule;

    public ChatConfig(ChatModule chatModule) {
        this.chatModule = chatModule;
    }

    @PostConstruct
    public void init() {
        chatModule.start();
    }

    @PreDestroy
    public void cleanup() {
        chatModule.stop();
    }
} 