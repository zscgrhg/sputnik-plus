package com.zte.sputnik.builder;

import com.alibaba.ttl.threadpool.agent.TtlAgent;
import com.zte.sputnik.Sputnik;
import com.zte.sputnik.instrument.TraceUtil;
import com.zte.sputnik.lbs.LoggerBuilder;
import com.zte.sputnik.parse.SubjectManager;
import com.zte.sputnik.trace.InvocationContext;
import com.zte.sputnik.util.ClassUtil;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import shade.sputnik.org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SputnikUTFactory extends TestWatcher implements SpecWriter {
    private static final Logger LOGGER = LoggerBuilder.of(InvocationContext.class);

    private static final ThreadLocal<List<Long>> CACHE = new ThreadLocal<List<Long>>();
    private Class subject;
    private final List<Field> values=new ArrayList<>();
    private final Set<String> valueFieldNames=new HashSet<>();
    private final Set<Class> trace=new HashSet<>();

    static {
        if (!TtlAgent.isTtlAgentLoaded()) {
            LOGGER.debug("load ttl");
            try {
                Sputnik.loadAgent();
                Sputnik.loadTtlAgent();
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

    }

    public SputnikUTFactory subject(Class subject){
        this.subject=subject;
        return this;
    }

    public SputnikUTFactory mockFieldsHasAnnotation(Class... anno){
        Collection<Field> fields = ClassUtil.fieldsOf(subject);
        for (Field field : fields) {
            boolean hasAnno = Stream.of(anno).anyMatch(an -> ClassUtil.hasAnnotation(field, an));
            if(hasAnno){
                if(valueFieldNames.contains(field.getName())){
                    values.add(field);
                }else {
                    trace.add(field.getType());
                }
            }
        }
        return this;
    }

    public SputnikUTFactory serializeFieldsHasAnnotation(Class... anno){
        Collection<Field> fields = ClassUtil.fieldsOf(subject);
        for (Field field : fields) {
            boolean hasAnno = Stream.of(anno).anyMatch(an -> ClassUtil.hasAnnotation(field, an));
            if(hasAnno){
                values.add(field);
            }
        }
        return this;
    }

    public SputnikUTFactory serializeFields(String... fieldNames){
       valueFieldNames.addAll(Stream.of(fieldNames).collect(Collectors.toSet()));
        return this;
    }

    public SputnikUTFactory mockClass(Class... classes){
        trace.addAll(Stream.of(classes).collect(Collectors.toSet()));
        return this;
    }

    @Override
    protected void succeeded(Description description) {
        super.succeeded(description);
        LOGGER.debug(description.getDisplayName() + " succeeded");
        List<Long> invocations = CACHE.get();
        if (invocations != null) {
            for (Long invocation : invocations) {
                try {
                    SpecFactory.writeSpec(invocation);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void failed(Throwable e, Description description) {
        super.failed(e, description);
        LOGGER.debug(description.getDisplayName() + " failed");
    }

    @Override
    protected void starting(Description description) {
        LOGGER.debug(description.getDisplayName() + " starting");
        SpecWriter.CURRENT.set(this);
        SubjectManager.getInstance().parse(subject);
        Field[] fields = values.stream().toArray(Field[]::new);
        TraceUtil.traceInvocation(subject,true,fields);
        for (Class clazz : trace) {
            SubjectManager.getInstance().parse(clazz);
            TraceUtil.traceInvocation(clazz,false);
        }

        super.starting(description);
    }

    @Override
    protected void finished(Description description) {
        super.finished(description);
        LOGGER.debug(description.getDisplayName() + " finished");
        SubjectManager.SUBJECTS.remove();
        SubjectManager.SUBJECTS_FIELDS.remove();
        TraceUtil.unload(subject);
        TraceUtil.unload(trace);
        CACHE.remove();
        SpecWriter.CURRENT.remove();
    }


    @Override
    public void write(Long invocationId) {
        if (CACHE.get() == null) {
            CACHE.set(new ArrayList<>());
        }
        CACHE.get().add(invocationId);
    }
}
