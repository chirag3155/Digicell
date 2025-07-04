package com.api.digicell.dto;

import com.api.digicell.entities.AgentStatus;
import jakarta.validation.constraints.Email;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentUpdateDTO {
    
    private String name;
    
    @Email(message = "Invalid email format")
    private String email;
    
    private String avatarUrl;
    
    private List<String> labels;
    
    private AgentStatus status;
} 