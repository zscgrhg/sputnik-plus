package com.zte.sputnik.instrument;


import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.tools.attach.AgentInitializationException;
import com.zte.sputnik.lbs.LoggerBuilder;
import com.zte.sputnik.util.JsonUtil;
import lombok.SneakyThrows;

import org.jboss.byteman.agent.install.Install;
import org.jboss.byteman.agent.submit.ScriptText;
import org.jboss.byteman.agent.submit.Submit;
import shade.sputnik.org.slf4j.Logger;


import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class BMUtil {
    private static final Logger LOGGER = LoggerBuilder.of(BMUtil.class);

    public static int getPort() {
        return 8848;
    }


    public static String getHost() {
        return "localhost";
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

    public static void loadAgent() throws Exception {
        System.setProperty("org.jboss.byteman.verbose, org.jboss.byteman.debug", "true");
        System.setProperty("org.jboss.byteman.transform.all", "true");
        System.setProperty("org.jboss.byteman.compileToBytecode", "true");
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
            submit.setSystemProperties(properties);
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


}
