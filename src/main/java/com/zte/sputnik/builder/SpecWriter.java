package com.zte.sputnik.builder;

import com.alibaba.ttl.TransmittableThreadLocal;

public interface SpecWriter {
    TransmittableThreadLocal<SpecWriter> CURRENT=new TransmittableThreadLocal<>();
    void write(Long invocationId);
}
