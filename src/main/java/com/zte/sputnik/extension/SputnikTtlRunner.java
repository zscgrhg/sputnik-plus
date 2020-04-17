package com.zte.sputnik.extension;

import com.alibaba.ttl.threadpool.agent.TtlAgent;
import com.zte.sputnik.SputnikMain;
import com.zte.sputnik.lbs.LoggerBuilder;
import org.junit.runners.model.InitializationError;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import shade.sputnik.org.slf4j.Logger;

public class SputnikTtlRunner extends SpringJUnit4ClassRunner {
    private static final Logger LOGGER= LoggerBuilder.of(SputnikTtlRunner.class);
    public SputnikTtlRunner(Class<?> clazz) throws InitializationError {
        super(clazz);
        if(!TtlAgent.isTtlAgentLoaded()){
            throw new IllegalStateException("TtlAgent not loaded");
        }
    }

    static {
        SputnikMain.loadAgent();
    }

}
