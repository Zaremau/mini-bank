package com.minibank;

import com.minibank.db.JpaUtil;
import com.minibank.db.LiquibaseRunner;
import com.minibank.httphandler.AccountHandler;
import com.minibank.httphandler.TransferHandler;
import com.minibank.service.BankService;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class WebRunner {
    public static void main(String[] args) throws IOException {
        LiquibaseRunner.runMigrations();
        BankService bankService = new BankService();

        HttpServer server = HttpServer.create(new InetSocketAddress(8080),0);
        server.createContext("/clients", new ClientHandler(bankService));
        server.createContext("/accounts", new AccountHandler(bankService));
        server.createContext("/transfer", new TransferHandler(bankService));

        server.start();
        System.out.println("MiniBank HTTP Server запущен на порту 8080");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Останавливаю сервер...");
            server.stop(2);
            JpaUtil.close();
        }));
    }
}
