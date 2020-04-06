package com.zte.sputnik.trace;

public interface TraceWriter {
    void write(ParamModel paramModel);

    void write(InvocationContext context);
}
