package com.zte.sputnik.parse;


import com.zte.sputnik.parse.annotation.TestSubject;
import com.zte.sputnik.util.ClassUtil;

public class DefaultTestSubjectSelectorImpl implements TestSubjectSelector {
    @Override
    public boolean selectTestSubject(Class clazz) {
        return ClassUtil.hasAnnotation(clazz, TestSubject.class);
    }


}
