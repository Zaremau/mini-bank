package com.minibank.httphandler;

import com.minibank.model.Account;
import com.minibank.service.BankService;
import com.minibank.dto.DepositRequest;
import com.minibank.dto.OpenAccountRequest;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.math.BigDecimal;

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

        if(request.accountNumber() == null || request.accountNumber().isBlank()
                || request.clientId() == null){
            throw new IllegalArgumentException("Поля accountNumber и clientId обязательны");
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

        if (request.amount() == null) {
            throw new IllegalArgumentException("Поле amount обязательно");
        }
        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сумма должна быть положительной");
        }

        bankService.deposit(accountId, request.amount());
        Account updatedAccount = bankService.getAccount(accountId);
        super.sendJson(exchange, 200, updatedAccount);
    }
}
