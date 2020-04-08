package com.zte.sputnik.config;

import com.alibaba.ttl.TransmittableThreadLocal;
import groovy.lang.Closure;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class StaticInvoker {
    private static final TransmittableThreadLocal<Map<String, Closure>> INVOKE_STATIC_TTL = new TransmittableThreadLocal();

    public static boolean hasMethod(String methodSignure) {
        return Optional.ofNullable(INVOKE_STATIC_TTL.get()).orElse(Collections.emptyMap()).containsKey(methodSignure);
    }

    public static void setupMock( Map<String, Closure> closureMap) {
        INVOKE_STATIC_TTL.set(closureMap);
    }

    public static Object invokeStatic(String methodSignure, Object[] args) {
        return Optional.ofNullable(INVOKE_STATIC_TTL.get()).map(m -> m.get(methodSignure))
                .map(c -> c.call(args)).orElse(null);
    }

    public static void cleanup(){
        INVOKE_STATIC_TTL.remove();
    }
}
