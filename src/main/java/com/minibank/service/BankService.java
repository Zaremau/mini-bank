package com.minibank.service;

import com.minibank.db.ConnectionManager;
import com.minibank.exception.AccountNotFoundException;
import com.minibank.exception.ClientNotFoundException;
import com.minibank.exception.InsufficientFundsException;
import com.minibank.model.Account;
import com.minibank.model.Client;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * MiniBank — "MVP", который наспех собрал предыдущий разработчик:
 * вся логика в одном классе, все данные — в HashMap, персистентности
 * нет вообще (перезапуск процесса — и всё пропало).
 * <p>
 * ПРОБЛЕМА (найдёшь и исправишь, когда дойдёт очередь — модуль 7.1,
 * когда появится @Transactional): в transfer() проверка существования
 * счёта-получателя происходит ПОСЛЕ списания денег со счёта-
 * отправителя. Если счёт-получатель не существует, деньги уже спишутся
 * с первого счёта и "исчезнут" — их некуда вернуть, потому что метод
 * прерывается исключением до зачисления.
 * <p>
 * Эта проблема не решается на HashMap "в лоб" без искусственных
 * костылей (можно вручную откатывать баланс в catch, но это не
 * настоящее решение) — она по-настоящему решается только когда
 * появится транзакционность (сначала в БД — ACID, модуль 4, а затем
 * в коде — @Transactional, модуль 7.1). До этого момента баг просто
 * существует и это нормально: то, что появится в модуле 7.1, — как
 * раз тот инструмент, который проблему полностью закрывает.
 */
public class BankService {


    public Client registerClient(String name, String email) {
        try (PreparedStatement statement = CONNECTION.prepareStatement(INSERT_CLIENT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, name);
            statement.setString(2, email);
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    Long id = generatedKeys.getLong(1);
                    return new Client(id, name, email);
                } else {
                    throw new SQLException("Не удалось создать клиента: ID не получен.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            if ("23505".equals(e.getSQLState())) {
                throw new IllegalStateException("Клиент с email " + email + " уже существует", e);
            }

            throw new RuntimeException("Ошибка при регистрации клиента", e);
        }
    }

    public Account openAccount(Long clientId, String accountNumber) {
        try (PreparedStatement statement = CONNECTION.prepareStatement(INSERT_ACCOUNT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, clientId);
            statement.setString(2, accountNumber);
            statement.setBigDecimal(3, BigDecimal.ZERO);
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    Long id = generatedKeys.getLong("id");
                    return new Account(id, clientId, accountNumber, BigDecimal.ZERO);
                } else {
                    throw new SQLException("Не удалось открыть счёт: ID не получен.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            if ("23503".equals(e.getSQLState())) {
                throw new ClientNotFoundException("Клиент с id=" + clientId + " не найден", e);
            }
            throw new RuntimeException("Ошибка при открытии счёта", e);
        }
    }

    public Account getAccount(Long accountId) {
        try (PreparedStatement statement = CONNECTION.prepareStatement(FIND_ACCOUNT_BY_ID)) {
            statement.setLong(1, accountId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Long clientId = resultSet.getLong("client_id");
                    String accountNumber = resultSet.getString("account_number");
                    BigDecimal balance = resultSet.getBigDecimal("balance");

                    return new Account(accountId, clientId, accountNumber, balance);
                } else {
                    throw new AccountNotFoundException("Не удалось найти счёт: ID не найден." + accountId);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Ошибка БД при поиске счёта с ID " + accountId, e);
        }
    }

    public Client getClient(Long clientId) {
        try (PreparedStatement statement = CONNECTION.prepareStatement(FIND_CLIENT_BY_ID)) {
            statement.setLong(1, clientId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String name = resultSet.getString("name");
                    String email = resultSet.getString("email");

                    return new Client(clientId, name, email);
                } else {
                    throw new ClientNotFoundException("Не удалось найти клиента: ID не найден " + clientId);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Ошибка БД при поиске клиента с ID " + clientId, e);
        }
    }

    public void transfer(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        Account from = getAccount(fromAccountId);

        if(amount == null ||amount.compareTo(BigDecimal.ZERO) <= 0){
            throw new IllegalArgumentException("Transfer amount should be positive");
        }
        if (from.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                    "Недостаточно средств на счёте " + fromAccountId +
                    ": баланс " + from.getBalance() + ", требуется " + amount);
        }

        updateBalance(fromAccountId, from.getBalance().subtract(amount));
        recordTransaction(fromAccountId, amount.negate(), "TRANSFER_OUT");

        Account to = getAccount(toAccountId); // если бросит исключение — деньги уже пропали
        
        updateBalance(toAccountId, to.getBalance().add(amount));
        recordTransaction(toAccountId, amount, "TRANSFER_IN");
    }

    private void recordTransaction(Long accountId, BigDecimal amount, String type) {
        try (PreparedStatement statement = CONNECTION.prepareStatement(INSERT_TRANSACTION)) {
            statement.setLong(1, accountId);
            statement.setBigDecimal(2, amount);
            statement.setString(3, type);
            statement.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));

            int rowsInserted = statement.executeUpdate();
            if (rowsInserted == 0) {
                throw new RuntimeException("Не удалось записать транзакцию по аккаунту: " + accountId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            if ("23503".equals(e.getSQLState())) {
                throw new AccountNotFoundException("Счёт с ID " + accountId + " не найден", e);
            }
            throw new RuntimeException("Ошибка БД при записи транзакции по аккаунту: " + accountId, e);
        }
    }

    public void deposit(Long accountId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сумма пополнения должна быть больше нуля");
        }
        Account account = getAccount(accountId);
        BigDecimal newBalance = account.getBalance().add(amount);
        updateBalance(accountId, newBalance);
        recordTransaction(accountId, amount, "TRANSFER_IN");
    }

    private void updateBalance(Long accountId, BigDecimal newBalance) {
        try (PreparedStatement statement = CONNECTION.prepareStatement(UPDATE_ACCOUNT_BALANCE_SQL)) {
            statement.setBigDecimal(1, newBalance);
            statement.setLong(2, accountId);

            int updated = statement.executeUpdate();
            if (updated == 0) {
                throw new AccountNotFoundException("Счёт с ID " + accountId + " не найден");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Ошибка БД при обновлении баланса счёта " + accountId, e);
        }
    }
}
