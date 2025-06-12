package com.api.digicell.dto;

import com.api.digicell.entities.UserAccount;
import lombok.Data;
import java.util.List;

@Data
public class UserAccountDetailsResponseDTO {
    private UserAccountResponseDTO userAccountResponseDTO;
    private List<ConversationResponseDTO> conversations;
} 