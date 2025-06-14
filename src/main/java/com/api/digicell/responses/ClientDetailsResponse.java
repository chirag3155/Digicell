package com.api.digicell.responses;

import com.api.digicell.dto.ClientConversationDTO;
import com.api.digicell.entities.Client;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * Wrapper holding a client along with the conversations that belong to the client.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientDetailsResponse {
    private Client client;
    private List<ClientConversationDTO> conversations;
} 