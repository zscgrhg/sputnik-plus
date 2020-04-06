package com.zte.sputnik.builder;

import com.alibaba.ttl.threadpool.agent.TtlAgent;

import com.zte.sputnik.Sputnik;
import com.zte.sputnik.lbs.LoggerBuilder;
import com.zte.sputnik.trace.InvocationContext;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import shade.sputnik.org.slf4j.Logger;


import java.util.ArrayList;
import java.util.List;

public class SputnikWatcher extends TestWatcher implements SpecWriter {
    private static final Logger LOGGER = LoggerBuilder.of(InvocationContext.class);

    private static final ThreadLocal<List<Long>> CACHE = new ThreadLocal<List<Long>>();

    static {
        if (!TtlAgent.isTtlAgentLoaded()) {
            LOGGER.debug("load alitababa");
            try {
                Sputnik.loadAgent();
                Sputnik.loadTtlAgent();
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

    }

    @Override
    protected void succeeded(Description description) {
        super.succeeded(description);
        LOGGER.debug(description.getDisplayName() + " succeeded");
        List<Long> invocations = CACHE.get();
        if (invocations != null) {
            for (Long invocation : invocations) {
                SpecFactory.writeSpec(invocation);
            }
        }
    }

    @Override
    protected void failed(Throwable e, Description description) {
        super.failed(e, description);
        LOGGER.debug(description.getDisplayName() + " failed");
    }

    @Override
    protected void starting(Description description) {
        LOGGER.debug(description.getDisplayName() + " starting");
        super.starting(description);
    }

    @Override
    protected void finished(Description description) {
        super.finished(description);
        LOGGER.debug(description.getDisplayName() + " finished");
        CACHE.remove();
    }


    @Override
    public void write(Long invocationId) {
        if (CACHE.get() == null) {
            CACHE.set(new ArrayList<>());
        }
        CACHE.get().add(invocationId);
    }
}
