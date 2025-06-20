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
public class UserMessage {
    
    @JsonProperty("content")
    private String content;
    
    @JsonProperty("type")
    private String type;
} 