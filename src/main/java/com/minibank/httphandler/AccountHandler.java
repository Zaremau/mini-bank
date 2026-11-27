package com.minibank.httphandler;

import com.minibank.dto.AccountResponse;
import com.minibank.dto.DepositRequest;
import com.minibank.dto.OpenAccountRequest;
import com.minibank.model.Account;
import com.minibank.service.BankService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.math.BigDecimal;

public class AccountHandler extends BaseHttpHandler {

    private final BankService bankService;

    public AccountHandler(BankService bankService) {
        this.bankService = bankService;
        registerRoute("POST", "/accounts", this::openAccount);
        registerRoute("GET", "/accounts/{id}", this::getAccount);
        registerRoute("POST", "/accounts/{id}/deposit", this::deposit);
    }

    private void openAccount(HttpExchange exchange) throws IOException {
        OpenAccountRequest request = JsonUtil.fromJson(readBody(exchange), OpenAccountRequest.class);

        if (request.accountNumber() == null || request.accountNumber().isBlank()
                || request.clientId() == null) {
            throw new IllegalArgumentException("Поля accountNumber и clientId обязательны");
        }

        AccountResponse account = bankService.openAccount(request.clientId(), request.accountNumber());
        sendJson(exchange, 201,account);
    }

    private void getAccount(HttpExchange exchange) throws IOException {
        Long accountId = extractId(exchange);
        Account account = bankService.getAccount(accountId);
        sendJson(exchange, 200, AccountResponse.from(account));
    }

    private void deposit(HttpExchange exchange) throws IOException {
        Long accountId = extractId(exchange);
        DepositRequest request = JsonUtil.fromJson(readBody(exchange), DepositRequest.class);

        if (request.amount() == null) {
            throw new IllegalArgumentException("Поле amount обязательно");
        }
        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сумма должна быть положительной");
        }

        bankService.deposit(accountId, request.amount());
        Account updatedAccount = bankService.getAccount(accountId);
        sendJson(exchange, 200, AccountResponse.from(updatedAccount));
    }
}

