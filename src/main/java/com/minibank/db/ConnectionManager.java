package com.minibank.db;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class ConnectionManager {

    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream input = ConnectionManager.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new RuntimeException(
                        "Файл application.properties не найден! Скопируйте application.properties.example в application.properties и укажите пароль от БД."
                );
            }
            PROPERTIES.load(input);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка загрузки настроек БД", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        String url = getConfigValue("DB_URL", "db.url");
        String user = getConfigValue("DB_USER", "db.user");
        String password = getConfigValue("DB_PASSWORD", "db.password");

        return DriverManager.getConnection(url, user, password);
    }

    public static String getProperty(String key) {
        return PROPERTIES.getProperty(key);
    }

    private static String getConfigValue(String envKey, String propKey) {
        String envValue = System.getenv(envKey);
        return (envValue != null && !envValue.isBlank()) ? envValue : PROPERTIES.getProperty(propKey);
    }
}

