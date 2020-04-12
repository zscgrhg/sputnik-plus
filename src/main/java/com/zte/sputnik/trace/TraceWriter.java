package com.zte.sputnik.trace;

import com.zte.sputnik.parse.ValueObjectModel;

import java.util.Map;

public interface TraceWriter {
    void write(ParamModel paramModel);

    void write(InvocationContext context);

    void writeValues(Invocation owner, Map<String, ValueObjectModel> values);
}
