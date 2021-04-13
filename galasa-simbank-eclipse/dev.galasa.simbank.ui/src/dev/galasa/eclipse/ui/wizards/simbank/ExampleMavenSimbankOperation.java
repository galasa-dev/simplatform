/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019,2021.
 */
package dev.galasa.eclipse.ui.wizards.simbank;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.m2e.core.ui.internal.UpdateMavenProjectJob;
import org.osgi.framework.Bundle;

import dev.galasa.eclipse.simbank.SimBankActivator;

public class ExampleMavenSimbankOperation implements IRunnableWithProgress {

    private final String prefix;

    public ExampleMavenSimbankOperation(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }

        String testProjectName = prefix + ".tests";
        String managerProjectName = prefix + ".manager";

        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

        try {
            IProject testProject = root.getProject(testProjectName);
            testProject.create(monitor);
            testProject.open(monitor);

            IProject managerProject = root.getProject(managerProjectName);
            managerProject.create(monitor);
            managerProject.open(monitor);

            Bundle bundle = SimBankActivator.getInstance().getBundle();
            IPath path = new Path("lib/galasa-simbanktests-parent-examples.zip");
            URL zipUrl = FileLocator.find(bundle, path, null);
            if (zipUrl == null) {
                throw new CoreException(new Status(Status.ERROR, SimBankActivator.PLUGIN_ID,
                        "The galasa-simbanktests-parent-examples.zip file is missing from the plugin"));
            }
            zipUrl = FileLocator.toFileURL(zipUrl);
            java.nio.file.Path pathZip = Paths.get(toUri(zipUrl));

            try (ZipFile zip = new ZipFile(pathZip.toFile())) {
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();

                    String fileName = entry.getName();
                    String[] fileNameParts = fileName.split("/");
                    if (fileNameParts.length < 3) {
                        continue;
                    }

                    if (!"examples".equals(fileNameParts[0])) {
                        continue;
                    }

                    IProject outputProject = null;
                    if ("dev.galasa.simbank.tests".equals(fileNameParts[1])) {
                        outputProject = testProject;
                    } else if ("dev.galasa.simbank.manager".equals(fileNameParts[1])) {
                        outputProject = managerProject;
                    } else {
                        continue;
                    }

                    String[] fileNameRemainingParts = Arrays.copyOfRange(fileNameParts, 2, fileNameParts.length);

                    IContainer container = outputProject;
                    for (int i = 0; i < fileNameRemainingParts.length - 1; i++) {
                        IFolder folder = container.getFolder(new Path(fileNameRemainingParts[i]));
                        if (!folder.exists()) {
                            folder.create(true, true, monitor);
                        }
                        container = folder;
                    }

                    if (!entry.isDirectory()) {
                        String name = fileNameRemainingParts[fileNameRemainingParts.length - 1];

                        if ("pom-example.xml".equals(name)) {
                            name = "pom.xml";

                            String prePrefix = IOUtils.toString(zip.getInputStream(entry));
                            prePrefix = prePrefix.replaceAll("\\Q%%prefix%%\\E", prefix);
                            ByteArrayInputStream bais = new ByteArrayInputStream(prePrefix.getBytes("utf-8"));
                            IFile file = container.getFile(new Path(name));
                            file.create(bais, IResource.NONE, monitor);
                        } else if ("bnd-example.bnd".equals(name)) {
                        	continue;
                        } else if ("build-example.gradle".equals(name)) {
                        	continue;
                        } else if ("settings-example.gradle".equals(name)) {
                        	continue;
                        } else {
                            IFile file = container.getFile(new Path(name));
                            file.create(zip.getInputStream(entry), IResource.NONE, monitor);
                        }
                    }
                }
            }

            IProjectDescription testDescription = testProject.getDescription();
            testDescription.setNatureIds(new String[] { "org.eclipse.m2e.core.maven2Nature" });
            testDescription.setBuildConfigs(new String[] { "org.eclipse.m2e.core.maven2Builder" });
            testProject.setDescription(testDescription, monitor);

            IProjectDescription managerDescription = managerProject.getDescription();
            managerDescription.setNatureIds(new String[] { "org.eclipse.m2e.core.maven2Nature" });
            managerDescription.setBuildConfigs(new String[] { "org.eclipse.m2e.core.maven2Builder" });
            managerProject.setDescription(managerDescription, monitor);

            @SuppressWarnings("restriction")
            UpdateMavenProjectJob job = new UpdateMavenProjectJob(new IProject[] { testProject, managerProject });// TODO
                                                                                                                  // find
                                                                                                                  // official
                                                                                                                  // way
                                                                                                                  // to
                                                                                                                  // do
                                                                                                                  // this
            job.schedule();

        } catch (Throwable t) {
            SimBankActivator.log(t);
        }

    }
    
    public static URI toUri(URL url) throws URISyntaxException {
        String sUrl = url.toString();
        
        sUrl = sUrl.replaceAll(" ", "%20");
        
        return new URI(sUrl);
    }


}
