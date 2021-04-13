/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2021.
 */
package dev.galasa.eclipse.ui.wizards.simbank;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
import org.osgi.framework.Bundle;

import dev.galasa.eclipse.simbank.SimBankActivator;

public class ExampleGradleSimbankOperation implements IRunnableWithProgress {

	private final String prefix;

	public ExampleGradleSimbankOperation(String prefix) {
		this.prefix = prefix;
	}

	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}

		String parentProjectName = prefix + ".parent";
		String testProjectName = prefix + ".tests";
		String managerProjectName = prefix + ".manager";

		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

		try {
			IProject parentProject = root.getProject(parentProjectName);
			parentProject.create(monitor);
			parentProject.open(monitor);

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

					IFolder container = null;
					if ("dev.galasa.simbank.tests".equals(fileNameParts[1])) {
						container = parentProject.getFolder(testProjectName);
						if (!container.exists()) {
							container.create(true, true, monitor);
						}
					} else if ("dev.galasa.simbank.manager".equals(fileNameParts[1])) {
						container = parentProject.getFolder(managerProjectName);
						if (!container.exists()) {
							container.create(true, true, monitor);
						}
					} else {
						continue;
					}

					String[] fileNameRemainingParts = Arrays.copyOfRange(fileNameParts, 2, fileNameParts.length);

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
							continue;
						} else if ("bnd-example.bnd".equals(name)) {
							convertFile(zip, container, entry, "bnd.bnd", monitor);
						} else if ("build-example.gradle".equals(name)) {
							convertFile(zip, container, entry, "build.gradle", monitor);
						} else if ("settings-example.gradle".equals(name)) {
							convertFile(zip, container, entry, "settings.gradle", monitor);
						} else {
							IFile file = container.getFile(new Path(name));
							file.create(zip.getInputStream(entry), IResource.NONE, monitor);
						}
					}
				}
			}

			// Create settings.gradle files for the parent project

			StringBuilder sb = new StringBuilder();
			sb.append("include '");
			sb.append(managerProjectName);
			sb.append("'\n");
			sb.append("include '");
			sb.append(testProjectName);
			sb.append("'\n");
			ByteArrayInputStream bais = new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));
			IFile settingsGradle = parentProject.getFile("settings.gradle");
			settingsGradle.create(bais, IResource.NONE, monitor);


			// Create the .settings folder for gradle
			createBuildshipSettings(parentProject, monitor);


			IProjectDescription testDescription = parentProject.getDescription();
			testDescription.setNatureIds(new String[] { "org.eclipse.buildship.core.gradleprojectnature" });
			testDescription.setBuildConfigs(new String[] { "org.eclipse.buildship.core.gradleprojectbuilder" });
			parentProject.setDescription(testDescription, monitor);


			IProject managerProject = root.getProject(managerProjectName);
			IProjectDescription description = managerProject.getWorkspace().newProjectDescription(managerProjectName);
			description.setLocation(parentProject.getFolder(managerProjectName).getLocation());
			description.setNatureIds(new String[] { "org.eclipse.jdt.core.javanature",
			"org.eclipse.buildship.core.gradleprojectnature" });
			description.setBuildConfigs(new String[] { "org.eclipse.jdt.core.javabuilder",
			"org.eclipse.buildship.core.gradleprojectbuilder" });
			managerProject.create(description, monitor);
			managerProject.open(monitor);


			IProject testProject = root.getProject(testProjectName);
			description = managerProject.getWorkspace().newProjectDescription(testProjectName);
			description.setLocation(parentProject.getFolder(testProjectName).getLocation());
			description.setNatureIds(new String[] { "org.eclipse.jdt.core.javanature",
			"org.eclipse.buildship.core.gradleprojectnature" });
			description.setBuildConfigs(new String[] { "org.eclipse.jdt.core.javabuilder",
			"org.eclipse.buildship.core.gradleprojectbuilder" });
			testProject.create(description, monitor);
			testProject.open(monitor);


			createBuildshipSettings(managerProject, monitor);
			createBuildshipSettings(testProject, monitor);
		} catch (Throwable t) {
			SimBankActivator.log(t);
		}

	}

	private void convertFile(ZipFile zip, IContainer container, ZipEntry entry, String toFile, IProgressMonitor monitor) throws IOException, CoreException {
		String prePrefix = IOUtils.toString(zip.getInputStream(entry), StandardCharsets.UTF_8.name());
		prePrefix = prePrefix.replaceAll("\\Q%%prefix%%\\E", prefix);
		ByteArrayInputStream bais = new ByteArrayInputStream(prePrefix.getBytes("utf-8"));
		IFile file = container.getFile(new Path(toFile));
		file.create(bais, IResource.NONE, monitor);
	}

	public static URI toUri(URL url) throws URISyntaxException {
		String sUrl = url.toString();

		sUrl = sUrl.replaceAll(" ", "%20");

		return new URI(sUrl);
	}


	private void createBuildshipSettings(IProject project, IProgressMonitor monitor) throws CoreException {
		IFolder settings = project.getFolder(".settings");
		if (!settings.exists()) {
			settings.create(true, true, monitor);
		}

		StringBuilder sb = new StringBuilder();
		sb.append("connection.project.dir=\n");
		sb.append("eclipse.preferences.version=1\n");

		ByteArrayInputStream bais = new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));

		IFile buildship = settings.getFile("org.eclipse.buildship.core.prefs");
		buildship.create(bais, IResource.NONE, monitor);

	}


}
