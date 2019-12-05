/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.simbank;

import java.io.File;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.JavaLaunchDelegate;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.osgi.framework.Bundle;

public class SimbankLauncher extends JavaLaunchDelegate {

    private MessageConsole console;
    private PrintStream    consoleDefault;
    private PrintStream    consoleRed;
    private PrintStream    consoleBlue;

    @Override
    public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor)
            throws CoreException {
        // *** Activate message console
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                activateMessageConsole();
            }
        });

        consoleDefault.append("\nLaunching SimBank\n");

        // *** Find all the information necessary to run
        File simbankJarFile = findSimbankJar();
        consoleDefault.append("Simbank jar is located at " + simbankJarFile.toURI().toString() + "\n");

        // *** Setup the Java running environment
        IVMRunner runner = getVMRunner(configuration, mode);

        // *** Only need the boot.jar on the classpath
        String[] classpath = new String[1];
        classpath[0] = simbankJarFile.toString();

        // *** From the config get any environment properties
        String[] envp = getEnvironment(configuration);

        // *** Setup our program arguments
        ArrayList<String> programArguments = new ArrayList<String>();

        // *** Get the vm args from the config
        ArrayList<String> vmArguments = new ArrayList<String>();
        String userVMArgs = getVMArguments(configuration);
        if (userVMArgs != null && !userVMArgs.isEmpty()) {
            String args[] = userVMArgs.split(" ");
            for (String s : args) {
                vmArguments.add(s.trim());
            }
        }
        // VM-specific attributes
        Map<String, Object> vmAttributesMap = getVMSpecificAttributesMap(configuration);

        // *** As can only use a classpath, need to provide main class
        String mainTypeName = "dev.galasa.simplatform.main.Simplatform";

        VMRunnerConfiguration runConfig = new VMRunnerConfiguration(mainTypeName, classpath);
        runConfig.setVMArguments((String[]) vmArguments.toArray(new String[vmArguments.size()]));
        runConfig.setProgramArguments((String[]) programArguments.toArray(new String[programArguments.size()]));
        runConfig.setEnvironment(envp);
        // runConfig.setWorkingDirectory(workingDirName);
        runConfig.setVMSpecificAttributesMap(vmAttributesMap);

        setDefaultSourceLocator(launch, configuration);

        runner.run(runConfig, launch, monitor);
    }

    private File findSimbankJar() throws CoreException {
        try {
            Bundle bundle = SimBankActivator.getInstance().getBundle();
            IPath path = new Path("lib/galasa-simplatform.jar");
            URL bootUrl = FileLocator.find(bundle, path, null);
            if (bootUrl == null) {
                throw new CoreException(new Status(Status.ERROR, SimBankActivator.PLUGIN_ID,
                        "The galasa-simplatform.jar is missing from the plugin"));
            }
            bootUrl = FileLocator.toFileURL(bootUrl);
            return Paths.get(toUri(bootUrl)).toFile().getAbsoluteFile();
        } catch (Exception e) {
            throw new CoreException(new Status(Status.ERROR, SimBankActivator.PLUGIN_ID,
                    "Problem locating the galasa-simplatform.jar in the plugin", e));
        }
    }

    /**
     * Activate message console
     */
    private void activateMessageConsole() {
        // Look for existing console
        ConsolePlugin consolePlugin = ConsolePlugin.getDefault();
        IConsoleManager consoleManager = consolePlugin.getConsoleManager();
        IConsole[] existingConsoles = consoleManager.getConsoles();
        for (IConsole existingConsole : existingConsoles) {
            if (existingConsole.getName().equals(SimBankActivator.PLUGIN_NAME)) {
                console = (MessageConsole) existingConsole;
                break;
            }
        }

        // Not found, create a new one
        if (console == null) {
            console = new MessageConsole(dev.galasa.eclipse.Activator.PLUGIN_NAME, null);
            consoleManager.addConsoles(new IConsole[] { console });
        }

        // activate console
        console.activate();

        // Create the default PrintStream
        MessageConsoleStream messageConsoleStreamDefault = console.newMessageStream();
        messageConsoleStreamDefault.setColor(null);
        consoleDefault = new PrintStream(messageConsoleStreamDefault, true);

        // Create a PrintStream for Red text
        MessageConsoleStream messageConsoleStreamRed = console.newMessageStream();
        messageConsoleStreamRed.setColor(new Color(null, new RGB(255, 0, 0)));
        consoleRed = new PrintStream(messageConsoleStreamRed, true);

        // Create a PrintStream for Blue text
        MessageConsoleStream messageConsoleStreamBlue = console.newMessageStream();
        messageConsoleStreamBlue.setColor(new Color(null, new RGB(0, 0, 255)));
        consoleBlue = new PrintStream(messageConsoleStreamBlue, true);
    }
    
    public static URI toUri(URL url) throws URISyntaxException {
        String sUrl = url.toString();
        
        sUrl = sUrl.replaceAll(" ", "%20");
        
        return new URI(sUrl);
    }


}
