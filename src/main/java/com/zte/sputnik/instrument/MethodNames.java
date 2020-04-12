package com.zte.sputnik.instrument;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zte.sputnik.lbs.LoggerBuilder;
import com.zte.sputnik.util.ClassUtil;
import lombok.Data;
import shade.sputnik.org.slf4j.Logger;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Data
public class MethodNames {
    private static final Logger LOGGER = LoggerBuilder.of(MethodNames.class);
    public static final AtomicLong NAMES_INCR = new AtomicLong(1);
    public static final Map<Long, MethodNames> METHOD_NAMES_MAP = new ConcurrentHashMap<>();

    public static final String BIND_NAME = "_moc_etz_zunit_instrument_MethodNames_mid_";
    @JsonIgnore
    public final Long mid = NAMES_INCR.getAndIncrement();
    public final Class context;
    public final Method method;
    public final String ownerName;
    public final String contextType;
    public final String name;
    public final Class[] parametersType;
    public final String genericSymbol;
    public final String symbol;
    public final String genericSignature;
    public final String signature;
    public final String erased;

    private MethodNames(Method m, Class context) {
        this.context = context;
        this.method = m;
        this.parametersType = m.getParameterTypes();
        this.contextType = context.isInterface() ? "INTERFACE" : "CLASS";
        this.ownerName = context.getName();
        this.name = m.getName();
        this.signature = resolveGeneric(method, context);
        this.erased = resolveGeneric(method, method.getDeclaringClass());
        this.genericSymbol = resolveGenericSymbol(m, context);
        this.symbol = removeGeneric(genericSymbol);
        int argStart = genericSymbol.indexOf('(');
        String noArgString = genericSymbol.substring(0, argStart);
        int idx = noArgString.lastIndexOf('.') + 1;
        this.genericSignature = genericSymbol.substring(idx);
    }


    public static MethodNames build(Method m, Class context) {
        MethodNames methodNames = new MethodNames(m, context);
        METHOD_NAMES_MAP.putIfAbsent(methodNames.mid, methodNames);
        LOGGER.debug(methodNames.mid + " is mapping to :" + methodNames.genericSymbol);
        return methodNames;
    }

    public static String resolveGenericSymbol(Method method, Class context) {
        return normalizeVarargs(context.getName() + "." + resolveGeneric(method, context));
    }

    public static String resolveGeneric(Method method, Class context) {
        StringBuilder nameBuilder = new StringBuilder(method.getName()).append("(");
        Type[] genericParameterTypes = method.getGenericParameterTypes();
        String args = Stream.of(genericParameterTypes)
                .map(t -> ClassUtil.resolve(t, context).getTypeName()).collect(Collectors.joining(","));
        nameBuilder.append(args);
        nameBuilder.append(")");
        return removeGeneric(nameBuilder.toString());
    }

    public static String removeGeneric(String name) {
        if (name == null) {
            return name;
        }
        String rp = name.replaceAll("<[^<>]*>", "");
        if (rp.contains("<")) {
            return removeGeneric(rp);
        }
        return rp;
    }

    public static String normalizeVarargs(String genericString) {

        return genericString.replaceAll("\\Q...\\E", "[]");
    }

    public String argRefs() {
        int length = parametersType.length;
        return IntStream.range(1, 1 + length).mapToObj(i -> "$" + i).collect(Collectors.joining(","));
    }
}
