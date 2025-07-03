package com.api.digicell.document;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tool {
    
    @JsonProperty("tool_id")
    private String toolId;
    
    @JsonProperty("tool_version")
    private Double toolVersion;
    
    @JsonProperty("tool_name")
    private String toolName;
    
    @JsonProperty("tool_result")
    private String toolResult;
    
    @JsonProperty("tool_failure")
    private ToolFailure toolFailure;
    
    @JsonProperty("start_time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private String startTime;
    
    @JsonProperty("end_time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private String endTime;
} 