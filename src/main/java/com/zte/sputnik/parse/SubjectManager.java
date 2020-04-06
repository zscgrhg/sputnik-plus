package com.zte.sputnik.parse;


import com.zte.sputnik.instrument.MethodNames;
import com.zte.sputnik.instrument.TraceUtil;
import com.zte.sputnik.lbs.LoggerBuilder;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import shade.sputnik.org.slf4j.Logger;


import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.zte.sputnik.instrument.TraceUtil.shouldIgnore;


public class SubjectManager {
    public static final SubjectManager instance = new SubjectManager();
    private static final Logger LOGGER
            = LoggerBuilder.of(SubjectManager.class);
    public final Map<Class, Map<String, RefsInfo>> SUBJECT_CLASS_REFS = new ConcurrentHashMap<>();
    TestSubjectSelector ss = new DefaultTestSubjectSelectorImpl();
    TraceSelector ts = new DefaultTraceSelectorImpl();

    private SubjectManager() {

    }

    public static SubjectManager getInstance() {
        return instance;
    }

    public static String keyOfArgs(String methodSignure, int idx) {
        return methodSignure + "[" + idx + "]";
    }

    public static boolean isTraced(Class clazz) {
        return TraceUtil.TRACED.containsKey(clazz) || getInstance().ss.selectTestSubject(clazz) || getInstance().ts.select(clazz);
    }

    public static boolean isSubject(Class clazz) {
        return getInstance().ss.selectTestSubject(clazz);
    }

    public void loadFromPkg(String... pkg) {

        try (ScanResult scanResult =
                     new ClassGraph()
                             .enableAllInfo()
                             .whitelistPackages(pkg)
                             .scan()) {
            ClassInfoList clzInpkg = scanResult.getAllClasses();
            List<Class<?>> classes = clzInpkg.loadClasses();
            parse(classes);
        }
    }

    public void parse(List<Class<?>> classList) {
        for (Class clz : classList) {

            if (ss.selectTestSubject(clz)) {
                TraceUtil.traceInvocation(clz, true);
                SUBJECT_CLASS_REFS.putIfAbsent(clz, new HashMap<>());
                Field[] fields = clz.getDeclaredFields();

                for (Field field : fields) {
                    Map<String, RefsInfo> subMap = SUBJECT_CLASS_REFS.get(clz);
                    RefsInfo obj = new RefsInfo();
                    obj.setType(RefsInfo.RefType.FIELD);
                    Class<?> type = field.getType();
                    if (ts.select(field) || ts.select(type)) {
                        TraceUtil.traceInvocation(type, ss.selectTestSubject(type));
                    }
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
            if (ts.select(param)) {
                TraceUtil.traceInvocation(type, ss.selectTestSubject(type));
            }
            obj.setDeclaredType(type);
            obj.setName(param.getName());
            obj.setIndex(i);
            subMap.put(keyOfArgs(MethodNames.resolveGenericSymbol(method, clz), i), obj);
        }
    }
}
