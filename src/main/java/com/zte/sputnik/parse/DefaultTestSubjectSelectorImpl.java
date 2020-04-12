package com.zte.sputnik.parse;


import com.zte.sputnik.parse.annotation.TestSubject;
import com.zte.sputnik.util.ClassUtil;

public class DefaultTestSubjectSelectorImpl implements TestSubjectSelector {
    final Class annoClazz;

    public DefaultTestSubjectSelectorImpl(Class annoClazz) {
        this.annoClazz = annoClazz;
    }

    @Override
    public boolean selectTestSubject(Class clazz) {
        return ClassUtil.hasAnnotation(clazz, annoClazz);
    }


}
