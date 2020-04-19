package com.zte.sputnik.builder;

import com.zte.sputnik.lbs.LoggerBuilder;
import shade.sputnik.org.slf4j.Logger;

public class NoopSpecWriterImpl implements SpecWriter {
    private static final Logger LOGGER = LoggerBuilder.of(NoopSpecWriterImpl.class);

    @Override
    public void write(Long invocationId) {
        LOGGER.debug("write spec(noop):{}", invocationId);
    }
}
