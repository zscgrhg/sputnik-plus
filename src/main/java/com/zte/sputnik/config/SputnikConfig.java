package com.zte.sputnik.config;



import com.zte.sputnik.builder.SpecWriter;
import com.zte.sputnik.trace.proxy.ProxyResolver;

import java.io.File;

public interface SputnikConfig {
    SputnikConfig INSTANCE = new SputnikConfigImpl();

    File getTraceOutputsDir();

    File getSpecOutputsDir();

    SpecWriter getSpecWriter();

    ProxyResolver getProxyResolver();

    boolean groopByClass();
}
