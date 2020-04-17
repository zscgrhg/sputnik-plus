package com.zte.sputnik.builder;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.zte.sputnik.trace.Invocation;

public interface SpecWriter {
    TransmittableThreadLocal<SpecWriter> CURRENT=new TransmittableThreadLocal<>();
    void write(Long invocationId);
    default void write(Invocation invocation){
        if(invocation.subject){
            write(invocation.id);
        }
    }
}
