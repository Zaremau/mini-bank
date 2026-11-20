package com.minibank.httphandler;

import com.minibank.exception.AccountNotFoundException;
import com.minibank.exception.ClientNotFoundException;
import com.minibank.exception.ErrorResponse;
import com.minibank.exception.InsufficientFundsException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public abstract class BaseHttpHandler implements HttpHandler {

    @FunctionalInterface
    protected interface RouteHandler{
        void handle(HttpExchange exchange) throws Exception;
    }

    private final Map<String, RouteHandler> routes = new HashMap<>();

    protected void registerRoute(String method, String pathPattern, RouteHandler handler){
        routes.put(method.toUpperCase() + " " + pathPattern, handler);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException{
        String method = exchange.getRequestMethod().toUpperCase();
        String path = normalizePath(exchange.getRequestURI().getPath());
        String routeKey = method + " " + path;

        try{
            RouteHandler handler = routes.get(routeKey);
            if(handler != null){
                handler.handle(exchange);
            }else {
                sendJson(exchange, 404, new ErrorResponse("Endpoint not found"));
            }
        }catch (AccountNotFoundException | ClientNotFoundException e) {
            sendJson(exchange, 404, new ErrorResponse(e.getMessage()));
        } catch (InsufficientFundsException | IllegalStateException e) {
            sendJson(exchange, 409, new ErrorResponse(e.getMessage()));
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, new ErrorResponse("Internal server error"));
        }
    }

    protected Long extractId(HttpExchange exchange){
        String[] parts = exchange.getRequestURI().getPath().split("/");
        for(String part : parts){
            if(part.matches("\\d+")){
                return Long.parseLong(part);
            }
        }
        throw new IllegalArgumentException("ID в URL не найден");
    }

    protected String readBody(HttpExchange exchange) throws IOException{
        try (InputStream is = exchange.getRequestBody()){
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    protected void sendJson(HttpExchange exchange, int statusCode, Object body) throws IOException {
        byte [] bytes = JsonUtil.toJson(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try(OutputStream os = exchange.getResponseBody()){
            os.write(bytes);
        }
    }

    private String normalizePath(String path) {
        return path.replaceAll("/\\d+", "/{id}");
    }
}
