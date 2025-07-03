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
public class PropsLlm {
    
    @JsonProperty("provider")
    private String provider;
    
    @JsonProperty("model")
    private String model;
    
    @JsonProperty("version")
    private String version;
    
    @JsonProperty("requestTime")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private String requestTime;
    
    @JsonProperty("responseTime")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private String responseTime;
    
    @JsonProperty("temperature")
    private Double temperature;
} 