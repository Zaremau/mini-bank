package com.minibank.httphandler;

import com.minibank.Account;
import com.minibank.BankService;
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
        Map<String, String> data = JsonUtil.parseFlatJson(super.readBody(exchange));
        String accountNumber = data.get("accountNumber");
        Long clientId = Long.parseLong(data.get("clientId"));

        if(accountNumber == null || accountNumber.isBlank()){
            throw new IllegalArgumentException("Поле accountNumber обязательно");
        }

        Account account = bankService.openAccount(clientId, accountNumber);
        super.sendJson(exchange, 201, toJson(account));
    }

    private void getAccount(HttpExchange exchange) throws IOException {
        Map<String, String> data = JsonUtil.parseFlatJson(super.readBody(exchange));
        Long accountId = Long.parseLong(data.get("accountId"));

        Account account = bankService.getAccount(accountId);
        super.sendJson(exchange, 200, toJson(account));
    }

    private void deposit(HttpExchange exchange) throws IOException{
        Map<String, String> data = JsonUtil.parseFlatJson(super.readBody(exchange));

        Long accountId = Long.parseLong(data.get("accountId"));
        BigDecimal amount = new BigDecimal(data.get("amount"));

        bankService.deposit(accountId, amount);
        Account updatedAccount = bankService.getAccount(accountId);
        super.sendJson(exchange, 200, toJson(updatedAccount));
    }
    private String toJson(Account a) {
        return """
                {"id": %d, "clientId": %d, "accountNumber": "%s", "balance": %s}
                """.formatted(a.getId(), a.getClientId(), JsonUtil.escape(a.getAccountNumber()), a.getBalance()).trim();
    }
}
