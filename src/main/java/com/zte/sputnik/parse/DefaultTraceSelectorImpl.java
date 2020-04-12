package com.zte.sputnik.parse;



import com.zte.sputnik.parse.annotation.TestSubject;
import com.zte.sputnik.parse.annotation.Trace;
import com.zte.sputnik.util.ClassUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DefaultTraceSelectorImpl implements TraceSelector {
    final Set<Class> annoClazz;

    public DefaultTraceSelectorImpl(Class... classes) {
        Set<Class> s = Stream.of(classes).collect(Collectors.toSet());
        this.annoClazz= Collections.unmodifiableSet(s);
    }

    @Override
    public boolean select(Class clazz) {
        return annoClazz.stream().anyMatch(c->ClassUtil.hasAnnotation(clazz,c));
    }

    @Override
    public boolean select(Field field) {
        return annoClazz.stream().anyMatch(c->ClassUtil.hasAnnotation(field,c));
    }

    @Override
    public boolean select(Parameter parameter) {
        return annoClazz.stream().anyMatch(c->ClassUtil.hasAnnotation(parameter,c));
    }
}
