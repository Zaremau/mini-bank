package com.minibank.httphandler;

import com.minibank.BankService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

public class TransferHandler extends BaseHttpHandler {

    private final BankService bankService;

    public TransferHandler(BankService bankService) {
        this.bankService = bankService;
        super.registerRoute("POST", "/transfer", this::transfer);
    }

    private void transfer(HttpExchange exchange) throws IOException {
        Map<String, String> data = JsonUtil.parseFlatJson(super.readBody(exchange));

        Long fromAccountId = Long.parseLong(data.get("fromAccountId"));
        Long toAccountId = Long.parseLong(data.get("toAccountId"));
        BigDecimal amount = new BigDecimal(data.get("amount"));

        bankService.transfer(fromAccountId, toAccountId, amount);
        super.sendJson(exchange, 200, "Transfer completed");
    }
}
