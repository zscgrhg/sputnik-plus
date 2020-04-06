package com.zte.sputnik.trace.proxy;

import lombok.SneakyThrows;
import org.springframework.aop.TargetClassAware;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class SpringProxyResolverImpl implements ProxyResolver {
    @Override
    public Class getTargetClass(Object candidate) {
        Object current = candidate;
        Class<?> result = null;
        while (current instanceof TargetClassAware) {
            result = ((TargetClassAware) current).getTargetClass();
            current = AopProxyUtils.getSingletonTarget(current);
        }
        if (result == null && AopUtils.isCglibProxy(candidate)) {
            return candidate.getClass().getSuperclass();
        }
        return null;
    }

    @Override
    @SneakyThrows
    public Object getTargetSource(Object candidate) {
        Object current = candidate;
        boolean found = false;
        while (current instanceof TargetClassAware) {
            found = true;
            current = AopProxyUtils.getSingletonTarget(current);
        }
        if (found) {
            return current;
        }
        if (!found && AopUtils.isCglibProxy(candidate)) {
            throw new RuntimeException("unknown");
        }
        return null;
    }

    @Override
    public boolean isProxy(Object proxy) {
        return AopUtils.isAopProxy(proxy);
    }

    @Override
    public Class findOwner(Object proxy, Method m) {
        Class targetClass = getTargetClass(proxy);
        if (targetClass != null) {
            return targetClass;
        }
        if (Proxy.isProxyClass(proxy.getClass())) {
            return ProxyResolver.findOwnerForJdkDynamicProxy(proxy, m);
        }
        return proxy.getClass();
    }
}
