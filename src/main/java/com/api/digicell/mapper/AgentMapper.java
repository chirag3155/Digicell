package com.api.digicell.mapper;

import com.api.digicell.dto.UserAccountUpdateDTO;
import com.api.digicell.entities.UserAccount;
import org.springframework.stereotype.Component;

@Component
public class AgentMapper {

    public void updateAgentFromDTO(UserAccount userAccount, UserAccountUpdateDTO dto) {
        if (dto.getName() != null) {
            userAccount.setUserName(dto.getName());
        }
        if (dto.getEmail() != null) {
            userAccount.setEmail(dto.getEmail());
        }
        if (dto.getLabels() != null) {
            userAccount.setLabels(dto.getLabels());
        }
        if (dto.getStatus() != null) {
            userAccount.setStatus(dto.getStatus());
        }
        userAccount.setUpdatedAt(java.time.LocalDateTime.now());
    }
} 