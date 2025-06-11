package com.api.digicell.responses;

import com.api.digicell.entities.UserAccount;
import com.api.digicell.entities.Conversation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Wrapper holding an agent along with the conversations assigned to the agent.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentDetailsResponse {
    private UserAccount userAccount;
    private List<Conversation> conversations;
} 