package com.zte.sputnik.trace;

import com.zte.sputnik.config.SputnikConfig;
import com.zte.sputnik.instrument.MethodNames;
import com.zte.sputnik.lbs.LoggerBuilder;
import com.zte.sputnik.parse.SubjectManager;
import com.zte.sputnik.trace.proxy.ProxyResolver;
import lombok.SneakyThrows;

import org.jboss.byteman.rule.Rule;
import shade.sputnik.org.slf4j.Logger;


import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.stream.Stream;

import static com.zte.sputnik.util.ClassUtil.resolve;


public class TraceHelper {
    private static final Logger LOGGER = LoggerBuilder.of(TraceHelper.class);
    private static final ProxyResolver RESOLVER = SputnikConfig.INSTANCE.getProxyResolver();
    Rule rule;


    public TraceHelper(Rule rule) {
        this.rule = rule;
    }

    @SneakyThrows
    public void atEntry(Long mid, Object[] args) {
        LOGGER.debug(mid + ",trigger by " + rule.getName());
        Invocation invocation = new Invocation();

        MethodNames names = MethodNames.METHOD_NAMES_MAP.get(mid);
        invocation.mid = mid;
        invocation.names=names;
        InvocationContext context = InvocationContext.getCurrent(names);
        if (context!=null&&context.canPush()) {
            Object[] methodArgs = Stream.of(args).skip(1).toArray();
            invocation.setMethod(names.name);
            invocation.setSignature(names.signature);
            Object thisObject = args[0];
            invocation.setThisObject(thisObject);
            invocation.setThisObjectSource(thisObject);
            invocation.staticInvoke = thisObject == null;
            invocation.setClazzThis(thisObject == null ? names.context : thisObject.getClass());
            Class c = invocation.clazzThis;
            if (thisObject != null && RESOLVER.isProxy(thisObject)) {
                Object targetSource = RESOLVER.getTargetSource(thisObject);
                Class targetClass = RESOLVER.getTargetClass(thisObject);
                if (targetSource != null) {
                    invocation.thisObjectSource = targetSource;
                    c = targetSource.getClass();
                } else if (targetClass != null) {
                    c = targetClass;
                } else {
                    c = RESOLVER.findOwner(thisObject, names.method);
                }
            }
            invocation.clazzSource = c;
            invocation.subject = SubjectManager.isSubject(invocation.clazzSource);
            invocation.saveObjectsRef(names.genericSymbol, methodArgs);
            //Method method = c.getMethod(names.name, names.parametersType);
            invocation.genericReturned = resolve(names.method.getGenericReturnType(), c).getTypeName();
            invocation.genericArgs = getGenericArgs(names.method, c);
            context.push(invocation, methodArgs);
        }
    }

    /**
     * 拦截返回类型为void的方法
     * @param  mid
     * @param args
     */
    public void atExit(Long mid, Object[] args) {
        LOGGER.debug(mid + ",trigger by " + rule.getName());
        InvocationContext context = InvocationContext.getCurrent(false);
        if (context != null && context.canPop()) {
            Object[] methodArgs = Stream.of(args).skip(1).toArray();
            context.pop(null, null);
        }
    }

    /**
     * 拦截返回类型为非void的方法
     * @param  mid
     * @param args
     */
    public void atExit(Long mid, Object[] args, Object ret) {
        LOGGER.debug(mid + ",trigger by " + rule.getName());
        InvocationContext context = InvocationContext.getCurrent(false);
        if (context != null && context.canPop()) {
            Object[] methodArgs = Stream.of(args).skip(1).toArray();
            context.pop(ret, null);
        }
    }

    public void atException(Long mid, Object[] args, Throwable t) {
        LOGGER.error(mid + ",trigger by " + rule.getName(), t);
        InvocationContext context = InvocationContext.getCurrent(false);
        if (context != null && context.canPop()) {
            Object[] methodArgs = Stream.of(args).skip(1).toArray();
            context.pop(null, t);
        }
    }


    public String[] getGenericArgs(Method m, Class inheritorClass) {
        Type[] parameterTypes = m.getGenericParameterTypes();
        return Stream.of(parameterTypes).map(t -> resolve(t, inheritorClass).getTypeName()).toArray(String[]::new);
    }


}
