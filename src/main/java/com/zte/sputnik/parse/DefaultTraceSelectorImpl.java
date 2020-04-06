package com.zte.sputnik.parse;



import com.zte.sputnik.parse.annotation.TestSubject;
import com.zte.sputnik.parse.annotation.Trace;
import com.zte.sputnik.util.ClassUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Parameter;

public class DefaultTraceSelectorImpl implements TraceSelector {
    @Override
    public boolean select(Class clazz) {
        return ClassUtil.hasAnnotation(clazz, Trace.class) || ClassUtil.hasAnnotation(clazz, TestSubject.class);
    }

    @Override
    public boolean select(Field field) {
        return ClassUtil.hasAnnotation(field, Trace.class);
    }

    @Override
    public boolean select(Parameter parameter) {
        return ClassUtil.hasAnnotation(parameter, Trace.class);
    }
}
