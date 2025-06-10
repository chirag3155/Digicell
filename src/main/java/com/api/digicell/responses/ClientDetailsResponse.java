package com.api.digicell.responses;

import com.api.digicell.entities.Client;
import com.api.digicell.dto.ClientConvoDto;
import java.util.List;

public class ClientDetailsResponse {
    private Client client;
    private List<ClientConvoDto> conversations;

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public List<ClientConvoDto> getConversations() {
        return conversations;
    }

    public void setConversations(List<ClientConvoDto> conversations) {
        this.conversations = conversations;
    }
} 