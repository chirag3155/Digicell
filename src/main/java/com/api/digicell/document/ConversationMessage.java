package com.api.digicell.document;

import com.fasterxml.jackson.annotation.JsonFormat;
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
public class ConversationMessage {
    
    @JsonProperty("correlation_id")
    private String correlationId;
    
    @JsonProperty("user_messsage")
    private Object userMessage;
    
    @JsonProperty("content")
    private String content;
    
    @JsonProperty("request_time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS", timezone = "UTC")
    private String requestTime;
    
    @JsonProperty("response_time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS", timezone = "UTC")
    private String responseTime;
    
    @JsonProperty("system_response")
    private List<SystemResponse> systemResponse;
    
    @JsonProperty("language")
    private Language language;
    
    @JsonProperty("props_llm")
    private PropsLlm propsLlm;
    
    @JsonProperty("props_asr")
    private PropsAsr propsAsr;
    
    @JsonProperty("props_tts")
    private PropsTts propsTts;
    
    @JsonProperty("analysis")
    private Analysis analysis;
} 