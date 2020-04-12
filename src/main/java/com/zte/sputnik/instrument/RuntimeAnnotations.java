package com.zte.sputnik.instrument;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

public class RuntimeAnnotations {
    public static final Constructor<?> ANNOTATION_INVOCATION_HANDLER_CONSTRUCTOR;

    static {
        try {
            Class<?> annoInvocationHandlerClass = Class.forName("sun.reflect.annotation.AnnotationInvocationHandler");
            Constructor<?> annoInvocationHandlerConstructor = annoInvocationHandlerClass.getDeclaredConstructor(
                    new Class[]{Class.class, Map.class}
            );
            annoInvocationHandlerConstructor.setAccessible(true);
            ANNOTATION_INVOCATION_HANDLER_CONSTRUCTOR = annoInvocationHandlerConstructor;
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public static <T extends Annotation> T buildFromMap(Class<T> annoClass, Map<String, Object> values) {
        return (T) AccessController.doPrivileged(new PrivilegedAction<Annotation>() {
            @Override
            public Annotation run() {

                try {
                    InvocationHandler handler = (InvocationHandler) ANNOTATION_INVOCATION_HANDLER_CONSTRUCTOR.newInstance(annoClass, new HashMap<>(values));

                    return (Annotation) Proxy.newProxyInstance(annoClass.getClassLoader(),
                            new Class[]{annoClass}, handler);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    throw new IllegalStateException(e);
                }

            }
        });

    }
}
