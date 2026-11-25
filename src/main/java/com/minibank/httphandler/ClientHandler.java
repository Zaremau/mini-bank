package com.minibank.httphandler;

import com.minibank.dto.ClientResponse;
import com.minibank.dto.ClientWithAccountsResponse;
import com.minibank.service.BankService;
import com.minibank.dto.RegisterClientRequest;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

public class ClientHandler extends BaseHttpHandler{

    private final BankService bankService;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.-]+@[\\w.-]+\\.\\w+$");

    public ClientHandler(BankService bankService) {
        this.bankService = bankService;
        super.registerRoute("POST", "/clients", this::registerClient);
        super.registerRoute("GET", "/clients/{id}", this::getClient);
        super.registerRoute("GET", "/clients", this::getAllClients);
        super.registerRoute("GET", "/clients/{id}/details", this::getClientWithAccounts);
        super.registerRoute("GET", "/clients/details", this::getAllClientsWithAccounts);
        super.registerRoute("DELETE", "/clients/{id}/accounts/{id}", this::removeAccountFromClient);
    }

    private void registerClient(HttpExchange exchange) throws IOException{
        RegisterClientRequest request = JsonUtil.fromJson(super.readBody(exchange), RegisterClientRequest.class);

        if (request.name() == null || request.name().isBlank() ||
                request.email() == null || request.email().isBlank()) {
            throw new IllegalArgumentException("Поля name и email обязательны");
        }
        if (!EMAIL_PATTERN.matcher(request.email()).matches()) {
            throw new IllegalArgumentException("Некорректный формат email");
        }

        ClientResponse  client = bankService.registerClient(request.name(), request.email());
        super.sendJson(exchange, 201, client);
    }

    private void getClient(HttpExchange exchange) throws IOException{
        Long id = super.extractId(exchange);
        ClientResponse client = bankService.getClient(id);
        sendJson(exchange, 200, client);
    }
    private void getClientWithAccounts(HttpExchange exchange) throws IOException{
        Long id = super.extractId(exchange);
        ClientWithAccountsResponse client = bankService.getClientWithAccounts(id);
        sendJson(exchange, 200, client);
    }


    private void getAllClients(HttpExchange exchange) throws IOException {
        List<ClientResponse> response = bankService.getAllClients();
        sendJson(exchange, 200, response);
    }

    private void getAllClientsWithAccounts(HttpExchange exchange) throws IOException {
        List<ClientWithAccountsResponse> response = bankService.getAllClientsWithAccounts();
        sendJson(exchange, 200, response);
    }

    private void removeAccountFromClient(HttpExchange exchange) throws IOException {
        List<Long> ids = super.extractIds(exchange);
        if (ids.size() < 2) {
            throw new IllegalArgumentException("В URL должны быть указаны ID клиента и ID счета");
        }
        Long clientId = ids.get(0);
        Long accountId = ids.get(1);

        bankService.removeAccountFromClient(clientId, accountId);

        exchange.sendResponseHeaders(204, -1);
        exchange.close();
    }

}
