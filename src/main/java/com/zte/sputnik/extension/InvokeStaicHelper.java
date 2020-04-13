package com.zte.sputnik.extension;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.zte.sputnik.instrument.BMUtil;
import com.zte.sputnik.lbs.LoggerBuilder;
import com.zte.sputnik.util.JsonUtil;
import org.jboss.byteman.rule.Rule;
import shade.sputnik.org.slf4j.Logger;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class InvokeStaicHelper {
    private static final Logger LOGGER = LoggerBuilder.of(InvokeStaicHelper.class);
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
        Map<String, Object> mocks = Optional.ofNullable(INVOKE_STATIC_TTL.get()).orElse(Collections.emptyMap());

        boolean b = mocks.containsKey(methodSignure);
        LOGGER.debug("has method {} : {}", b,methodSignure);

        if(!b){
            LOGGER.debug("not match has method {} : {}", b,methodSignure);
            LOGGER.debug("mocks {} \n", mocks.keySet().stream().collect(Collectors.joining("\n")));
            LOGGER.debug("methodSignure: {} \n", methodSignure);
        }
        return b;
    }

    public  Object getMock(String methodSignure) {
        return Optional.ofNullable(INVOKE_STATIC_TTL.get()).map(m -> m.get(methodSignure))
                .orElse(null);
    }
}
