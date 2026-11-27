package com.minibank.dto;

import com.minibank.model.Client;

public record ClientResponse(
        Long id,
        String name,
        String email
) {
    public static ClientResponse from(Client client) {
        return new ClientResponse(
                client.getId(),
                client.getName(),
                client.getEmail()
        );
    }
}
