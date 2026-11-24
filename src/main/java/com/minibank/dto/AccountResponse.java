package com.minibank.dto;


import com.minibank.model.Account;
import java.math.BigDecimal;

public record AccountResponse(
        Long id,
        Long clientId,
        String accountNumber,
        BigDecimal balance
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getClient().getId(),
                account.getAccountNumber(),
                account.getBalance()
        );
    }
}

