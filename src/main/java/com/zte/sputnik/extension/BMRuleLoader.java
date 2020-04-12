package com.zte.sputnik.extension;

import com.alibaba.ttl.threadpool.agent.TtlAgent;
import com.zte.sputnik.Sputnik;
import org.jboss.byteman.contrib.bmunit.*;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static org.jboss.byteman.contrib.bmunit.BMRunnerUtil.constructScriptText;

/**B
 * @author zscgrhg
 */
public class BMRuleLoader implements MethodRule {

    @Override
    public Statement apply(Statement s, FrameworkMethod m, Object t) {
        return this.addMethodRuleLoader(s, m, t.getClass());
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

    static {
        Sputnik.loadAgent();
        if(TtlAgent.firstLoad){
            Sputnik.loadTtlAgent();
        }
    }

}
