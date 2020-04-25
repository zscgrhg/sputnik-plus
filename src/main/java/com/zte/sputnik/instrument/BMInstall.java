package com.zte.sputnik.instrument;

/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010 Red Hat and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 * (C) 2010,
 * @authors Andrew Dinn
 */


import com.sun.tools.attach.*;
import lombok.SneakyThrows;
import org.jboss.byteman.agent.Main;
import org.jboss.byteman.agent.install.VMInfo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.jar.JarFile;

/**
 * A program which uses the sun.com.tools.attach.VirtualMachine class to install the Byteman agent into a
 * running JVM. This provides an alternative to using the -javaagent option to install the agent.
 */
public class BMInstall
{
    /**
     * main routine for use from command line
     *
     *
     * Install [-h host] [-p port] [-b] [-s] [-m] [-Dorg.jboss.Byteman.xxx]* pid
     *
     *
     * see method {@link #usage} for details of the command syntax
     * @param args the command options
     */
    public static void main(String[] args)
    {
        BMInstall attachTest = new BMInstall();
        attachTest.parseArgs(args);
        try {
            attachTest.locateAgent();
            attachTest.attach();
            attachTest.injectAgent();
        } catch (Exception e) {
            System.out.println(e);
            System.exit(1);
        }
    }

    /**
     * compatability mode
     * @param pid the process id of the JVM into which the agent should be installed or 0 for this JVM
     * @param addToBoot true if the agent jar should be installed into the bootstrap classpath
     * @param host the hostname to be used by the agent listener or null for localhost
     * @param port the port to be used by the agent listener or 0 for the default port
     * @param properties an array of System properties to be installed by the agent with optional values e.g.
     * values such as "org.jboss.byteman.verbose" or "org.jboss.byteman.dump.generated.classes.directory=./dump"
     * @throws IllegalArgumentException if any of the arguments  is invalid
     * @throws FileNotFoundException if the agent jar cannot be found using the environment variable BYTEMAN_HOME
     * or the System property org.jboss.byteman.home and cannot be located in the current classpath
     * @throws IOException if the byteman jar cannot be opened or uploaded to the requested JVM
     * @throws AttachNotSupportedException if the requested JVM cannot be attached to
     * @throws AgentLoadException if an error occurs during upload of the agent into the JVM
     * @throws AgentInitializationException if the agent fails to initialize after loading. this almost always
     * indicates that the agent is already loaded into the JVM
     */
    public static void install(String pid, boolean addToBoot, String host, int  port, String[] properties)
            throws IllegalArgumentException, FileNotFoundException,
            IOException, AttachNotSupportedException,
            AgentLoadException, AgentInitializationException
    {
        install(pid, addToBoot, false, host, port, properties);
    }

    /**
     * compatability mode
     * @param pid the process id of the JVM into which the agent should be installed or 0 for this JVM
     * @param addToBoot true if the agent jar should be installed into the bootstrap classpath
     * @param setPolicy true if the agent jar should set an access-all-areas securityPolicy
     * @param host the hostname to be used by the agent listener or null for localhost
     * @param port the port to be used by the agent listener or 0 for the default port
     * @param properties an array of System properties to be installed by the agent with optional values e.g.
     * values such as "org.jboss.byteman.verbose" or "org.jboss.byteman.dump.generated.classes.directory=./dump"
     * @throws IllegalArgumentException if any of the arguments  is invalid
     * @throws FileNotFoundException if the agent jar cannot be found using the environment variable BYTEMAN_HOME
     * or the System property org.jboss.byteman.home and cannot be located in the current classpath
     * @throws IOException if the byteman jar cannot be opened or uploaded to the requested JVM
     * @throws AttachNotSupportedException if the requested JVM cannot be attached to
     * @throws AgentLoadException if an error occurs during upload of the agent into the JVM
     * @throws AgentInitializationException if the agent fails to initialize after loading. this almost always
     * indicates that the agent is already loaded into the JVM
     */
    public static void install(String pid, boolean addToBoot, boolean setPolicy, String host, int  port, String[] properties)
            throws IllegalArgumentException, FileNotFoundException,
            IOException, AttachNotSupportedException,
            AgentLoadException, AgentInitializationException
    {
        install(pid, addToBoot, setPolicy, false, host, port, properties);
    }

    /**
     * compatability mode
     * @param pid the process id of the JVM into which the agent should be installed or 0 for this JVM
     * @param addToBoot true if the agent jar should be installed into the bootstrap classpath
     * @param setPolicy true if the agent jar should set an access-all-areas securityPolicy
     * @param useModuleLoader true if the JBoss module loader mode should be configured
     * @param host the hostname to be used by the agent listener or null for localhost
     * @param port the port to be used by the agent listener or 0 for the default port
     * @param properties an array of System properties to be installed by the agent with optional values e.g.
     * values such as "org.jboss.byteman.verbose" or "org.jboss.byteman.dump.generated.classes.directory=./dump"
     * @throws IllegalArgumentException if any of the arguments  is invalid
     * @throws FileNotFoundException if the agent jar cannot be found using the environment variable BYTEMAN_HOME
     * or the System property org.jboss.byteman.home and cannot be located in the current classpath
     * @throws IOException if the byteman jar cannot be opened or uploaded to the requested JVM
     * @throws AttachNotSupportedException if the requested JVM cannot be attached to
     * @throws AgentLoadException if an error occurs during upload of the agent into the JVM
     * @throws AgentInitializationException if the agent fails to initialize after loading. this almost always
     * indicates that the agent is already loaded into the JVM
     */
    public static void install(String pid, boolean addToBoot, boolean setPolicy, boolean useModuleLoader, String host, int  port, String[] properties)
            throws IllegalArgumentException, FileNotFoundException,
            IOException, AttachNotSupportedException,
            AgentLoadException, AgentInitializationException
    {


        if (port < 0) {
            throw new IllegalArgumentException("Install : port cannot be negative");
        }

        for (int i = 0; i < properties.length; i++) {
            String prop = properties[i];
            if (prop == null || prop.length()  == 0) {
                throw new IllegalArgumentException("Install : properties  cannot be null or \"\"");
            }
            if (prop.indexOf(',') >= 0) {
                throw new IllegalArgumentException("Install : properties may not contain ','");
            }
        }

        BMInstall install = new BMInstall(pid, addToBoot, setPolicy, useModuleLoader, host, port, properties);
        install.locateAgent();
        install.attach();
        install.injectAgent();
    }

    public static VMInfo[] availableVMs()
    {
        List<VirtualMachineDescriptor> vmds = VirtualMachine.list();
        VMInfo[] vmInfo = new VMInfo[vmds.size()];
        int i = 0;
        for (VirtualMachineDescriptor vmd : vmds) {
            vmInfo[i++] = new VMInfo(vmd.id(), vmd.displayName());
        }

        return vmInfo;
    }

    /**
     * attach to the virtual machine identified by id and return the value of the named property. id must
     * be the id of a virtual machine returned by method availableVMs.
     * @param id the id of the machine to attach to
     * @param property the proeprty to be retrieved
     * @return the value of the property or null if it is not set
     */
    public static String getSystemProperty(String id, String property)
    {
        return getProperty(id, property);
    }

    /**
     * attach to the virtual machine identified by id and return {@code true} if a Byteman
     * agent has already been attached to it. id must be the id of a virtual machine
     * returned by method availableVMs.
     * @param id the id of the machine to attach to
     * @return {@code true} if and only if a Byteman agent has already been attached to the
     * virtual machine.
     */
    public static boolean isAgentAttached(String id)
    {
        String value = getProperty(id, BYTEMAN_AGENT_LOADED_PROPERTY);
        return Boolean.parseBoolean(value);
    }

    private static String getProperty(String id, String property)
    {
        VirtualMachine vm = null;
        try {
            vm = VirtualMachine.attach(id);
            String value = (String)vm.getSystemProperties().get(property);
            return value;
        } catch (AttachNotSupportedException e) {
            return null;
        } catch (IOException e) {
            return null;
        } finally {
            if (vm != null) {
                try {
                    vm.detach();
                } catch (IOException e) {
                    // ignore;
                }
            }
        }
    }

    /**
     *  only this class creates instances
     */
    private BMInstall()
    {
        agentJar = null;
        id = null;
        port = 0;
        addToBoot = false;
        props="";
        vm = null;
    }

    /**
     * compatibility mode
     */
    private BMInstall(String pid, boolean addToBoot, String host, int port, String[] properties)
    {
        this(pid, addToBoot, false, false, host, port, properties);
    }

    /**
     *  only this class creates instances
     */
    private BMInstall(String pid, boolean addToBoot, boolean setPolicy, boolean useModuleLoader, String host, int port, String[] properties)
    {
        agentJar = null;
        modulePluginJar = null;
        this.id = pid;
        this.port = port;
        this.addToBoot = addToBoot;
        this.setPolicy = setPolicy;
        this.useModuleLoader = useModuleLoader;
        this.host = host;
        if (properties != null) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < properties.length; i++) {
                builder.append(",prop:");
                builder.append(properties[i]);
            }
            props = builder.toString();
        } else {
            props = "";
        }
        vm = null;
    }

    /**
     * check the supplied arguments and stash away the relevant data
     * @param args the value supplied to main
     */
    private void parseArgs(String[] args)
    {
        int argCount = args.length;
        int idx = 0;
        if (idx == argCount) {
            usage(0);
        }

        String nextArg = args[idx];

        while (nextArg.length() != 0 &&
                nextArg.charAt(0) == '-') {
            if (nextArg.equals("-p")) {
                idx++;
                if (idx == argCount) {
                    usage(1);
                }
                nextArg = args[idx];
                idx++;
                try {
                    port = Integer.decode(nextArg);
                } catch (NumberFormatException e) {
                    System.out.println("Install : invalid value for port " + nextArg);
                    usage(1);
                }
            } else if (nextArg.equals("-h")) {
                idx++;
                if (idx == argCount) {
                    usage(1);
                }
                nextArg = args[idx];
                idx++;
                host = nextArg;
            } else if (nextArg.equals("-b")) {
                idx++;
                addToBoot = true;
            } else if (nextArg.equals("-s")) {
                idx++;
                setPolicy = true;
            } else if (nextArg.equals("-m")) {
                idx++;
                useModuleLoader = true;
            } else if (nextArg.startsWith("-D")) {
                idx++;
                String prop=nextArg.substring(2);
                if (!prop.startsWith(BYTEMAN_PREFIX) || prop.contains(",")) {
                    System.out.println("Install : invalid property setting " + prop);
                    usage(1);
                }
                props = props + ",prop:" + prop;
            } else if (nextArg.equals("--help")) {
                usage(0);
            } else {
                System.out.println("Install : invalid option " + args[idx]);
                usage(1);
            }
            if (idx == argCount) {
                usage(1);
            }
            nextArg = args[idx];
        }

        if (idx != argCount - 1) {
            usage(1);
        }

        // we actually allow any string for the process id as we can look up by name also
        id = nextArg;
    }

    /**
     * Check for system property org.jboss.byteman.home in preference to the environment setting
     * BYTEMAN_HOME and use it to identify the location of the byteman agent jar.
     */
    @SneakyThrows
    private void locateAgent()  {
        // use the current system property in preference to the environment setting
        Path agentPath = Paths.get(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        agentJar=agentPath.toString();
    }

    public String locateJarFromHomeDir(String bmHome, String baseDir, String libName) throws IOException
    {
        if (bmHome.endsWith("/")) {
            bmHome = bmHome.substring(0, bmHome.length() - 1);
        }

        File bmHomeFile = new File(bmHome);
        if (!bmHomeFile.isDirectory()) {
            throw new FileNotFoundException("Install : " + bmHome + " does not identify a directory");
        }

        File bmLibFile = new File(bmHome + "/" + baseDir);
        if (!bmLibFile.isDirectory()) {
            throw new FileNotFoundException("Install : " + bmHome + "/" + baseDir + " does not identify a directory");
        }

        try {
            JarFile jarFile = new JarFile(bmHome + "/" + baseDir + "/" + libName + ".jar");
        } catch (IOException e) {
            throw new IOException("Install : " + bmHome + "/" + baseDir + "/" + libName + ".jar is not a valid jar file", e);
        }

        return bmHome + "/" + baseDir + "/" + libName + ".jar";
    }

    public String locateJarFromClasspath(String libName) throws IOException
    {
        String javaClassPath = System.getProperty("java.class.path");
        String pathSepr = System.getProperty("path.separator");
        String fileSepr = System.getProperty("file.separator");
        final String EXTENSION = ".jar";
        final int EXTENSION_LEN = EXTENSION.length();
        final int NAME_LEN = libName.length();
        final String VERSION_PATTERN = "-[0-9]+\\.[0-9]+\\.[0-9]+.*";

        String[] elements = javaClassPath.split(pathSepr);
        String jarname = null;
        for (String element : elements) {
            if (element.endsWith(EXTENSION)) {
                String name = element.substring(0, element.length() - EXTENSION_LEN);
                int lastFileSepr = name.lastIndexOf(fileSepr);
                if (lastFileSepr >= 0) {
                    name= name.substring(lastFileSepr+1);
                }
                if (name.startsWith(libName)) {
                    if (name.length() == NAME_LEN) {
                        jarname = element;
                        break;
                    }
                    //  could be a contender --  check it only has a standard version suffix
                    // i.e. "-NN.NN.NN-ANANAN"
                    String version = name.substring(NAME_LEN);
                    if (version.matches(VERSION_PATTERN)) {
                        jarname =  element;
                        break;
                    }
                }
            }
        }

        if (jarname != null) {
            System.out.println(libName + " jar is " + jarname);
            return jarname;
        } else {
            throw new  FileNotFoundException("Install : cannot find " + libName + " jar please set environment variable " + BYTEMAN_HOME_ENV_VAR + " or System property " + BYTEMAN_HOME_SYSTEM_PROP);
        }
    }

    /**
     * attach to the Java process identified by the process id supplied on the command line
     */
    private void attach() throws AttachNotSupportedException, IOException, IllegalArgumentException
    {

        if (id.matches("[0-9]+")) {
            // integer process id
            int pid = Integer.valueOf(id);
            if (pid <= 0) {
                throw new IllegalArgumentException("Install : invalid pid " +id);
            }
            vm = VirtualMachine.attach(Integer.toString(pid));
        } else {
            // try to search for this VM with an exact match
            List<VirtualMachineDescriptor> vmds = VirtualMachine.list();
            for (VirtualMachineDescriptor vmd: vmds) {
                String displayName = vmd.displayName();
                int spacePos = displayName.indexOf(' ');
                if (spacePos > 0) {
                    displayName = displayName.substring(0, spacePos);
                }
                if (displayName.equals(id)) {
                    String pid = vmd.id();
                    vm = VirtualMachine.attach(vmd);
                    return;
                }
            }
            // hmm, ok, lets see if we can find a trailing match e.g. if the displayName
            // is org.jboss.Main we will accept jboss.Main or Main
            for (VirtualMachineDescriptor vmd: vmds) {
                String displayName = vmd.displayName();
                int spacePos = displayName.indexOf(' ');
                if (spacePos > 0) {
                    displayName = displayName.substring(0, spacePos);
                }

                if (displayName.indexOf('.') >= 0 && displayName.endsWith(id)) {
                    // looking hopeful ensure the preceding char is a '.'
                    int idx = displayName.length() - (id.length() + 1);
                    if (displayName.charAt(idx) == '.') {
                        // yes it's a match
                        String pid = vmd.id();
                        vm = VirtualMachine.attach(vmd);
                        return;
                    }
                }
            }

            // no match so throw an exception

            throw new IllegalArgumentException("Install : invalid pid " + id);
        }


    }

    /**
     * get the attached process to upload and install the agent jar using whatever agent options were
     * configured on the command line
     */
    private void injectAgent() throws AgentLoadException, AgentInitializationException, IOException
    {
        try {
            // we need at the very least to enable the listener so that scripts can be uploaded
            String agentOptions = "listener:true";
            if (host != null && host.length() != 0) {
                agentOptions += ",address:" + host;
            }
            if (port != 0) {
                agentOptions += ",port:" + port;
            }
            if (addToBoot) {
                agentOptions += ",boot:" + agentJar;
            }
            if (setPolicy) {
                agentOptions += ",policy:true";
            }
            if (useModuleLoader) {
                agentOptions += ",modules:org.jboss.byteman.modules.jbossmodules.JBossModulesSystem,sys:" + modulePluginJar;
            }
            if (props != null) {
                agentOptions += props;
            }

            vm.loadAgent(agentJar, agentOptions);
        } finally {
            vm.detach();
        }
    }

    /**
     * print usage information and exit with a specific exit code
     * @param exitValue the value to be supplied to the exit call
     */
    private static void usage(int exitValue)
    {
        System.out.println("usage : Install [-h host] [-p port] [-b] [-s] [-m] [-Dprop[=value]]* pid");
        System.out.println("        upload the byteman agent into a running JVM");
        System.out.println("    pid is the process id of the target JVM or the unique name of the process as reported by the jps -l command");
        System.out.println("    -h host selects the host name or address the agent listener binds to");
        System.out.println("    -p port selects the port the agent listener binds to");
        System.out.println("    -b adds the byteman jar to the bootstrap classpath");
        System.out.println("    -s sets an access-all-areas security policy for the Byteman agent code");
        System.out.println("    -m activates the byteman JBoss modules plugin");
        System.out.println("    -Dname=value can be used to set system properties whose name starts with \"org.jboss.byteman.\"");
        System.out.println("    expects to find a byteman agent jar in ${" + BYTEMAN_HOME_ENV_VAR + "}/lib/byteman.jar");
        System.out.println("    and JBoss modules plugin jar (if -m option is enabled)  in ${" + BYTEMAN_HOME_ENV_VAR + "}/contrib/jboss-modules-system/byteman-jboss-modules-plugin.jar");
        System.out.println("    (alternatively set System property " + BYTEMAN_HOME_SYSTEM_PROP + " to overide ${" + BYTEMAN_HOME_ENV_VAR + "})");
        System.exit(exitValue);
    }

    private String agentJar;
    private String modulePluginJar;
    private String id;
    private int port;
    private String host;
    private boolean addToBoot;
    private boolean setPolicy;
    private boolean useModuleLoader;
    private String props;
    private VirtualMachine vm;

    private static final String BYTEMAN_PREFIX="org.jboss.byteman.";

    private static final String BYTEMAN_AGENT_LOADED_PROPERTY = "org.jboss.byteman.agent.loaded";

    /**
     * System property used to idenitfy the location of the installed byteman release.
     */
    private static final String BYTEMAN_HOME_SYSTEM_PROP = BYTEMAN_PREFIX + "home";

    /**
     * environment variable used to idenitfy the location of the installed byteman release.
     */
    private static final String BYTEMAN_HOME_ENV_VAR = "BYTEMAN_HOME";

    /**
     * Base directory to look for agent jar.
     */
    private static final String BYTEMAN_AGENT_BASE_DIR = "lib";

    /**
     * Name of agent jar (without extension).
     */
    private static final String BYTEMAN_AGENT_NAME = "byteman";

    /**
     * Base directory to look for JBoss modules plugin jar.
     */
    private static final String BYTEMAN_MODULES_PLUGIN_BASE_DIR = "contrib/jboss-modules-system";

    /**
     * Name of JBoss modules plugin jar (without extension).
     */
    private static final String BYTEMAN_MODULES_PLUGIN_NAME = "byteman-jboss-modules-plugin";
}

