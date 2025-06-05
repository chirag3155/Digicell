package com.api.digicell.responses;

import com.api.digicell.dto.UserConversationDTO;
import com.api.digicell.entities.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * Wrapper holding a user along with the conversations that belong to the user.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailsResponse {
    private User user;
    private List<UserConversationDTO> conversations;
} 