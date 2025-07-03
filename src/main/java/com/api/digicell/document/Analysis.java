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
public class Analysis {
    
    @JsonProperty("sentiment")
    private String sentiment;
    
    @JsonProperty("emotion")
    private String emotion;
    
    @JsonProperty("purpose")
    private String purpose;
    
    @JsonProperty("accuracy *")
    private Double accuracy;
    
    @JsonProperty("sentiment_score*")
    private Double sentimentScore;
    
    @JsonProperty("Satifcation_score*")
    private Double satisfactionScore;
    
    @JsonProperty("moderation")
    private Object moderation;
    
    @JsonProperty("additional_attribues")
    private Object additionalAttributes;
} 