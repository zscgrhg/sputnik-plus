package com.zte.sputnik.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.zte.sputnik.trace.Invocation;
import lombok.SneakyThrows;


public class JsonUtil {

    static ObjectMapper MAPPER = new ObjectMapper()
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false);

    @SneakyThrows
    public static String write(Object obj) {
        return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
    }

    @SneakyThrows
    public static JsonNode readTree(byte[] bytes) {
        return MAPPER.readTree(bytes);
    }

    @SneakyThrows
    public static <T> T readerFor(Class<T> clazz, byte[] bytes) {
        return MAPPER.readerFor(Invocation.class).readValue(bytes);
    }

    @SneakyThrows
    public static <T> T readerFor(TypeReference<T> tTypeReference, String data) {
        return MAPPER.readerFor(tTypeReference).readValue(data);
    }
    @SneakyThrows
    public static <T> T readerFor(TypeReference<T> tTypeReference, byte[] data) {
        return MAPPER.readerFor(tTypeReference).readValue(data);
    }
    @SneakyThrows
    public static <T> T convert(TypeReference<T> tTypeReference, Object data) {
        return MAPPER.convertValue(data,tTypeReference);
    }

    @SneakyThrows
    public static <T> T convert(Class<T> clazz, Object data) {
        return MAPPER.convertValue(data,clazz);
    }
}
