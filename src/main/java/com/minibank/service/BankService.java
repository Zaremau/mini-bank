package com.minibank.service;

import com.minibank.db.ConnectionManager;
import com.minibank.db.JpaUtil;
import com.minibank.dto.AccountResponse;
import com.minibank.dto.ClientResponse;
import com.minibank.dto.ClientWithAccountsResponse;
import com.minibank.exception.AccountNotFoundException;
import com.minibank.exception.ClientNotFoundException;
import com.minibank.exception.InsufficientFundsException;
import com.minibank.model.Account;
import com.minibank.model.Client;
import com.minibank.model.Transaction;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.hibernate.exception.ConstraintViolationException;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

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


    public ClientResponse registerClient(String name, String email) {
        try (EntityManager em = JpaUtil.getEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                Client client = new Client(null, name, email);
                em.persist(client);
                tx.commit();
                return ClientResponse.from(client);
            } catch (Exception e) {
                if (tx.isActive()) tx.rollback();
                if (e instanceof ConstraintViolationException ||
                        e.getCause() instanceof ConstraintViolationException) {
                    throw new IllegalStateException("Клиент с email " + email + " уже существует", e);
                }
                throw new RuntimeException("Ошибка при регистрации клиента", e);
            }
        }
    }

    public AccountResponse openAccount(Long clientId, String accountNumber) {
        try (EntityManager em = JpaUtil.getEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                Client client = em.find(Client.class, clientId);
                if (client == null) {
                    throw new ClientNotFoundException("Клиент с id=" + clientId + " не найден");
                }
                Account account = new Account(null, client, accountNumber, BigDecimal.ZERO);
                em.persist(account);
                tx.commit();
                return AccountResponse.from(account);
            } catch (ClientNotFoundException e) {
                if (tx.isActive()) tx.rollback();
                throw e;
            }catch (Exception e) {
                if (tx.isActive()) tx.rollback();
                if (e instanceof ConstraintViolationException ||
                        e.getCause() instanceof ConstraintViolationException) {
                    throw new IllegalStateException("Аккаунт с номером" + accountNumber + " уже существует", e);
                }
                throw new RuntimeException("Ошибка при регистрации аккаунта", e);
            }
        }
    }

    public Account getAccount(Long accountId) {
        try (EntityManager em = JpaUtil.getEntityManager()) {
            return em.createQuery(
                            "select a from Account a join fetch a.client where a.id = :id",
                            Account.class
                    )
                    .setParameter("id", accountId)
                    .getResultStream()
                    .findFirst()
                    .orElseThrow(() -> new AccountNotFoundException("Не удалось найти счёт: ID не найден " + accountId));
        }
    }


    public ClientResponse getClient(Long clientId) {
        try (EntityManager em = JpaUtil.getEntityManager()) {
            Client client = em.find(Client.class, clientId);
            if (client == null) {
                throw new ClientNotFoundException("Клиент с id=" + clientId + " не найден");
            }
            return ClientResponse.from(client);
        }
    }

    public ClientWithAccountsResponse getClientWithAccounts(Long id) {
        try (EntityManager em = JpaUtil.getEntityManager()) {
            Client client = em.createQuery(
                            "select c from Client c left join fetch c.accounts where c.id = :id",
                            Client.class
                    )
                    .setParameter("id", id)
                    .getResultStream()
                    .findFirst()
                    .orElseThrow(() -> new ClientNotFoundException("Клиент не найден"));

            List<AccountResponse> accountDtos = client.getAccounts().stream()
                    .map(account ->
                            new AccountResponse(
                                    account.getId(),
                                    account.getClient().getId(),
                                    account.getAccountNumber(),
                                    account.getBalance()))
                    .toList();

            return new ClientWithAccountsResponse(
                    client.getId(),
                    client.getName(),
                    accountDtos
            );
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
        try (EntityManager em = JpaUtil.getEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();

                Account account = em.find(Account.class, accountId);
                if (account == null) {
                    throw new AccountNotFoundException("Счёт с ID " + accountId + " не найден");
                }

                Transaction transaction = new Transaction(
                        null,
                        account,
                        amount,
                        type,
                        LocalDateTime.now()
                );
                em.persist(transaction);

                tx.commit();
            } catch (AccountNotFoundException e) {
                if (tx.isActive()) tx.rollback();
                throw e;
            } catch (Exception e) {
                if (tx.isActive()) tx.rollback();
                throw new RuntimeException("Ошибка БД при записи транзакции по аккаунту: " + accountId, e);
            }
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
        try (EntityManager em = JpaUtil.getEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();

                Account account = em.find(Account.class, accountId);
                if (account == null) {
                    throw new AccountNotFoundException("Счёт с ID " + accountId + " не найден");
                }
                account.setBalance(newBalance);

                tx.commit();
            } catch (AccountNotFoundException e) {
                if (tx.isActive()) tx.rollback();
                throw e;
            } catch (Exception e) {
                if (tx.isActive()) tx.rollback();
                throw new RuntimeException("Ошибка БД при обновлении баланса счёта " + accountId, e);
            }
        }
    }

    public List<ClientResponse> getAllClients() {
        try (EntityManager em = JpaUtil.getEntityManager()) {
            return em.createQuery("SELECT c FROM Client c ORDER BY c.id", Client.class)
                    .setMaxResults(5)
                    .getResultList()
                    .stream()
                    .map(ClientResponse::from).toList();
        }
    }

    public List<ClientWithAccountsResponse> getAllClientsWithAccounts(){
        try(EntityManager em = JpaUtil.getEntityManager()){
            List<Client> clients = em.createQuery(
                    "select distinct c from Client c left join fetch c.accounts",
                    Client.class)
                    .getResultList();

            return clients.stream()
                    .map(ClientWithAccountsResponse::from).toList();
        }
    }

    public void removeAccountFromClient(Long clientId, Long accountId) {
        try (EntityManager em = JpaUtil.getEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();

                Client client = em.createQuery(
                                "select c from Client c left join fetch c.accounts where c.id = :id",
                                Client.class
                        )
                        .setParameter("id", clientId)
                        .getResultStream()
                        .findFirst()
                        .orElseThrow(() -> new ClientNotFoundException("Client not found"));

                boolean removed = client.getAccounts()
                        .removeIf(account -> account.getId().equals(accountId));

                if (!removed) {
                    throw new AccountNotFoundException("Счёт с ID " + accountId + " не найден");
                }

                tx.commit();
            } catch (AccountNotFoundException | ClientNotFoundException e) {
                if (tx.isActive()) tx.rollback();
                throw e;
            } catch (Exception e) {
                if (tx.isActive()) tx.rollback();
                throw new RuntimeException("Ошибка БД при удалении аккаунта " + accountId, e);
            }
        }
    }
}
