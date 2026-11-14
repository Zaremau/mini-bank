package com.minibank.db;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

import java.sql.Connection;

public class LiquibaseRunner {

    public static void runMigrations() {
        String changelogPath = ConnectionManager.getProperty("liquibase.changelog");

        try (Connection connection = ConnectionManager.getConnection()) {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));

            try (Liquibase liquibase = new Liquibase(
                    changelogPath,
                    new ClassLoaderResourceAccessor(),
                    database)) {

                liquibase.update();
            }
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при выполнении миграций Liquibase", e);
        }
    }
}
