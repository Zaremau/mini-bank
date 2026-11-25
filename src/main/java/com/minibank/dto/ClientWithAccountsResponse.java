package com.minibank.dto;

import com.minibank.model.Client;

import java.util.List;

public class ClientWithAccountsResponse {
    private Long id;
    private String name;
    private List<AccountResponse> accounts;

    public ClientWithAccountsResponse(Long id, String name, List<AccountResponse> accounts) {
        this.id = id;
        this.name = name;
        this.accounts = accounts;
    }
    public static ClientWithAccountsResponse from(Client client) {
        return new ClientWithAccountsResponse(
                client.getId(),
                client.getName(),
                client.getAccounts()
        );
    }
}
