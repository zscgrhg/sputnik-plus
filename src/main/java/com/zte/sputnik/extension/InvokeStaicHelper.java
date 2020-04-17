package com.zte.sputnik.extension;

import com.zte.sputnik.lbs.LoggerBuilder;
import groovy.lang.Closure;
import org.jboss.byteman.rule.Rule;
import org.spockframework.mock.runtime.MockInvocation;
import shade.sputnik.org.slf4j.Logger;

import java.lang.reflect.Field;
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

    public boolean hasMethod(String methodSignure,Object[] args) {
        if(args!=null&&args.length>1){
            if(isMock(args[0])){
                return false;
            }
        }
        return INVOKE_STATIC_TTL.containsKey(methodSignure);
    }

    public boolean isMock(Object target){
        try {
            if(target!=null&&target instanceof Closure){
                Field delegate = Closure.class.getDeclaredField("delegate");
                delegate.setAccessible(true);
                Object obj = delegate.get(target);
                return obj!=null&&obj instanceof MockInvocation;
            }
            return false;
        } catch (NoSuchFieldException|IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    public Object getMock(String methodSignure) {
        return INVOKE_STATIC_TTL.get(methodSignure);
    }
}
