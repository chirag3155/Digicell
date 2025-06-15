package com.api.digicell.document;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationDocument {

    private String id; // Elasticsearch document ID (String)

    private String conversationId;
    private Long userId;
    private String userName;
    private String clientId;
    private String clientName;
    private String intent;
    private List<String> labels;
    private String chatSummary;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private String startTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private String endTime;

    private List<ChatMessageDocument> chatHistory;  // Nested field

}
