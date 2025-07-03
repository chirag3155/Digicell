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
public class PropsTts {
    
    @JsonProperty("start_time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private String startTime;
    
    @JsonProperty("end_time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private String endTime;
    
    @JsonProperty("lang_code")
    private String langCode;
    
    @JsonProperty("voice_name")
    private String voiceName;
    
    @JsonProperty("speed")
    private Double speed;
    
    @JsonProperty("response_format")
    private String responseFormat;
    
    @JsonProperty("style")
    private String style;
    
    @JsonProperty("duration")
    private Double duration;
} 