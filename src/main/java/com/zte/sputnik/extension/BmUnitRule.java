package com.zte.sputnik.extension;

import org.jboss.byteman.contrib.bmunit.*;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static org.jboss.byteman.contrib.bmunit.BMRunnerUtil.constructScriptText;

/**
 * @author zscgrhg
 */
public class BmUnitRule implements MethodRule {
    public static final ThreadLocal<Boolean> PUSHED=new ThreadLocal<>();
    public static final ThreadLocal<Boolean> SCRIPT_PUSHED=new ThreadLocal<>();
    @Override
    public Statement apply(Statement s, FrameworkMethod m, Object t) {
        s = this.addMethodRuleLoader(s, m, t.getClass());
        s = this.addMethodConfigLoader(s, m, t.getClass());
        return s;
    }

    private Statement addMethodRuleLoader(final Statement s, FrameworkMethod m, final Class clazz) {
        BMRule[] bmRules = Optional.ofNullable(m.getAnnotation(BMRules.class)).map(a -> a.rules()).orElse(null);
        BMRule bmRule = Optional.ofNullable(m.getAnnotation(BMRule.class)).orElse(null);
        BMRule[] rulesConcat = Stream.concat(Stream.of(bmRules), Stream.of(bmRule)).filter(Objects::nonNull).toArray(BMRule[]::new);

        return Optional.ofNullable(rulesConcat)
                .filter(a -> a.length > 0).map(rules -> {
                    final String name = m.getName();
                    final String script = constructScriptText(rules);
                    return (Statement) new Statement() {
                        @Override
                        public void evaluate() throws Throwable {

                            Boolean p = SCRIPT_PUSHED.get();
                            if(!Boolean.TRUE.equals(p)){
                                BMUnit.loadScriptText(clazz, name, script);
                                SCRIPT_PUSHED.set(true);
                            }

                            try {
                                s.evaluate();
                            } finally {
                                if(Boolean.TRUE.equals(p)){
                                    BMUnit.unloadScriptText(clazz, name);
                                    SCRIPT_PUSHED.remove();
                                }
                            }
                        }
                    };
                }).orElse(s);
    }


    private Statement addMethodConfigLoader(final Statement s, FrameworkMethod m, final Class clazz) {
        if (s instanceof ConfigStatement) {
            return s;
        } else {
            return new ConfigStatement(s, m, clazz);
        }
    }

    private static class ConfigStatement extends Statement {
        final Statement s;
        final FrameworkMethod m;
        final Class clazz;
        final BMUnitConfig classAnnotation;
        final BMUnitConfig annotation;
        final Method testMethod;

        public ConfigStatement(Statement s, FrameworkMethod m, Class clazz) {
            this.s = s;
            this.m = m;
            this.clazz = clazz;
            this.classAnnotation = (BMUnitConfig) clazz.getAnnotation(BMUnitConfig.class);
            this.annotation = m.getAnnotation(BMUnitConfig.class);
            this.testMethod = m.getMethod();
        }


        @Override
        public void evaluate() throws Throwable {
            Boolean p = PUSHED.get();
            if(!Boolean.TRUE.equals(p)){
                BMUnitConfigState.pushConfigurationState(classAnnotation, clazz);
                BMUnitConfigState.pushConfigurationState(annotation, testMethod);
                PUSHED.set(true);
            }
            try {
                s.evaluate();
            } finally {
                if(Boolean.TRUE.equals(p)){
                    BMUnitConfigState.popConfigurationState(testMethod);
                    BMUnitConfigState.popConfigurationState(clazz);
                    PUSHED.remove();
                }

            }
        }
    }
}
