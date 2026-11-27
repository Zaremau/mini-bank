package com.minibank.db;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.Map;

public class JpaUtil {

    private static final EntityManagerFactory EMF;

    static {
        Map<String, String> properties = Map.of(
                "jakarta.persistence.jdbc.url", ConnectionManager.getProperty("db.url"),
                "jakarta.persistence.jdbc.user", ConnectionManager.getProperty("db.user"),
                "jakarta.persistence.jdbc.password", ConnectionManager.getProperty("db.password")
        );
        EMF = Persistence.createEntityManagerFactory("minibank-pu", properties);
    }

    public static EntityManager getEntityManager() {
        return EMF.createEntityManager();
    }

    public static void close() {
        if (EMF != null && EMF.isOpen()) {
            EMF.close();
        }
    }
}
