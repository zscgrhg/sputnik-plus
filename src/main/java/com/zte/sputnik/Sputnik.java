package com.zte.sputnik;

import com.alibaba.ttl.threadpool.agent.TtlAgent;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.VirtualMachine;
import com.zte.sputnik.instrument.BMUtil;
import com.zte.sputnik.lbs.LoggerBuilder;
import com.zte.sputnik.parse.SubjectManager;
import lombok.SneakyThrows;
import org.jboss.byteman.agent.Main;
import shade.sputnik.org.slf4j.Logger;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * @author zscgrhg
 */
public class Sputnik {


    private static final Logger LOGGER = LoggerBuilder.of(Sputnik.class);

    public static final Properties CONFIG = loadConfig();

    static {
        initJNA();
    }
    @SneakyThrows
    private static void initJNA(){
        URL jnaLocation = Kernel32.class.getProtectionDomain().getCodeSource().getLocation();
        String path = Paths.get(jnaLocation.toURI()).toString();
        System.setProperty("jna.nosys", "true");
        System.setProperty("jna.boot.library.path", path);
    }

    @SneakyThrows
    public static Properties loadConfig() {
        Properties config = new Properties();
        Optional.ofNullable(Sputnik.class.getClassLoader().getResourceAsStream("sputnik.properties"))
                .ifPresent(p->{
                    config.load(p);
                });

        return config;
    }

    @SneakyThrows
    public synchronized static void loadAgent()  {


        if (Main.firstTime) {
            BMUtil.loadAgent();
        }
        if (TtlAgent.firstLoad) {
            loadTtlAgent();
        }
    }

    @SneakyThrows
    public static void loadTtlAgent() {
        try {
            VirtualMachine jvm = VirtualMachine.attach(String.valueOf(BMUtil.getPid()));
            Path agentPath = Paths.get(TtlAgent.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            jvm.loadAgent(agentPath.toString());
        } catch (AgentInitializationException e) {
            // this probably indicates that the agent is already installed
        }
    }

}
