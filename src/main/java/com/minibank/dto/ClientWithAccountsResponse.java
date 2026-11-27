package com.minibank.dto;

import com.minibank.model.Client;

import java.util.List;
public record ClientWithAccountsResponse(
        Long id,
        String name,
        List<AccountResponse> accounts
) {
    public static ClientWithAccountsResponse from(Client client) {
        return new ClientWithAccountsResponse(
                client.getId(),
                client.getName(),
                client.getAccounts().stream()
                        .map(AccountResponse::from)
                        .toList()
        );
    }
}
