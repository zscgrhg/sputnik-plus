package com.zte.sputnik.parse;

import java.lang.reflect.Field;
import java.lang.reflect.Parameter;

public interface TraceSelector {
    boolean select(Class clazz);

    boolean select(Field field);

    boolean select(Parameter parameter);
}
