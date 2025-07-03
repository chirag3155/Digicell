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
public class ChannelInfo {
    
    @JsonProperty("type")
    private String type;
    
    @JsonProperty("provider")
    private String provider;
    
    @JsonProperty("interface_id")
    private String interfaceId;
    
    @JsonProperty("direction")
    private String direction;
    
    @JsonProperty("additional_info")
    private Object additionalInfo;
    
    @JsonProperty("protocol")
    private String protocol;
} 