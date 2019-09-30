package act.db.ebean;

/*-
 * #%L
 * ACT Ebean
 * %%
 * Copyright (C) 2015 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import act.Act;
import act.app.event.SysEventId;
import act.sys.Env;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import com.sun.tools.attach.spi.AttachProvider;
import org.avaje.agentloader.AgentLoader;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.S;
import sun.tools.attach.BsdVirtualMachine;
import sun.tools.attach.LinuxVirtualMachine;
import sun.tools.attach.SolarisVirtualMachine;
import sun.tools.attach.WindowsVirtualMachine;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public class EbeanAgentLoader extends AgentLoader {


    private static final Logger LOGGER = LogManager.get(EbeanAgentLoader.class.getName());

    private static final List<String> loaded = new ArrayList<String>();

    private static final AttachProvider ATTACH_PROVIDER = new AttachProvider() {
        @Override
        public String name() {
            return null;
        }

        @Override
        public String type() {
            return null;
        }

        @Override
        public VirtualMachine attachVirtualMachine(String id) {
            return null;
        }

        @Override
        public List<VirtualMachineDescriptor> listVirtualMachines() {
            return null;
        }
    };

    /**
     * Load an agent providing the full file path.
     */
    public static void loadAgent(String jarFilePath) {
        loadAgent(jarFilePath, "");
    }

    /**
     * Load an agent providing the full file path with parameters.
     */
    public static void loadAgent(String jarFilePath, String params) {
        try {

            String pid = Env.PID.get();

            VirtualMachine vm;
            if (AttachProvider.providers().isEmpty()) {
                vm = getVirtualMachineImplementationFromEmbeddedOnes(pid);
            } else {
                vm = VirtualMachine.attach(pid);
            }

            final PrintStream ps = System.out;
            try {
                System.setOut(new PrintStream(new FileOutputStream(".ebean_agent.log")));
                vm.loadAgent(jarFilePath, params);
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("javaagent loaded: " + jarFilePath);
                }
            } finally {
                // ensure ebean2 EnhanceContext logout set to dump output
                Act.jobManager().on(SysEventId.CLASS_LOADER_INITIALIZED,
                        S.buffer("EbeanAgentLoader - clean up for ").append(jarFilePath).toString(),
                        new Runnable() {
                    @Override
                    public void run() {
                        System.setOut(ps);
                    }
                });
            }
            vm.detach();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Load the agent from the classpath using its name.
     */
    public static void loadAgentFromClasspath(String agentName) {
        loadAgentFromClasspath(agentName, "");
    }

    /**
     * Load the agent from the classpath using its name and passing params.
     */
    public synchronized static boolean loadAgentFromClasspath(String agentName, String params) {
        if (loaded.contains(agentName)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(S.concat("agent already loaded: ", agentName));
            }
            // the agent is already loaded
            return true;
        }
        try {
            // Search for the agent jar in the classpath
            ClassLoader cl0 = AgentLoader.class.getClassLoader();
            if (!(cl0 instanceof URLClassLoader)) {
                cl0 = cl0.getParent();
            }
            if (cl0 instanceof URLClassLoader) {
                URLClassLoader cl = (URLClassLoader) (cl0);
                for (URL url : cl.getURLs()) {
                    if (isMatch(url, agentName)) {
                        // We have found the agent jar in the classpath
                        String fullName = url.toURI().getPath();
                        if (fullName.startsWith("/") && isWindows()) {
                            fullName = fullName.substring(1);
                        }
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace(S.concat("loading agent: ", fullName));
                        }
                        loadAgent(fullName, params);
                        loaded.add(agentName);
                        return true;
                    }
                }
            }

            // Agent not found and not loaded
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("agent not found");
            }
            return false;

        } catch (URISyntaxException use) {
            throw new RuntimeException(use);
        }
    }

    /**
     * Check to see if this url/jar matches our agent name.
     */
    private static boolean isMatch(URL url, String partial) {
        String fullPath = url.getFile();
        int lastSlash = fullPath.lastIndexOf('/');
        if (lastSlash < 0) {
            return false;
        }
        String jarName = fullPath.substring(lastSlash + 1);
        // Use startsWith so ignoring the version of the agent
        return jarName.startsWith(partial);
    }

    private static final boolean isWindows() {
        return File.separatorChar == '\\';
    }

    private static VirtualMachine getVirtualMachineImplementationFromEmbeddedOnes(String pid) {
        try {
            if (isWindows()) {
                return new WindowsVirtualMachine(ATTACH_PROVIDER, pid);
            }

            String osName = System.getProperty("os.name");

            if (osName.startsWith("Linux") || osName.startsWith("LINUX")) {
                return new LinuxVirtualMachine(ATTACH_PROVIDER, pid);

            } else if (osName.startsWith("Mac OS X")) {
                return new BsdVirtualMachine(ATTACH_PROVIDER, pid);

            } else if (osName.startsWith("Solaris")) {
                return new SolarisVirtualMachine(ATTACH_PROVIDER, pid);
            }

        } catch (AttachNotSupportedException e) {
            throw new RuntimeException(e);

        } catch (IOException e) {
            throw new RuntimeException(e);

        } catch (UnsatisfiedLinkError e) {
            throw new IllegalStateException("Native library for Attach API not available in this JRE", e);
        }

        return null;
    }

}
