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
public class ConversationEndInfo {
    
    @JsonProperty("type")
    private String type;
    
    @JsonProperty("reason")
    private String reason;
} 