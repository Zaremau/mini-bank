package com.minibank.httphandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public final class JsonUtil {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static String toJson(Object object){
        try{
            return MAPPER.writeValueAsString(object);
        }catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T fromJson(String s, Class<T> clazz){
        try{
            return MAPPER.readValue(s, clazz);
        }catch (IOException e){
            throw new IllegalArgumentException("Невалидный JSON: " + e.getMessage());
        }
    }
}
