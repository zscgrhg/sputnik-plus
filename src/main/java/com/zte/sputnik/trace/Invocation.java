package com.zte.sputnik.trace;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zte.sputnik.config.SputnikConfig;
import com.zte.sputnik.instrument.MethodNames;
import com.zte.sputnik.parse.RefsInfo;
import com.zte.sputnik.parse.SubjectManager;
import com.zte.sputnik.trace.proxy.ProxyResolver;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.ToString;


import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


/**
 * The type Invocation.
 */
@Data
@ToString(exclude = {
        "refs",
        "thisObject",
        "thisObjectSource",
        "parent",
        "children",
})
public class Invocation {
    /**
     * ID生成器，作为一次方法调用的唯一标识
     */
    public static final AtomicLong INVOCATION_INCR = new AtomicLong(1);

    /**
     * The Id.
     */
    public final Long id = INVOCATION_INCR.getAndIncrement();
    /**
     * 调用方法的线程ID
     */
    public final long threadId = Thread.currentThread().getId();
    /**
     * @{link java.lang.reflect.Method} 唯一标识
     */
    public Long mid;
    public MethodNames names;
    /**
     * The Refs. 用于解析对象引用关系
     * @see Invocation#saveObjectsRef
     */
    @JsonIgnore
    public final Map<Object, RefsInfo> refs = new HashMap<>();
    /**
     * The Children.
     */
    public final List<Invocation> children = new CopyOnWriteArrayList<>();
    /**
     * The This object.
     */
    @JsonIgnore
    public Object thisObject;
    /**
     * The This object source.
     */
    @JsonIgnore
    public Object thisObjectSource;
    /**
     * The Args names.
     */
    public final Map<Integer, RefsInfo> argsNames = new HashMap<>();

    /**
     * The Method.
     */
    public String method;
    /**
     * The Signature.
     */
    public String signature;
    /**
     * The Stack counter.
     */
    public final AtomicInteger stackCounter = new AtomicInteger(1);

    /**
     * The Refs info.
     */
    public RefsInfo refsInfo;
    /**
     * The Declared class.
     */
    public Class declaredClass;
    /**
     * The Static invoke.
     */
    public boolean staticInvoke = false;
    /**
     * The Subject.
     */
    public boolean subject = false;
    /**
     * The Finished.
     */
    public volatile boolean finished = false;
    /**
     * The Generic returned.
     */
    public String genericReturned;
    /**
     * The Generic args.
     */
    public String[] genericArgs;
    /**
     * The Clazz source.
     */
    public Class clazzSource;
    /**
     * The Clazz this.
     */
    public Class clazzThis;
    /**
     * The Args type.
     */
    public Class[] argsType;
    /**
     * The Returned type.
     */
    public Class returnedType;
    /**
     * The Parent.
     */
    @JsonIgnore
    Invocation parent;

    /**
     * Resolve source object behind some aop proxy
     *
     * @param proxy the proxy
     * @return the object behind the proxy
     */
    public static Object resolveSource(Object proxy) {
        ProxyResolver proxyResolver = SputnikConfig.INSTANCE.getProxyResolver();
        if (proxyResolver.isProxy(proxy)) {
            Object targetSource = proxyResolver.getTargetSource(proxy);
            if (targetSource != null) {
                return targetSource;
            }
        }
        return proxy;
    }

    /**
     * Save objects ref.
     *
     * @param methodSignure the method signure
     * @param args          the args
     */
    @SneakyThrows
    public void saveObjectsRef(String methodSignure, Object[] args) {
        try {
            if (subject) {
                Map<String, RefsInfo> refMap = SubjectManager.getInstance().SUBJECT_CLASS_REFS.get(clazzSource);
                Map<String, Object> argMap = new HashMap<>();
                if (args != null && args.length > 0) {
                    for (int i = 0; i < args.length; i++) {
                        argMap.put(SubjectManager.keyOfArgs(methodSignure, i), args[i]);
                    }
                }
                RefsInfo refThis = new RefsInfo();
                refThis.name = "this";
                refThis.type = RefsInfo.RefType.FIELD;
                refs.put(thisObjectSource, refThis);
                Set<Map.Entry<String, RefsInfo>> entries = refMap.entrySet();
                for (Map.Entry<String, RefsInfo> entry : entries) {
                    String key = entry.getKey();
                    RefsInfo value = entry.getValue();
                    if (value.type.equals(RefsInfo.RefType.ARG)) {
                        Object argValue = resolveSource(argMap.get(key));
                        if (argValue != null && SubjectManager.isTraced(argValue.getClass())) {
                            refs.putIfAbsent(argValue, value);
                        }
                    } else if (value.type.equals(RefsInfo.RefType.FIELD)) {
                        Field field = clazzSource.getDeclaredField(value.name);
                        field.setAccessible(true);
                        Object feildValue = resolveSource(field.get(thisObjectSource));
                        if (feildValue != null &&
                                (SubjectManager.isTraced(feildValue.getClass()) ||
                                        SubjectManager.isTraced(value.declaredType))) {
                            refs.put(feildValue, value);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

}
