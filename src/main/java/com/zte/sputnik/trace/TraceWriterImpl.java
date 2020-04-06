package com.zte.sputnik.trace;

import com.zte.sputnik.config.SputnikConfig;
import com.zte.sputnik.lbs.LoggerBuilder;
import com.zte.sputnik.util.JsonUtil;
import lombok.SneakyThrows;
import shade.sputnik.org.slf4j.Logger;


import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class TraceWriterImpl implements TraceWriter {

    private static final Logger LOGGER = LoggerBuilder.of(TraceWriterImpl.class);
    @Override
    @SneakyThrows
    public void write(ParamModel paramModel) {
        Path file = SputnikConfig.INSTANCE.getTraceOutputsDir().toPath().resolve(paramModel.invocationId + "." + paramModel.name + ".json");
        LOGGER.debug("write:" + file);
        Files.copy(new ByteArrayInputStream(JsonUtil.write(paramModel).getBytes("UTF8")),
                file,
                StandardCopyOption.REPLACE_EXISTING);
    }

    @SneakyThrows
    @Override
    public void write(InvocationContext context) {

        List<Invocation> nodes = context.getNodes();
        for (Invocation node : nodes) {
            Path file = SputnikConfig.INSTANCE.getTraceOutputsDir().toPath().resolve(node.id + ".subject.json");
            LOGGER.debug("write:" + file);
            Files.copy(new ByteArrayInputStream(JsonUtil.write(node).getBytes("UTF8")),
                    file,
                    StandardCopyOption.REPLACE_EXISTING);
        }

    }

}
