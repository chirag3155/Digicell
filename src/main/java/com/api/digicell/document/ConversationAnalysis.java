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
public class ConversationAnalysis {
    
    @JsonProperty("intent")
    private List<String> intent;
    
    @JsonProperty("summary")
    private String summary;
    
    @JsonProperty("languages")
    private List<String> languages;
} 