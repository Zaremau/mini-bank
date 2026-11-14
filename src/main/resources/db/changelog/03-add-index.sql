--liquibase formatted sql

--changeset dev:3-add-indexes
CREATE INDEX idx_transactions_account_id_date
    ON transactions (account_id, transaction_date DESC);