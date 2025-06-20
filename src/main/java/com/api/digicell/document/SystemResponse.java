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
public class SystemResponse {
    
    @JsonProperty("sys_query_time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    private String sysQueryTime;
    
    @JsonProperty("sys_response_time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    private String sysResponseTime;
    
    @JsonProperty("type")
    private String type; // "system" or "Real Agent"
    
    @JsonProperty("sys_response")
    private String sysResponse;
    
    @JsonProperty("first_chunk_time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private String firstChunkTime;
    
    @JsonProperty("llm_duration")
    private Double llmDuration;
    
    @JsonProperty("llm_failure")
    private LlmFailure llmFailure;
    
    @JsonProperty("tools")
    private List<Tool> tools;
    
    @JsonProperty("work_flow")
    private WorkFlow workFlow;
    
    @JsonProperty("input_tokens")
    private Integer inputTokens;
    
    @JsonProperty("output_tokens")
    private Integer outputTokens;
    
    @JsonProperty("cached_tokens")
    private Integer cachedTokens;
    
    @JsonProperty("real_agent_details")
    private RealAgentDetails realAgentDetails;
} 