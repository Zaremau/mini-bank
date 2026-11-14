--liquibase formatted sql

--changeset dev:2-seed-test-data
INSERT INTO clients (name, email)
SELECT
    'Client ' || i,
    'client' || i || '@example.com'
FROM generate_series(1, 1000) AS s(i);

INSERT INTO accounts (client_id, account_number, balance)
SELECT
    (floor(random() * 1000) + 1)::bigint,
    'RU' || lpad(i::text, 20, '0'),
    round((random() * 100000)::numeric, 2)
FROM generate_series(1, 2000) AS s(i);

INSERT INTO transactions (account_id, amount, type, transaction_date)
SELECT
    (floor(random() * 2000) + 1)::bigint,
    round((random() * 10000 + 1)::numeric, 2),
    CASE WHEN random() < 0.5 THEN 'TRANSFER_IN' ELSE 'TRANSFER_OUT' END,
    now() - (random() * interval '365 days')
FROM generate_series(1, 60000) AS s(i);
