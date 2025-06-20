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
public class PropsAsr {
    
    @JsonProperty("cached_tokens")
    private Object cachedTokens;
    
    @JsonProperty("vendor")
    private String vendor;
    
    @JsonProperty("lang_code")
    private String langCode;
    
    @JsonProperty("lang_name")
    private String langName;
    
    @JsonProperty("delay")
    private Double delay;
    
    @JsonProperty("language")
    private String language;
    
    @JsonProperty("code")
    private String code;
} 