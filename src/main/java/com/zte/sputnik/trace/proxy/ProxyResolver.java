package com.zte.sputnik.trace.proxy;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.stream.Stream;

public interface ProxyResolver {


    static Class findOwnerForJdkDynamicProxy(Object proxyObject, Method method) {
        Class proxy = proxyObject.getClass();
        Type[] genericInterfaces = proxy.getInterfaces();
        for (Type type : genericInterfaces) {
            assert type instanceof Class;
            Class clazz = (Class) type;
            boolean anyMatch = Stream.of(clazz.getMethods()).anyMatch(method::equals);
            if (anyMatch) {
                return clazz;
            }
        }
        return proxy;
    }

    static boolean isOwnerOf(Class clazz, Method method) {
        return Stream.of(clazz.getMethods()).anyMatch(method::equals);
    }

    Class getTargetClass(Object proxy);

    Object getTargetSource(Object proxy);

    boolean isProxy(Object proxy);

    Class findOwner(Object proxy, Method m);
}
