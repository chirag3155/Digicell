package com.api.digicell.document;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LlmFailure {
    
    @JsonProperty("failure_code")
    private String failureCode;
    
    @JsonProperty("failure_message")
    private String failureMessage;
} 