package com.zte.sputnik.trace;

import java.util.Map;

public interface TraceWriter {
    void write(ParamModel paramModel);

    void write(InvocationContext context);
    void write(Invocation invocation);
    void writeValues(Invocation owner, Map<String, ValueObjectModel> values);
}
