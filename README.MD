## Запуск проекта

1. Создайте пустую базу данных `minibank` в PostgreSQL.
2. Скопируйте `src/main/resources/application.properties.example` в `src/main/resources/application.properties` и впишите свой пароль от БД.
3. Запустите метод `Main.main()` — таблицы, тестовые данные и индексы создадутся автоматически через Liquibase.

## Анализ производительности (EXPLAIN ANALYZE)

### До добавления индекса (Seq Scan)
![План выполнения запроса без индекса](docs/images/Sec%20Scan.png)

### После добавления составного индекса (Bitmap Index Scan)
![План выполнения запроса с индексом](docs/images/Bitmap%20Scan.png)

### Дополнительные запросы из презентации (слайд 30)
![Запросы из презентации](docs/images/Products%20SQL.png)