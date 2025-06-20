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
public class Language {
    
    @JsonProperty("language")
    private String language;
    
    @JsonProperty("code")
    private String code;
    
    @JsonProperty("region")
    private String region;
} 