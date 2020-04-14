package com.zte.sputnik.trace;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zte.sputnik.builder.SpecWriter;
import com.zte.sputnik.config.SputnikConfig;
import com.zte.sputnik.instrument.MethodNames;
import com.zte.sputnik.lbs.LoggerBuilder;
import com.zte.sputnik.parse.RefsInfo;
import com.zte.sputnik.parse.SubjectManager;
import com.zte.sputnik.parse.ValueObjectModel;
import com.zte.sputnik.util.ClassUtil;
import lombok.Data;
import shade.sputnik.org.slf4j.Logger;


import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.zte.sputnik.parse.SubjectManager.SUBJECT_CLASS_FIELDS;

/**
 * The type Invocation context.
 */
@Data
public class InvocationContext {
    private static final Logger LOGGER = LoggerBuilder.of(InvocationContext.class);
    private static final SpecWriter SPEC_WRITER = SputnikConfig.INSTANCE.getSpecWriter();

    /**
     * The constant PREVIOUS.
     */
    public final static TransmittableThreadLocal<Invocation> PREVIOUS = new TransmittableThreadLocal<>();
    /**
     * The constant CXT_INCR.
     */
    public static final AtomicLong CXT_INCR = new AtomicLong(1);
    /**
     * The constant STAGED.
     */
    public final static ThreadLocal<Invocation> STAGED = new ThreadLocal<>();
    /**
     * The constant CONTEXT.
     */
    public final static ThreadLocal<InvocationContext> CONTEXT = new ThreadLocal<>();
    /**
     * The constant STACK_THREAD_LOCAL.
     */
    public final static ThreadLocal<Stack<Invocation>> STACK_THREAD_LOCAL = new ThreadLocal<>();
    /**
     * The constant ARGS_STACK.
     */
    public final static ThreadLocal<Stack<Object[]>> ARGS_STACK = new ThreadLocal<>();
    /**
     * The Entry counter.
     */
    public final AtomicInteger ENTRY_COUNTER = new AtomicInteger(Integer.MIN_VALUE);
    /**
     * The Exit counter.
     */
    public final AtomicInteger EXIT_COUNTER = new AtomicInteger(Integer.MAX_VALUE);

    /**
     * The Id.
     */
    public final Long id = CXT_INCR.getAndIncrement();
    /**
     * The Map.
     */
    @JsonIgnore
    public final Map<Long, Invocation> map = new ConcurrentHashMap<>();
    /**
     * The Trace writer.
     */
    final TraceWriter traceWriter = new TraceWriterImpl();

    /**
     * Gets current.
     *
     * @param create the create
     * @return the current
     */
    public static InvocationContext getCurrent(boolean create) {
        InvocationContext current = CONTEXT.get();
        if (create && current == null) {
            CONTEXT.set(new InvocationContext());
        }
        return CONTEXT.get();
    }

    private static boolean checkTheadId(Invocation invocation) {
        return invocation.threadId == Thread.currentThread().getId();
    }
    public static InvocationContext getCurrent(MethodNames names) {
       return getCurrent(canCreateContxt(names));
    }
    public static boolean canCreateContxt(MethodNames names){
        return SubjectManager.isSubject(names.context)||PREVIOUS.get()!=null;
    }

    /**
     * 一个方法调用可能触发多条 byteman rule: 追踪超类方法的rule和追踪子类方法的rule在子类方法
     * 调用时都会触发。
     *
     * @return 如果已经执行了追踪规则 返回false;否则返回true并且重置入栈计数器和出栈计数器
     */
    public boolean canPush() {
        long prev = ENTRY_COUNTER.get();
        int length = Thread.currentThread().getStackTrace().length;
        boolean success = false;
        if (length > prev) {
            ENTRY_COUNTER.set(length);
            EXIT_COUNTER.set(Integer.MAX_VALUE);
            success = true;
        }
        LOGGER.debug("canPush :" + success);
        return success;
    }

    /**
     * 一个方法调用可能触发多条 byteman rule: 追踪超类方法的rule和追踪子类方法的rule在子类方法
     * 调用时都会触发。
     *
     * @return 如果已经执行了追踪规则 返回false;否则返回true并且重置入栈计数器和出栈计数器
     */
    public boolean canPop() {
        long prev = EXIT_COUNTER.get();
        int length = Thread.currentThread().getStackTrace().length;
        boolean success = false;
        if (length < prev) {
            EXIT_COUNTER.set(length);
            ENTRY_COUNTER.set(Integer.MIN_VALUE);
            success = true;
        }
        LOGGER.debug("canPop :" + success);
        return success;
    }

    /**
     * Push.
     *
     * @param invocation the invocation
     * @param originArgs the origin args
     */
    public void push(Invocation invocation, Object[] originArgs) {
        Stack<Invocation> stack = STACK_THREAD_LOCAL.get();
        if (stack == null) {
            stack = new Stack<>();
            STACK_THREAD_LOCAL.set(stack);
        }
        Stack<Object[]> argsStack = ARGS_STACK.get();
        if (argsStack == null) {
            argsStack = new Stack<>();
            ARGS_STACK.set(argsStack);
        }
        Object[] argsCopy = Arrays.copyOf(originArgs, originArgs.length);


        Invocation prevTTL = PREVIOUS.get();
        if (prevTTL != null && !checkTheadId(prevTTL) && stack.isEmpty()) {
            //in spawned thread
            LOGGER.debug("stage:" + prevTTL);
            STAGED.set(prevTTL);
        }


        Invocation prev = PREVIOUS.get();
        MethodNames names = MethodNames.METHOD_NAMES_MAP.get(invocation.mid);
        if (prev != null) {
            invocation.parent = prev;
            if (prev.thisObjectSource == invocation.thisObjectSource) {
                assert prev.threadId == Thread.currentThread().getId();
                int andIncrement = prev.stackCounter.incrementAndGet();
                LOGGER.debug("stackCounter ++ :" + andIncrement);
                return;
            }
           if(invocation.staticInvoke){
               RefsInfo refsInfo =new RefsInfo();
               refsInfo.declaredType=invocation.clazzThis;
               refsInfo.type= RefsInfo.RefType.INVOKE_STATIC;
               refsInfo.name=names.signature;
               invocation.refsInfo = refsInfo;
               invocation.declaredClass = refsInfo.declaredType;
           }else {
               RefsInfo refsInfo = prev.refs.get(invocation.thisObjectSource);
               if(refsInfo!=null){
                   invocation.refsInfo = refsInfo;
                   invocation.declaredClass = refsInfo.declaredType;
               }else {
                   StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                   StackTraceElement stackTraceElement =
                           Stream.of(stackTrace).filter(ste -> ste.getClassName().equals(prev.clazzSource.getName())).findFirst().get();
                   LOGGER.error("Refsinfo lost! A method with return type '{}' at location {} was not tracked!",invocation.clazzSource,stackTraceElement);
               }
           }
            prev.getChildren().add(invocation);
        }
        stack.push(invocation);
        argsStack.push(argsCopy);
        PREVIOUS.set(invocation);
        map.put(invocation.id, invocation);

        ParamModel p = new ParamModel();
        for (int i = 0; i < argsCopy.length; i++) {
            Object arg = argsCopy[i];
            if (arg != null) {
                Object argSource = Invocation.resolveSource(arg);
                RefsInfo argRefInfo = invocation.refs.get(argSource);
                if (argRefInfo != null) {
                    argsCopy[i] = argRefInfo;
                    invocation.argsNames.put(i, argRefInfo);
                }
            }
        }
        p.args = argsCopy;
        p.argsGenericType = invocation.genericArgs;
        p.argsType = ParamModel.valuesTypeOf(argsCopy);
        invocation.argsType = p.argsType;
        invocation.returnedType=names.getMethod().getReturnType();
        p.invocationId = invocation.id;
        p.name = ParamModel.INPUTS;
        traceWriter.write(p);
        if(invocation.subject&&invocation.thisObjectSource!=null){
            List<Field> fields = Optional.ofNullable(SUBJECT_CLASS_FIELDS.get())
                    .map(m -> m.get(names.context))
                    .orElse(new ArrayList<>());
            if(fields!=null){
                Map<String, ValueObjectModel> values=new HashMap<>();
                for (Field field : fields) {
                    field.setAccessible(true);
                    Object fieldValue = ClassUtil.getFieldValue(field, invocation.thisObjectSource);
                    ValueObjectModel vm=new ValueObjectModel();
                    vm.setValue(fieldValue);
                    vm.setDeclType(field.getGenericType().getTypeName());
                    Class clazz = Optional.ofNullable(fieldValue)
                            .map(Object::getClass)
                            .orElse((Class) field.getDeclaringClass());
                    vm.setValueClass(clazz);
                    values.put(field.getName(),vm);
                }
                traceWriter.writeValues(invocation,values);
            }
        }
    }

    /**
     * Pop.
     *
     * @param returnValue the return value
     * @param exception   the exception
     */
    public void pop(Object returnValue, Throwable exception) {
        Stack<Invocation> stack = STACK_THREAD_LOCAL.get();
        Invocation last = stack.lastElement();
        int stackCounter = last.stackCounter.decrementAndGet();
        LOGGER.debug("stackCounter -- :" + stackCounter);
        if (stackCounter > 0) {
            assert last.threadId == Thread.currentThread().getId();
            return;
        }
        Invocation pop = stack.pop();
        Object[] originArgs = ARGS_STACK.get().pop();
        Invocation parent = pop.parent;


        //MethodNames names = MethodNames.METHOD_NAMES_MAP.get(pop.mid);
        ParamModel p = new ParamModel();
        p.invocationId = pop.id;
        p.args = originArgs;
        p.argsType = ParamModel.valuesTypeOf(originArgs);
        p.argsGenericType = pop.genericArgs;
        p.returned = returnValue;
        p.returnedType = ParamModel.typeOf(p.returned);

        p.returnedGenericType = pop.genericReturned;
        p.thrown = exception;
        if (exception != null) {
            p.exception = exception.getClass().getName();
        }
        p.name = ParamModel.OUTPUTS;
        if (exception == null && parent != null && returnValue != null) {
            Object returnedSourceObject = Invocation.resolveSource(returnValue);
            boolean traced = SubjectManager.isTraced(returnedSourceObject.getClass())
                    ||SubjectManager.isTraced(pop.returnedType);
            if (traced) {
                RefsInfo returnedRef = new RefsInfo();
                returnedRef.type = RefsInfo.RefType.RETURNED;
                returnedRef.returnedFrom = pop.id;
                returnedRef.declaredType= MethodNames.METHOD_NAMES_MAP.get(pop.mid).method.getReturnType();
                returnedRef.name="returnedBy"+pop.id;
                parent.refs.put(returnedSourceObject, returnedRef);
                p.returned=returnedRef;
                p.returnedType=RefsInfo.class;
                p.returnedGenericType=RefsInfo.class.getName();
            }
        }
        traceWriter.write(p);
        pop.finished = true;
        if(pop.subject){
            traceWriter.write(pop);
            SPEC_WRITER.write(pop.id);
        }
        if (stack.isEmpty()) {
            traceWriter.write(this);


            CONTEXT.remove();
            PREVIOUS.remove();
            Invocation prevTTL = STAGED.get();
            if (prevTTL != null) {
                STAGED.remove();
                LOGGER.debug("unstaged:" + prevTTL);
                PREVIOUS.set(prevTTL);
            }
        } else {
            PREVIOUS.set(stack.lastElement());
        }
    }

    /**
     * Gets nodes.
     *
     * @return the nodes
     */
    public List<Invocation> getNodes() {
        List<Invocation> root = map.values().stream()
                .filter(inv->inv.parent==null)
                .collect(Collectors.toList());
        return root;
    }


}
