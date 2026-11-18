package com.minibank.httphandler;

import com.minibank.BankService;
import com.minibank.Client;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.Map;

public class ClientHandler extends BaseHttpHandler{

    private final BankService bankService;


    public ClientHandler(BankService bankService) {
        this.bankService = bankService;
        super.registerRoute("POST", "/clients", this::registerClient);
        super.registerRoute("GET", "clients/{id}", this::getClient);
    }

    private void registerClient(HttpExchange exchange) throws IOException{
        Map<String, String> data = JsonUtil.parseFlatJson(super.readBody(exchange));
        String name = data.get("name");
        String email = data.get("email");

        if(name == null || email == null){
            throw  new IllegalArgumentException("Поля name и email обязательны");
        }

        Client client = bankService.registerClient(name, email);
        super.sendJson(exchange, 200, toJson(client));
    }

    private void getClient(HttpExchange exchange) throws IOException{
        Long id = super.extractId(exchange);
        Client client = bankService.getClient(id);
        sendJson(exchange, 200, toJson(client));
    }

    private String toJson(Client c) {
        return """
                {"id": %d, "name": "%s", "email": "%s"}
                """.formatted(c.getId(), JsonUtil.escape(c.getName()), JsonUtil.escape(c.getEmail())).trim();
    }
}
