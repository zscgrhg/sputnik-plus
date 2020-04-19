package com.zte.sputnik.builder;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.zte.sputnik.SputnikMain;
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

    private static   final TransmittableThreadLocal<Map<Long,StageDescription>> CACHE = new TransmittableThreadLocal<>();
    public static final TransmittableThreadLocal<StageDescription> STAGE_TTL=new TransmittableThreadLocal();

    private  final Map<Class,Set<Field>> subjectFiledInfo=new HashMap<>();
    private  final Map<Class,Set<String>> subjectFiledNameInfo=new HashMap<>();
    private final Set<Class> mockAnno=new HashSet<>();
    private final Set<Class> valueAnno=new HashSet<>();


    private final Set<Class> trace=new HashSet<>();



    public SputnikUTFactory addSubject(Class... subject){
        for (Class cls : subject) {
            subjectFiledInfo.putIfAbsent(cls,new HashSet<>());
            subjectFiledNameInfo.putIfAbsent(cls,new HashSet<>());
        }
        return this;
    }

    public SputnikUTFactory mockFieldsHasAnnotation(Class... anno){

        for (Class a : anno) {
            mockAnno.add(a);
        }


      /* */


        return this;
    }

    public SputnikUTFactory serializeFieldsHasAnnotation(Class... anno){

        for (Class a : anno) {
            valueAnno.add(a);
        }
        return this;
    }

    public SputnikUTFactory serializeFields(Class cls,String... fieldNames){
        subjectFiledNameInfo.get(cls).addAll(Stream.of(fieldNames).collect(Collectors.toSet()));
        return this;
    }

    public SputnikUTFactory mockClasses(Class... classes){
        trace.addAll(Stream.of(classes).collect(Collectors.toSet()));
        return this;
    }

    @Override
    protected void succeeded(Description description) {
        super.succeeded(description);
        LOGGER.debug(description.getDisplayName() + " succeeded");
        Map<Long,StageDescription> invocations = CACHE.get();
        if (invocations != null&&!invocations.isEmpty()) {
            for (Map.Entry<Long, StageDescription> invocation : invocations.entrySet()) {
                try {
                    SpecBuilder.writeSpec(invocation.getKey(),invocation.getValue());
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

    private void parseMockAnno(){
        for (Class m : mockAnno) {
            for (Map.Entry<Class, Set<Field>> entry : subjectFiledInfo.entrySet()) {
                Class key = entry.getKey();
                Set<Field> value = entry.getValue();
                //
                Set<String> valueFieldNames = subjectFiledNameInfo.get(key);
                Collection<Field> fields = ClassUtil.fieldsOf(key);
                for (Field field : fields) {
                    boolean hasAnno = ClassUtil.hasAnnotation(field, m);
                    if(hasAnno){
                        if(valueFieldNames.contains(field.getName())){
                            value.add(field);
                        }else {
                            trace.add(field.getType());
                        }
                    }
                }
            }
        }
    }

    private void parseValueAnno(){
        for (Class m : valueAnno) {
            for (Map.Entry<Class, Set<Field>> entry : subjectFiledInfo.entrySet()) {
                Class key = entry.getKey();
                Set<Field> value = entry.getValue();
                Collection<Field> fields = ClassUtil.fieldsOf(key);
                for (Field field : fields) {
                    boolean hasAnno = ClassUtil.hasAnnotation(field, m);
                    if(hasAnno){
                        value.add(field);
                    }
                }
            }
        }
    }

    @Override
    protected void starting(Description description) {
        LOGGER.debug(description.getDisplayName() + " starting");
        String enableFromConfig = SputnikMain.CONFIG.getProperty("sputnik.enable", "false");
        String enable=System.getProperty("sputnik.enable",enableFromConfig);
        if(!"true".equalsIgnoreCase(enable)){
            super.starting(description);
            LOGGER.debug("sputnik.enable={}",enableFromConfig);
            return;
        }
        SpecWriter.CURRENT.set(this);

        String distinct = SputnikMain.CONFIG.getProperty("sputnik.builder.distinct", "false");
        if("false".equalsIgnoreCase(distinct)){
            STAGE_TTL.set(new StageDescriptionImpl(description));
        }


        parseMockAnno();
        parseValueAnno();


        for (Map.Entry<Class, Set<Field>> entry : subjectFiledInfo.entrySet()) {
            Class key = entry.getKey();
            Set<Field> value = entry.getValue();
            SubjectManager.getInstance().parse(key);
            Field[] fields = value.stream().toArray(Field[]::new);
            TraceUtil.traceInvocation(key,true,fields);
        }

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
        CACHE.remove();
        SpecWriter.CURRENT.remove();
        STAGE_TTL.remove();
    }


    @Override
    public void write(Long invocationId) {
        if (CACHE.get() == null) {
            CACHE.set(new HashMap<>());
        }
        CACHE.get().put(invocationId,STAGE_TTL.get());
    }
}
