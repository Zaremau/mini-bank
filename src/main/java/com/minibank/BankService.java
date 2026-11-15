package com.minibank;

import com.minibank.db.ConnectionManager;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

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
    private static final Connection CONNECTION;

    static {
        try {
            CONNECTION = ConnectionManager.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException("Не удалось установить соединение с базой данных", e);
        }
    }

    private static final String INSERT_CLIENT_SQL = """
            INSERT INTO clients (name, email)
            VALUES (?, ?)
            """;
    private static final String INSERT_ACCOUNT_SQL = """
            INSERT INTO accounts (client_id, account_number, balance)
            VALUES(?,?,?)
            """;

    private static final String FIND_ACCOUNT_BY_ID = """
            SELECT id, client_id, account_number, balance
            FROM accounts
            WHERE id = ?
            """;

    private static final String FIND_CLIENT_BY_ID = """
            SELECT id, name, email
            FROM clients
            WHERE id = ?
            """;
    private static final String INSERT_TRANSACTION = """
            INSERT INTO transactions (account_id, amount, type, transaction_date)
            VALUES(?,?,?,?)
            """;

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
            if ("23505".equals(e.getSQLState())) {
                throw new IllegalArgumentException("Клиент с email " + email + " уже существует", e);
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
            if ("23503".equals(e.getSQLState())) {
                throw new IllegalArgumentException("Клиент с id=" + clientId + " не найден", e);
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
                    throw new IllegalArgumentException("Не удалось найти счёт: ID не найден." + accountId);
                }
            }
        } catch (SQLException e) {
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
                    throw new IllegalArgumentException("Не удалось найти клиента: ID не найдет " + clientId);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Ошибка БД при поиске клиента с ID " + clientId, e);
        }
    }

    public void transfer(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        Account from = getAccount(fromAccountId);

        // Списываем деньги ДО того, как проверили существование счёта-
        // получателя — если toAccountId не существует, деньги уже
        // списаны и потеряны.
        if (from.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient funds on account " + fromAccountId);
        }
        from.setBalance(from.getBalance().subtract(amount));
        recordTransaction(fromAccountId, amount.negate(), "TRANSFER_OUT");

        Account to = getAccount(toAccountId); // если бросит исключение — деньги уже пропали

        to.setBalance(to.getBalance().add(amount));
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
            if ("23503".equals(e.getSQLState())) {
                throw new IllegalArgumentException("Счёт с ID " + accountId + " не найден", e);
            }
            throw new RuntimeException("Ошибка БД при записи транзакции по аккаунту: " + accountId, e);
        }
    }
}
