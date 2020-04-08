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
                            BMUnit.loadScriptText(clazz, name, script);
                            try {
                                s.evaluate();
                            } finally {
                                BMUnit.unloadScriptText(clazz, name);
                            }
                        }
                    };
                }).orElse(s);
    }


    private Statement addMethodConfigLoader(final Statement s, FrameworkMethod m, final Class clazz) {
        final BMUnitConfig classAnnotation = (BMUnitConfig) clazz.getAnnotation(BMUnitConfig.class);
        final BMUnitConfig annotation = m.getAnnotation(BMUnitConfig.class);
        final Method testMethod = m.getMethod();
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                BMUnitConfigState.pushConfigurationState(classAnnotation, clazz);
                BMUnitConfigState.pushConfigurationState(annotation, testMethod);
                try {
                    s.evaluate();
                } finally {
                    BMUnitConfigState.popConfigurationState(testMethod);
                    BMUnitConfigState.popConfigurationState(clazz);
                }
            }
        };
    }
}
