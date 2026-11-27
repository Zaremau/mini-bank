package com.minibank.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne()
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "account_number", nullable = false, unique = true, length = 34)
    private String accountNumber;

    @Column(name = "balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    public Account() {
    }

    public Account(Long id, Client client, String accountNumber, BigDecimal balance) {
        this.id = id;
        this.client = client;
        this.accountNumber = accountNumber;
        this.balance = balance;
    }

    public Long getId() {
        return id;
    }

    public Client getClient() {
        return client;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof Account account)) return false;
        if (accountNumber == null || account.accountNumber == null) return false;
        return Objects.equals(accountNumber, account.getAccountNumber());
    }

    @Override
    public int hashCode() {
        return accountNumber == null ? 0 : accountNumber.hashCode();
    }
}

