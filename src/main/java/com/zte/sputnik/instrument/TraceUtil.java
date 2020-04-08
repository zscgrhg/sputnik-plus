package com.zte.sputnik.instrument;


import com.zte.sputnik.trace.TraceHelper;
import com.zte.sputnik.util.MustacheUtil;
import org.jboss.byteman.agent.submit.ScriptText;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TraceUtil {
    public static final Map<Class, Boolean> TRACED = new ConcurrentHashMap<>();


    public static boolean shouldIgnore(Method method) {
        String pkg = method.getDeclaringClass().getPackage().getName();
        return method.isSynthetic()
                //|| Modifier.isStatic(method.getModifiers())
                || Modifier.isPrivate(method.getModifiers())
                || pkg.startsWith("java.")
                || pkg.startsWith("sun.");
    }

    public static void traceInvocation(Class clazz, boolean isTSClass) {

        Boolean exist = TRACED.putIfAbsent(clazz, true);
        if (exist != null) {
            return;
        }
        List<ScriptText> scriptTexts = new ArrayList<>();
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            if (shouldIgnore(method)) {
                continue;
            }
            MethodNames names = MethodNames.build(method, clazz, isTSClass);
            {

                BMRuleMustacheModel model = BMRuleMustacheModel.atEntry(names, true);
                model.setHelper(TraceHelper.class);
                model.addAction(MustacheUtil.format("atEntry({{0}},$*);", MethodNames.BIND_NAME));
                String rule = MustacheUtil.render(model);
                ScriptText scriptText = new ScriptText(model.ruleId, rule);
                scriptTexts.add(scriptText);
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
                ScriptText scriptText = new ScriptText(model.ruleId, rule);
                scriptTexts.add(scriptText);
            }
            {
                BMRuleMustacheModel model = BMRuleMustacheModel.atException(names, true);
                model.setHelper(TraceHelper.class);
                model.addAction(MustacheUtil.format("atException({{0}},$*,$^);", MethodNames.BIND_NAME));
                String rule = MustacheUtil.render(model);
                ScriptText scriptText = new ScriptText(model.ruleId, rule);
                scriptTexts.add(scriptText);
            }
        }
        BMUtil.submitText(scriptTexts);
    }
}
