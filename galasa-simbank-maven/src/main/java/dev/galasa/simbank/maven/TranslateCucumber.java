/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2020.
 */
package dev.galasa.simbank.maven;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.plugins.annotations.LifecyclePhase;

@Mojo(name = "translatecucumber", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class TranslateCucumber extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(defaultValue="${project.compileSourceRoots}")
    private List<String> projectSources;

    private List<File> sourceFiles = new ArrayList<File>();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("TranslateCucumber: Generating Sources " + project.getName());

        for(String f : projectSources) {
            System.out.println(f);
            File source = new File(f);
            checkForSource(source);
        }

        for (File cucumberSource : sourceFiles) {
            getLog().info(cucumberSource.getName());
            try {
                generateGalasaTest(cucumberSource);
            } catch (Exception e) {
                throw new MojoExecutionException(
                        "Error processing feature file " + cucumberSource.getName(), e);
            }
        }
    }

    private void generateGalasaTest(File source) throws IOException {
        getLog().info("TranslateCucumber: Generating Test Class: " + source.getName());

        File outputDirectory = source.getParentFile();

        List<String> sourceLines = new ArrayList<String>();
        BufferedReader br = new BufferedReader(new FileReader(source));
        String st;
        while((st = br.readLine()) != null) {
            sourceLines.add(st);
        }
        br.close();

        File[] oldVersions = outputDirectory.listFiles(new JavaFilter(source.getName().substring(0, source.getName().indexOf('.'))));
        if(oldVersions != null) {
            for(File old : oldVersions) {
                old.delete();
            }
        }

        System.out.println(outputDirectory.getPath() + "/" + source.getName().substring(0, source.getName().indexOf('.')) + ".java");
        File generated = new File(outputDirectory.getPath() + "/" + source.getName().substring(0, source.getName().indexOf('.')) + ".java");
        generated.createNewFile();

        FileWriter writer = new FileWriter(generated);
        for(String line : sourceLines) {
            writer.write(line);
            writer.write("\n");
        }
        writer.close();
    }

    private void checkForSource(File directory) {
        File[] features = directory.listFiles(new FeatureFilter(".feature"));
        if(features != null)
            sourceFiles.addAll(Arrays.asList(features));
        File[] children = directory.listFiles();
        if(children != null) {
            for(File child : children) {
                checkForSource(child);
            }
        }
    }

	private static class FeatureFilter implements FilenameFilter {

		private String extension;

		public FeatureFilter(String extension) {
			this.extension = extension.toLowerCase();
		}

		@Override
		public boolean accept(File dir, String name) {
			return name.toLowerCase().endsWith(extension);
		}

    }
    
    private static class JavaFilter implements FilenameFilter {

		private String senarioName;

		public JavaFilter(String senarioName) {
			this.senarioName = senarioName.toLowerCase();
		}

		@Override
		public boolean accept(File dir, String name) {
			return name.toLowerCase().equals(this.senarioName + ".java");
		}

	}
    
}