package com.zte.sputnik.trace;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.zte.sputnik.config.SputnikConfig;
import com.zte.sputnik.util.JsonUtil;
import lombok.SneakyThrows;


import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class TraceReaderImpl implements TraceReader {
    private File getWorkDir() {
        return SputnikConfig.INSTANCE.getTraceOutputsDir();
    }

    @SneakyThrows
    private JsonNode readFile(String fileName) {
        Path input = getWorkDir().toPath().resolve(fileName);
        byte[] bytes = Files.readAllBytes(input);
        JsonNode jsonNode = JsonUtil.readTree(bytes);
        return jsonNode;
    }

    @SneakyThrows
    private <T> T readFile(TypeReference<T> clazz, String fileName) {
        byte[] bytes = Files.readAllBytes(getWorkDir().toPath().resolve(fileName));
        T model = JsonUtil.readerFor(clazz, bytes);
        return model;
    }

    @SneakyThrows
    private <T> T readFile(Class<T> clazz, String fileName) {
        byte[] bytes = Files.readAllBytes(getWorkDir().toPath().resolve(fileName));
        T model = JsonUtil.readerFor(clazz, bytes);
        return model;
    }

    @Override
    @SneakyThrows
    public JsonNode readInParam(Long invocationId) {
        return readFile(invocationId + ".in.json");
    }

    @SneakyThrows
    @Override
    public JsonNode readOutParam(Long invocationId) {
        return readFile(invocationId + ".out.json");
    }


    @Override
    public Invocation readInvocation(Long invocationId) {
        return readFile(Invocation.class, invocationId + ".subject.json");
    }

    @Override
    public JsonNode readValues(Long invocationId) {
        return readFile(invocationId + ".values.json");
    }
}
