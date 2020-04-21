package com.zte.sputnik.config;

import com.zte.sputnik.SputnikMain;
import com.zte.sputnik.builder.NoopSpecWriterImpl;
import com.zte.sputnik.builder.SpecWriter;
import com.zte.sputnik.trace.TraceWriterImpl;
import com.zte.sputnik.trace.proxy.DefaultProxyResolverImpl;
import com.zte.sputnik.trace.proxy.ProxyResolver;
import lombok.SneakyThrows;

import java.io.File;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.Properties;


public class SputnikConfigImpl implements SputnikConfig {
    public static final File workspace = new File(getBaseDir()).toPath().resolve("data").resolve(ymdHmsOfNow()).toFile();
    public static final File specDir = Paths.get(getBaseDir()).resolve(getSpecDir())
            .toFile();

    static {
        if (!workspace.exists()) {
            workspace.mkdirs();
        }

        if (!specDir.exists()) {
            specDir.mkdirs();
        }
    }

    @SneakyThrows
    public static String ymdHmsOfNow() {
        Properties p = new Properties();
        p.load(TraceWriterImpl.class.getClassLoader().getResourceAsStream("git.properties"));
        return p.getProperty("git.commit.id.abbrev") + "/"
                + new SimpleDateFormat("yyyy-MM-dd--HH-mm-ss").format(new Date());
    }

    public static String getBaseDir() {
        return Optional.ofNullable(SputnikMain.CONFIG.getProperty("sputnik.dir.base")).orElse("");
    }

    public static String getSpecDir() {
        return  Optional.ofNullable(SputnikMain.CONFIG.getProperty("sputnik.dir.spec")).orElse("src/sputnik/groovy");
    }

    @Override
    public File getTraceOutputsDir() {
        return workspace;
    }

    @Override
    public File getSpecOutputsDir() {
        return specDir;
    }

    @Override
    public SpecWriter getSpecWriter() {
        SpecWriter specWriter = Optional.ofNullable(SpecWriter.CURRENT.get())
                .orElse(new NoopSpecWriterImpl());
        return specWriter;
    }

    @Override
    public ProxyResolver getProxyResolver() {
        ProxyResolver proxyResolver = Optional.ofNullable(SputnikMain.CONFIG.getProperty("sputnik.proxy.resolver")).map(s -> {
            try {
                Class<?> res = Class.forName(s);
                return (ProxyResolver) res.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).orElse(new DefaultProxyResolverImpl());
        return proxyResolver;
    }


    @Override
    public boolean groopByClass() {
        boolean groopByClass = Optional.ofNullable(SputnikMain.CONFIG.getProperty("sputnik.spec.groop-by-class"))
                .map("true"::equalsIgnoreCase).orElse(true);
        return groopByClass;
    }
}
