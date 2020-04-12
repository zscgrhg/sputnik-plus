package com.zte.sputnik.instrument;


import com.zte.sputnik.parse.SubjectManager;
import com.zte.sputnik.trace.TraceHelper;
import com.zte.sputnik.util.MustacheUtil;
import org.jboss.byteman.agent.submit.ScriptText;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TraceUtil {
    public static final Map<Class, Set<ScriptTextValue>> TRACED = new ConcurrentHashMap<>();
    public static final Set<String> NO_TRACE= Collections.unmodifiableSet(noTrace());

    public static String singureOf(Method m){
        String ms = m.toString();
        return ms.substring(ms.indexOf(m.getName()+"("),ms.indexOf(")")+1);
    }

    public static Set<String> noTrace(){
        return Stream.of(Object.class)
                .flatMap(cls->Stream.of(cls.getMethods()))
                .map(TraceUtil::singureOf)
                .collect(Collectors.toSet());
    }

    public static boolean shouldIgnore(Method method) {

        return method.isSynthetic()
                //|| Modifier.isStatic(method.getModifiers())
                || Modifier.isPrivate(method.getModifiers())
                || shouldIgnore(method.getDeclaringClass())
                ||NO_TRACE.contains(singureOf(method));
    }
    public static boolean shouldIgnore(Class clazz){
        String pkg = clazz.getPackage().getName();
        return pkg.startsWith("java.")|| pkg.startsWith("sun.");
    }

    public static void traceInvocation(Class clazz, boolean isSubject, Field... fields) {
        if(shouldIgnore(clazz)){
            return;
        }
        if(isSubject){
            SubjectManager.SUBJECTS.set(clazz);
            SubjectManager.SUBJECTS_FIELDS.set(Stream.of(fields).collect(Collectors.toList()));
        }
        Set<ScriptTextValue> exist = TRACED.putIfAbsent(clazz, new HashSet<>());
        if (exist != null) {
            return;
        }
        List<ScriptText> scriptTexts = new ArrayList<>();
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            if (shouldIgnore(method)) {
                continue;
            }
            MethodNames names = MethodNames.build(method, clazz);
            {

                BMRuleMustacheModel model = BMRuleMustacheModel.atEntry(names, true);
                model.setHelper(TraceHelper.class);
                model.addAction(MustacheUtil.format("atEntry({{0}},$*);", MethodNames.BIND_NAME));
                String rule = MustacheUtil.render(model);
                ScriptTextValue scriptText = new ScriptTextValue(model.ruleId, rule);
                scriptTexts.add(scriptText);
                TRACED.compute(clazz,(k,v)->{
                   v.add(scriptText);
                   return v;
                });
            }
            {
                BMRuleMustacheModel model = BMRuleMustacheModel.atExit(names, true);
                model.setHelper(TraceHelper.class);
                Class<?> returnType = names.method.getReturnType();
                if (Void.class.equals(returnType) || void.class.equals(returnType)) {
                    model.addAction(MustacheUtil.format("atExit({{0}},$*);", MethodNames.BIND_NAME));
                } else {
                    model.addAction(MustacheUtil.format("atExit({{0}},$*,$!);", MethodNames.BIND_NAME));
                }

                String rule = MustacheUtil.render(model);
                ScriptTextValue scriptText = new ScriptTextValue(model.ruleId, rule);
                scriptTexts.add(scriptText);
                TRACED.compute(clazz,(k,v)->{
                    v.add(scriptText);
                    return v;
                });
            }
            {
                BMRuleMustacheModel model = BMRuleMustacheModel.atException(names, true);
                model.setHelper(TraceHelper.class);
                model.addAction(MustacheUtil.format("atException({{0}},$*,$^);", MethodNames.BIND_NAME));
                String rule = MustacheUtil.render(model);
                ScriptTextValue scriptText = new ScriptTextValue(model.ruleId, rule);
                scriptTexts.add(scriptText);
                TRACED.compute(clazz,(k,v)->{
                    v.add(scriptText);
                    return v;
                });
            }
        }
        BMUtil.submitText(scriptTexts);
    }

    public static void unload(Collection<Class> classes){
        List<ScriptText> scriptTexts = classes.stream().flatMap(clazz ->
                TRACED.getOrDefault(clazz, Collections.emptySet()).stream()
                        .map(s -> (ScriptText) s)
        ).collect(Collectors.toList());
        BMUtil.unloadText(scriptTexts);
        for (Class clazz : classes) {
            TRACED.remove(clazz);
        }
    }
    public static void unload(Class... classes){
        unload(Stream.of(classes).collect(Collectors.toSet()));
    }
}
