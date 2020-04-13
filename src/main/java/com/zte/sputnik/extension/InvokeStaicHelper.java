package com.zte.sputnik.extension;

import com.zte.sputnik.lbs.LoggerBuilder;
import org.jboss.byteman.rule.Rule;
import shade.sputnik.org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InvokeStaicHelper {
    private static final Logger LOGGER = LoggerBuilder.of(InvokeStaicHelper.class);
    private static final Map<String, Object> INVOKE_STATIC_TTL = new ConcurrentHashMap<>();

    Rule rule;

    public InvokeStaicHelper(Rule rule) {
        this.rule = rule;
    }

    public static void setupMock(Map<String, Object> closureMap) {
        INVOKE_STATIC_TTL.putAll(closureMap);
    }


    public static void cleanup(Map<String, Object> closureMap) {
        for (String key : closureMap.keySet()) {
            INVOKE_STATIC_TTL.remove(key);
        }
    }

    public boolean hasMethod(String methodSignure) {

        return INVOKE_STATIC_TTL.containsKey(methodSignure);
    }

    public Object getMock(String methodSignure) {
        return INVOKE_STATIC_TTL.get(methodSignure);
    }
}
