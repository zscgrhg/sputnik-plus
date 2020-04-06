package com.zte.sputnik.parse.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Trace
@Target({ElementType.TYPE})
public @interface TestSubject {
}