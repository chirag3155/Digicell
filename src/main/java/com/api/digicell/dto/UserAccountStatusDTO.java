package com.api.digicell.dto;

import com.api.digicell.entities.UserAccountStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAccountStatusDTO {
    @NotNull(message = "Status is required")
    private UserAccountStatus status;
} 