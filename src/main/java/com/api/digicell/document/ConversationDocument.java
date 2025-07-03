package com.api.digicell.document;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("tenant_id")
    private String tenantId;

    @JsonProperty("assistant_id")
    private String assistantId;

    @JsonProperty("user_info")
    private UserInfo userInfo;

    @JsonProperty("convesation_id")
    private String conversationId;

    @JsonProperty("convesation_start_time")
    private String conversationStartTime;

    @JsonProperty("connection_establish_time")
    private String connectionEstablishTime;

    @JsonProperty("connection_establish")
    private Boolean connectionEstablish;

    @JsonProperty("channel_info")
    private ChannelInfo channelInfo;

    @JsonProperty("request_info")
    private RequestInfo requestInfo;

    @JsonProperty("messages")
    private List<ConversationMessage> messages;

    @JsonProperty("conversaton_end_time")
    private String conversationEndTime;

    @JsonProperty("conversation_end_info")
    private Object conversationEndInfo;

    @JsonProperty("type (user/system)")
    private String type;

    @JsonProperty("conversation_analysis")
    private ConversationAnalysis conversationAnalysis;
}
