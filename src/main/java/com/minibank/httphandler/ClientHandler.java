package com.minibank.httphandler;

import com.minibank.dto.ClientResponse;
import com.minibank.service.BankService;
import com.minibank.model.Client;
import com.minibank.dto.RegisterClientRequest;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.regex.Pattern;

public class ClientHandler extends BaseHttpHandler{

    private final BankService bankService;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.-]+@[\\w.-]+\\.\\w+$");

    public ClientHandler(BankService bankService) {
        this.bankService = bankService;
        super.registerRoute("POST", "/clients", this::registerClient);
        super.registerRoute("GET", "/clients/{id}", this::getClient);
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

        Client client = bankService.registerClient(request.name(), request.email());
        super.sendJson(exchange, 201, ClientResponse.from(client));
    }

    private void getClient(HttpExchange exchange) throws IOException{
        Long id = super.extractId(exchange);
        Client client = bankService.getClient(id);
        sendJson(exchange, 200, ClientResponse.from(client));
    }
}
