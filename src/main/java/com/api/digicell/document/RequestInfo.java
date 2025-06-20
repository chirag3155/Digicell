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
public class RequestInfo {
    
    @JsonProperty("ip_address")
    private String ipAddress;
    
    @JsonProperty("isp")
    private String isp;
    
    @JsonProperty("country")
    private String country;
    
    @JsonProperty("region")
    private String region;
    
    @JsonProperty("city")
    private String city;
    
    @JsonProperty("network_type")
    private String networkType;
    
    @JsonProperty("device_info")
    private Object deviceInfo;
    
    @JsonProperty("msc_ip")
    private String mscIp;
    
    @JsonProperty("short_code")
    private String shortCode;
} 