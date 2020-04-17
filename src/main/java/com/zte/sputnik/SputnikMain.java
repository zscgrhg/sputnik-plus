package com.zte.sputnik;

import com.alibaba.ttl.threadpool.agent.TtlAgent;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.VirtualMachine;
import com.zte.sputnik.instrument.BMUtil;
import com.zte.sputnik.lbs.LoggerBuilder;
import lombok.SneakyThrows;
import shade.sputnik.org.slf4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;

/**
 * @author zscgrhg
 */
public class SputnikMain {


    private static final Logger LOGGER = LoggerBuilder.of(SputnikMain.class);

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
        Optional.ofNullable(SputnikMain.class.getClassLoader().getResourceAsStream("sputnik.properties"))
                .ifPresent(p->{
                    try {
                        config.load(p);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

        return config;
    }

    @SneakyThrows
    public synchronized static void loadAgent()  {
        LOGGER.info("start load agent");
        loadTtlAgent();
        BMUtil.loadAgent();
    }

    @SneakyThrows
    public synchronized static void loadBytemanAgent()  {
        BMUtil.loadAgent();
    }

    @SneakyThrows
    public synchronized static void loadTtlAgent() {
       if(!TtlAgent.isTtlAgentLoaded()){
           try {
               VirtualMachine jvm = VirtualMachine.attach(String.valueOf(BMUtil.getPid()));
               Path agentPath = Paths.get(TtlAgent.class.getProtectionDomain().getCodeSource().getLocation().toURI());
               jvm.loadAgent(agentPath.toString());
           } catch (AgentInitializationException e) {
               // this probably indicates that the agent is already installed
           }
       }
    }

}
