package com.zte.sputnik.extension;

import com.alibaba.ttl.TransmittableThreadLocal;
import org.jboss.byteman.rule.Rule;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class InvokeStaicHelper {
    private static final TransmittableThreadLocal<Map<String, Object>> INVOKE_STATIC_TTL = new TransmittableThreadLocal();

    Rule rule;

    public InvokeStaicHelper(Rule rule) {
        this.rule = rule;
    }

    public static void setupMock(Map<String, Object> closureMap) {
        INVOKE_STATIC_TTL.set(closureMap);
    }



    public static void cleanup(){
        INVOKE_STATIC_TTL.remove();
    }

    public  boolean hasMethod(String methodSignure) {
        return Optional.ofNullable(INVOKE_STATIC_TTL.get()).orElse(Collections.emptyMap()).containsKey(methodSignure);
    }

    public  Object getMock(String methodSignure) {
        return Optional.ofNullable(INVOKE_STATIC_TTL.get()).map(m -> m.get(methodSignure))
                .orElse(null);
    }
}
