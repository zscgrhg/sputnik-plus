package com.zte.sputnik.parse;


import com.alibaba.ttl.TransmittableThreadLocal;
import com.zte.sputnik.instrument.MethodNames;
import com.zte.sputnik.instrument.TraceUtil;
import com.zte.sputnik.lbs.LoggerBuilder;
import shade.sputnik.org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.zte.sputnik.instrument.TraceUtil.shouldIgnore;


public class SubjectManager {
    public static final SubjectManager instance = new SubjectManager();
    private static final Logger LOGGER
            = LoggerBuilder.of(SubjectManager.class);
    public final Map<Class, Map<String, RefsInfo>> SUBJECT_CLASS_REFS = new ConcurrentHashMap<>();
    public static final TransmittableThreadLocal<Map<Class, List<Field>>> SUBJECT_CLASS_FIELDS = new TransmittableThreadLocal<>();
    public static final TransmittableThreadLocal<Class> SUBJECTS=new TransmittableThreadLocal<>();
    public static final TransmittableThreadLocal<List<Field>> SUBJECTS_FIELDS=new TransmittableThreadLocal<>();


    private SubjectManager() {

    }

    public static SubjectManager getInstance() {
        return instance;
    }

    public static String keyOfArgs(String methodSignure, int idx) {
        return methodSignure + "[" + idx + "]";
    }

    public static boolean isTraced(Class clazz) {
        return TraceUtil.TRACED.containsKey(clazz) ||
                TraceUtil.TRACED.keySet().stream().anyMatch(ck -> ck.isAssignableFrom(clazz));
    }

    public static boolean isSubject(Class clazz) {
        return Optional.ofNullable(SUBJECT_CLASS_FIELDS.get())
                .map(m -> m.containsKey(clazz))
                .orElse(false);
    }

    public void parse(Class<?>... classList){
        parse(Stream.of(classList).collect(Collectors.toList()));
    }

    public void parse(Collection<Class> classList) {
        for (Class clz : classList) {

            SUBJECT_CLASS_REFS.putIfAbsent(clz, new HashMap<>());
            Field[] fields = clz.getDeclaredFields();

            for (Field field : fields) {
                Map<String, RefsInfo> subMap = SUBJECT_CLASS_REFS.get(clz);
                RefsInfo obj = new RefsInfo();
                obj.setType(RefsInfo.RefType.FIELD);
                Class<?> type = field.getType();
                obj.setDeclaredType(type);
                obj.setName(field.getName());
                subMap.put(field.getName(), obj);
            }

            Method[] methods = clz.getDeclaredMethods();
            for (Method method : methods) {
                parseMethod(clz, method);
            }
        }

    }

    public void parseMethod(Class clz, Method method) {

        if (shouldIgnore(method)) {
            return;
        }
        Parameter[] parameters = method.getParameters();
        if (parameters == null) {
            return;
        }
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            Map<String, RefsInfo> subMap = SUBJECT_CLASS_REFS.get(clz);
            RefsInfo obj = new RefsInfo();
            obj.setType(RefsInfo.RefType.ARG);
            Class<?> type = param.getType();
            obj.setDeclaredType(type);
            obj.setName(param.getName());
            obj.setIndex(i);
            subMap.put(keyOfArgs(MethodNames.resolveGenericSymbol(method, clz), i), obj);
        }
    }
}
