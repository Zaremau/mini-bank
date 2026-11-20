package com.minibank.httphandler;

import com.minibank.service.BankService;
import com.minibank.dto.TransferRequest;
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
        TransferRequest request = JsonUtil.fromJson(super.readBody(exchange), TransferRequest.class);

        Long fromAccountId = request.fromAccountId();
        Long toAccountId = request.toAccountId();
        BigDecimal amount = request.amount();

        if (fromAccountId == null || toAccountId == null || amount == null) {
            throw new IllegalArgumentException("Поля fromAccountId, toAccountId и amount обязательны");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сумма должна быть положительной");
        }
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Нельзя переводить на тот же счёт");
        }

        bankService.transfer(fromAccountId, toAccountId, amount);
        super.sendJson(exchange, 200, Map.of(
                "status", "completed",
                "fromAccountId", fromAccountId,
                "toAccountId", toAccountId,
                "amount", amount
        ));
    }
}
