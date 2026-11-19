package com.minibank.httphandler;

import com.minibank.BankService;
import com.minibank.Client;
import com.minibank.dto.RegisterClientRequest;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.Map;

public class ClientHandler extends BaseHttpHandler{

    private final BankService bankService;


    public ClientHandler(BankService bankService) {
        this.bankService = bankService;
        super.registerRoute("POST", "/clients", this::registerClient);
        super.registerRoute("GET", "/clients/{id}", this::getClient);
    }

    private void registerClient(HttpExchange exchange) throws IOException{
        RegisterClientRequest request = JsonUtil.fromJson(super.readBody(exchange), RegisterClientRequest.class);

        if(request.name() == null || request.email() == null){
            throw  new IllegalArgumentException("Поля name и email обязательны");
        }

        Client client = bankService.registerClient(request.name(), request.email());
        super.sendJson(exchange, 201,client);
    }

    private void getClient(HttpExchange exchange) throws IOException{
        Long id = super.extractId(exchange);
        Client client = bankService.getClient(id);
        sendJson(exchange, 200, client);
    }
}
