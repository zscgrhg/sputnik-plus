package com.zte.sputnik.trace.proxy;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class DefaultProxyResolverImpl implements ProxyResolver {

    @Override
    public Class getTargetClass(Object proxy) {
        return null;
    }

    @Override
    public Object getTargetSource(Object proxy) {
        return null;
    }

    @Override
    public boolean isProxy(Object proxy) {
        return Proxy.isProxyClass(proxy.getClass());
    }

    @Override
    public Class findOwner(Object proxyObject, Method method) {
        return ProxyResolver.findOwnerForJdkDynamicProxy(proxyObject, method);
    }
}
