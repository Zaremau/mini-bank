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