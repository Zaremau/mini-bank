package com.minibank.httphandler;

import com.minibank.Account;
import com.minibank.BankService;
import com.minibank.dto.DepositRequest;
import com.minibank.dto.OpenAccountRequest;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

public class AccountHandler extends BaseHttpHandler {

    private final BankService bankService;


    public AccountHandler(BankService bankService) {
        this.bankService = bankService;
        super.registerRoute("POST", "/accounts", this::openAccount);
        super.registerRoute("GET", "/accounts/{id}", this::getAccount);
        super.registerRoute("POST", "/accounts/{id}/deposit", this::deposit);
    }

    private void openAccount(HttpExchange exchange) throws IOException {
        OpenAccountRequest request = JsonUtil.fromJson(super.readBody(exchange), OpenAccountRequest.class);

        if(request.accountNumber() == null || request.accountNumber().isBlank()){
            throw new IllegalArgumentException("Поле accountNumber обязательно");
        }

        Account account = bankService.openAccount(request.clientId(), request.accountNumber());
        super.sendJson(exchange, 201, account);
    }

    private void getAccount(HttpExchange exchange) throws IOException {
        Long accountId = super.extractId(exchange);
        Account account = bankService.getAccount(accountId);
        super.sendJson(exchange, 200, account);
    }

    private void deposit(HttpExchange exchange) throws IOException{
        Long accountId = super.extractId(exchange);
        DepositRequest request = JsonUtil.fromJson(super.readBody(exchange), DepositRequest.class);

        bankService.deposit(accountId, request.amount());
        Account updatedAccount = bankService.getAccount(accountId);
        super.sendJson(exchange, 200, updatedAccount);
    }
}
