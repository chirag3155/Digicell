package com.api.digicell.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackMessage {
    private String roomId;
    private String clientId;
    private String feedback;
    private int rating;
} 