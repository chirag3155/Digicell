package com.api.digicell.dto;

import com.api.digicell.entities.AgentStatus;
import lombok.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Email;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentCreateDTO {
    @NotBlank(message = "Name is required")
    private String name;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;
    
    private String avatarUrl;
    
    private List<String> labels;

    @NotNull(message = "Status is required")
    private AgentStatus status = AgentStatus.AVAILABLE; // Default status
} 