package com.api.digicell.dto;

import com.api.digicell.entities.UserAccount;
import lombok.Data;
import java.util.List;

@Data
public class UserAccountDetailsResponseDTO {
    private UserAccount userAccount;
    private List<ConversationResponseDTO> conversations;
} 