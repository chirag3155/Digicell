package com.api.digicell.entities;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import com.fasterxml.jackson.annotation.JsonFormat;

import com.api.digicell.converters.ChatHistoryConverter;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "Conversations")
public class Conversation {
    @Id
    @Column(name = "conversation_id")
    private String conversationId;

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount userAccount;
} 