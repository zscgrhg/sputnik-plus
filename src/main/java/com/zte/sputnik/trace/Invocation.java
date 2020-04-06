package com.zte.sputnik.trace;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zte.sputnik.config.SputnikConfig;
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


@Data
@ToString(exclude = {
        "refs",
        "thisObject",
        "thisObjectSource",
        "parent",
        "children",
})
public class Invocation {
    public static final AtomicLong INVOCATION_INCR = new AtomicLong(1);

    public final Long id = INVOCATION_INCR.getAndIncrement();
    public final long threadId = Thread.currentThread().getId();
    public Long mid;
    @JsonIgnore
    public final Map<Object, RefsInfo> refs = new HashMap<>();
    public final List<Invocation> children = new CopyOnWriteArrayList<>();
    @JsonIgnore
    public Object thisObject;
    @JsonIgnore
    public Object thisObjectSource;
    public final Map<Integer, RefsInfo> argsNames = new HashMap<>();

    public String method;
    public String signature;
    public final AtomicInteger stackCounter = new AtomicInteger(1);

    public RefsInfo refsInfo;
    public Class declaredClass;
    public boolean staticInvoke = false;
    public boolean subject = false;
    public volatile boolean finished = false;
    public String genericReturned;
    public String[] genericArgs;
    public Class clazzSource;
    public Class clazzThis;
    public Class[] argsType;
    @JsonIgnore
    Invocation parent;

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
