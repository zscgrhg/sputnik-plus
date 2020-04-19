package com.zte.sputnik.instrument;


import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.tools.attach.AgentInitializationException;
import com.zte.sputnik.SputnikMain;
import com.zte.sputnik.lbs.LoggerBuilder;
import com.zte.sputnik.util.JsonUtil;
import lombok.SneakyThrows;
import org.jboss.byteman.agent.install.Install;
import org.jboss.byteman.agent.submit.ScriptText;
import org.jboss.byteman.agent.submit.Submit;
import org.jboss.byteman.contrib.bmunit.BMUnitConfig;
import org.jboss.byteman.contrib.bmunit.BMUnitConfigState;
import shade.sputnik.org.slf4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.*;

public class BMUtil {
    private static final Logger LOGGER = LoggerBuilder.of(BMUtil.class);
    public static final int AGENT_PORT=findFreeSocketPort();
    public static final String AGENT_HOST="localhost";
    public static final BMUnitConfig DEFAULT_CONFIG=createConfig();

    private static BMUnitConfig createConfig(){
        Map<String,Object> value=new HashMap<>();
        value.put("enforce",true);
        value.put("agentPort",Integer.toString(AGENT_PORT));
        value.put("agentHost",AGENT_HOST);
        value.put("inhibitAgentLoad",true);
        value.put("loadDirectory","");
        value.put("resourceLoadDirectory","");
        value.put("allowAgentConfigUpdate",false);
        value.put("verbose",verbose());
        value.put("debug",debug());
        value.put("bmunitVerbose",verbose());
        value.put("policy",false);
        value.put("dumpGeneratedClasses",false);
        value.put("dumpGeneratedClassesDirectory","");
        value.put("dumpGeneratedClassesIntermediate",false);
        return RuntimeAnnotations.buildFromMap(BMUnitConfig.class,value);
    }

    public static int getPort() {
        return AGENT_PORT;
    }


    public static String getHost() {
        return AGENT_HOST;
    }

    public static int getPid() {
        int pid;
        try {
            pid = Kernel32.INSTANCE.GetCurrentProcessId();
        } catch (Exception e) {
            pid = CLibrary.INSTANCE.getpid();
        }
        LOGGER.debug("CurrentProcessId=" + pid);
        return pid;
    }

    public static boolean verbose(){
        String verbose= SputnikMain.CONFIG.getProperty("sputnik.byteman.verbose","false");
        return "true".equalsIgnoreCase(verbose);
    }
    public static boolean debug(){
        String debug= SputnikMain.CONFIG.getProperty("sputnik.byteman.debug","false");
        return "true".equalsIgnoreCase(debug);
    }

    public static void loadAgent() throws Exception {
        if(Install.isAgentAttached(Integer.toString(getPid()))){
            LOGGER.debug("agent already loaded!");
            return;
        }


        Properties p=new Properties();
        p.put("org.jboss.byteman.contrib.bmunit.agent.host",AGENT_HOST);
        p.put("org.jboss.byteman.contrib.bmunit.agent.port",AGENT_PORT);
        p.put("org.jboss.byteman.contrib.bmunit.agent.inhibit",true);
        if(verbose()){
            p.put("org.jboss.byteman.verbose", "true");
        }
        if(debug()){
            p.put("org.jboss.byteman.debug", "true");
        }
        p.put("org.jboss.byteman.transform.all", "true");
        p.put("org.jboss.byteman.compileToBytecode", "true");

        System.getProperties().putAll(p);
        String id = String.valueOf(getPid());
        try {
            System.out.println("BMUnit : loading agent id = " + id);
            Properties properties = new Properties();
            properties.setProperty("org.jboss.byteman.transform.all", "true");
            properties.setProperty("org.jboss.byteman.debug", "true");
            Submit submit = new Submit(getHost(), getPort());
            int size = properties.size();
            String[] proparray = new String[size];
            int i = 0;
            for (String key : properties.stringPropertyNames()) {
                proparray[i++] = key + "=" + properties.getProperty(key);
            }
            Install.install(id, true, false, getHost(), getPort(), proparray);
            LOGGER.debug("bm:agent successfully installed at pid {} port {}",AGENT_HOST,AGENT_PORT);
            submit.setSystemProperties(properties);
            BMUnitConfigState.pushConfigurationState(DEFAULT_CONFIG,(Class)null);
            LOGGER.debug("bm:config pushed.");
        } catch (AgentInitializationException e) {
            // this probably indicates that the agent is already installed
        }
    }

    @SneakyThrows
    public static void submitFile(String btm) {
        Submit submit = new Submit(getHost(), getPort());
        List<String> files = new ArrayList<String>();
        files.add(btm);
        LOGGER.debug("BMUnit : loading file script = " + btm);
        submit.addRulesFromFiles(files);
    }

    @SneakyThrows
    public static void unload(String btm) {
        Submit submit = new Submit(getHost(), getPort());
        List<String> files = new ArrayList<String>();
        files.add(btm);
        LOGGER.debug("BMUnit : unloading file script = " + btm);
        submit.deleteRulesFromFiles(files);
    }


    private interface CLibrary extends Library {
        CLibrary INSTANCE = Native.load("c", CLibrary.class);

        int getpid();
    }


    @SneakyThrows
    public static void submitText(List<ScriptText> scripts) {
        Submit submit = new Submit(getHost(), getPort());
        LOGGER.debug("submit" + JsonUtil.write(scripts));
        submit.addScripts(scripts);
    }

    @SneakyThrows
    public static void unloadText(List<ScriptText> scripts) {
        Submit submit = new Submit(getHost(), getPort());
        LOGGER.debug("unload" + JsonUtil.write(scripts));
        submit.deleteScripts(scripts);
    }

    public static int findFreeSocketPort(){
        try {
            ServerSocket socket = new ServerSocket(0);
            socket.close();
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
