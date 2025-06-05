package com.api.digicell.responses;

import com.api.digicell.entities.Agent;
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
    private Agent agent;
    private List<Conversation> conversations;
} 