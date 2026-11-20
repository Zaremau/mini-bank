package com.minibank;

import com.minibank.db.ConnectionManager;
import com.minibank.db.LiquibaseRunner;
import com.minibank.model.Account;
import com.minibank.model.Client;
import com.minibank.service.BankService;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        LiquibaseRunner.runMigrations();
        cleanupDemoUser("ivan@example.com");

        BankService bank = new BankService();

        Client client = bank.registerClient("Ivan Petrov", "ivan@example.com");
        Account account1 = bank.openAccount(client.getId(), "ACC-001");
        Account account2 = bank.openAccount(client.getId(), "ACC-002");

        bank.deposit(account1.getId(), new BigDecimal("1000.00"));

        System.out.println("=== Обычный перевод ===");
        bank.transfer(account1.getId(), account2.getId(), new BigDecimal("100.00"));
        System.out.println("account1 balance: " + bank.getAccount(account1.getId()).getBalance());
        System.out.println("account2 balance: " + bank.getAccount(account2.getId()).getBalance());

        System.out.println();
        System.out.println("=== Перевод на несуществующий счёт (баг) ===");
        BigDecimal balanceBefore = bank.getAccount(account1.getId()).getBalance();
        try {
            bank.transfer(account1.getId(), 99999L, new BigDecimal("50.00"));
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка (как и ожидалось): " + e.getMessage());
        }
        BigDecimal balanceAfter = bank.getAccount(account1.getId()).getBalance();
        System.out.println("Баланс до попытки перевода: " + balanceBefore);
        System.out.println("Баланс после попытки перевода: " + balanceAfter);
        System.out.println("Деньги пропали: " + (balanceAfter.compareTo(balanceBefore) < 0));
    }

    /**
     * Служебный метод для очистки тестовых данных в базе данных во время перезапуска теста
     * @param email электронная почта клиента
     */
    private static void cleanupDemoUser(String email) {
        String deleteTransactionsSql = """
            DELETE FROM transactions
            WHERE account_id IN (
                SELECT a.id FROM accounts a
                JOIN clients c ON a.client_id = c.id
                WHERE c.email = ?
            )
            """;
        String deleteAccountsSql = """
            DELETE FROM accounts
            WHERE client_id IN (SELECT id FROM clients WHERE email = ?)
            """;
        String deleteClientSql = "DELETE FROM clients WHERE email = ?";

        try (Connection conn = ConnectionManager.getConnection()) {
            for (String sql : List.of(deleteTransactionsSql, deleteAccountsSql, deleteClientSql)) {
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, email);
                    stmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при очистке демо-данных", e);
        }
    }
}
