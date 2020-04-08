package com.zte.sputnik.config;

import com.alibaba.ttl.TransmittableThreadLocal;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class StaticInvoker {
    private static final TransmittableThreadLocal<Map<String, Object>> INVOKE_STATIC_TTL = new TransmittableThreadLocal();

    public static boolean hasMethod(String methodSignure) {
        return Optional.ofNullable(INVOKE_STATIC_TTL.get()).orElse(Collections.emptyMap()).containsKey(methodSignure);
    }

    public static void setupMock( Map<String, Object> closureMap) {
        INVOKE_STATIC_TTL.set(closureMap);
    }

    public static Object getMock(String methodSignure) {
        return Optional.ofNullable(INVOKE_STATIC_TTL.get()).map(m -> m.get(methodSignure))
        .orElse(null);
    }

    public static void cleanup(){
        INVOKE_STATIC_TTL.remove();
    }
}
