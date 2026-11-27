## Запуск проекта

1. Создайте пустую базу данных `minibank` в PostgreSQL.
2. Скопируйте `src/main/resources/application.properties.example` в `src/main/resources/application.properties` и впишите свой пароль от БД.
3. Запустите метод `Main.main()` — таблицы, тестовые данные и индексы создадутся автоматически через Liquibase.

# Модуль 4
## Анализ производительности (EXPLAIN ANALYZE)

### До добавления индекса (Seq Scan)
![План выполнения запроса без индекса](docs/images/Sec%20Scan.png)

### После добавления составного индекса (Bitmap Index Scan)
![План выполнения запроса с индексом](docs/images/Bitmap%20Scan.png)

### Дополнительные запросы из презентации (слайд 30)
![Запросы из презентации](docs/images/Products%20SQL.png)

# Модуль 5
## Основные сценарии HTTP-запросов

### 200 — успех
![Перевод средств с одного аккаунта на другой](docs/images/transfer%20success.png)

### 400 — невалидная сумма
![В переводе указана отрицательная сумма](docs/images/invalid%20amount.png)

### 404 — счёт не найден
![В переводе указан несуществующий счёт](docs/images/account%20not%20found.png)

### 409 — недостаточно средств
![На счёте недостаточно средств](docs/images/insuficcient%20funds.png)

### 200 — успех GET
![Счёт найден](docs/images/get%20account%20success.png)

# Модуль 6

## Работа с JPA / Hibernate и оптимизация проблемы N+1

При переходе на Hibernate была проверена работа ленивой и жадной загрузки связей (`@OneToMany` / `@ManyToOne`) на примере запроса списка клиентов (`GET /clients`).

### Проблема N+1 (`FetchType.EAGER`)

### Тестовый запрос в Postman
![Запрос GET /clients в Postman](docs/images/get%20clients.png)

При использовании жадной загрузки (`FetchType.EAGER`) для получения списка из `N` клиентов Hibernate выполняет **1 основной запрос** к таблице `clients` и дополнительно **`N` отдельных `SELECT`-запросов** к таблице `accounts` (по одному на каждого клиента):
![Лог SQL-запросов с проблемой N+1](docs/images/n+1.png)

### Решение (`FetchType.LAZY` + `JOIN FETCH`)

### Тестовый запрос в Postman
![Запрос GET /clients в Postman](docs/images/join%20fetch%20postman.png)

После перевода связей на ленивую загрузку (`FetchType.LAZY`) и добавления метода с запросом join fetch Hibernate подгружает связанные счета, только если они требуются в ответе API. В результате выполняется **только 1 SQL-запрос** к таблице `clients`:
![Лог SQL-запросов после перехода на LAZY](docs/images/get%20clients%20join%20fetch.png)

### Удаление аккаунта (` orphanRemoval = true`)
При удалении аккаунта из коллекции клиента он удаляется из базы данных. Пока реализована только логика удаления аккаунта без транзакций и счёт с историей удалить пока нельзя из-за FK.
![Запрос DELETE /clients/665/accounts/2011 в Postman](docs/images/delete%20postman.png)
В базе данных больше нет аккаунта с ID 2011:
![Запрос DELETE accounts/2011 в Postman](docs/images/account%20not%20found%20after%20delete.png)
