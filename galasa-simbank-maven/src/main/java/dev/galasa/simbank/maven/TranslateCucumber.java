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
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.inject.internal.Annotations;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.plugins.annotations.LifecyclePhase;

import dev.galasa.simbank.manager.ghrekin.CucumberSimbank;

@Mojo(name = "translatecucumber", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class TranslateCucumber extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${project.compileSourceRoots}")
    private List<String> projectSources;

    private List<File> sourceFiles = new ArrayList<File>();

    private File javaCode;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("TranslateCucumber: Generating Sources " + project.getName());

        for (Method t : CucumberSimbank.class.getDeclaredMethods())
            System.out.println(t.toString());


        javaCode = new File(projectSources.get(0));
        if(!javaCode.exists())
            javaCode.mkdirs();

        File testResources = new File(project.getBasedir() + "/src/test/resources");
        checkForSource(testResources);

        for (File cucumberSource : sourceFiles) {
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

        List<String> sourceLines = new ArrayList<String>();
        BufferedReader br = new BufferedReader(new FileReader(source));
        String st;
        while((st = br.readLine()) != null) {
            sourceLines.add(st);
        }
        br.close();

        File[] oldVersions = javaCode.listFiles(new JavaFilter(source.getName().substring(0, source.getName().indexOf('.'))));
        if(oldVersions != null) {
            for(File old : oldVersions) {
                old.delete();
            }
        }

        String className = source.getName().substring(0, source.getName().indexOf('.'));
        File generated = new File(javaCode.getPath() + "/" + className + ".java");
        generated.createNewFile();

        FileWriter writer = new FileWriter(generated);
        Boolean method = false;
        for(String line : sourceLines) {
            if(line.trim().indexOf(" ") >= 0) {
                switch (line.trim().substring(0, line.trim().indexOf(" "))) {
                    case "Feature:":
                        writer.write("import dev.galasa.Test;\n");
                        writer.write("@Test\n");
                        writer.write("public class " + className + " {");
                        writer.write("\n");
                        break;
                    case "Scenario:":
                        if(method)
                            writer.write("}\n");
                        method = true;
                        writer.write("@Test\n");
                        
                        writer.write("public void " + camelCase(line.substring(line.indexOf(":") + 2, line.length())) + "() {\n");
                        //TODO Write actual test methods
                        break;
                    default:
                        if(!line.trim().equals(""))
                            writer.write("// " + line + "\n");
                        break;
                }
            }
            // if(line.startsWith("Feature:")) {
            //     writer.write("import dev.galasa.Test;\n");
            //     writer.write("@Test\n");
            //     writer.write("public class " + className + " {");
            //     writer.write("\n");
            // } else if(line.trim().startsWith("Scenario:")) {
            //     if(method)
            //         writer.write("}\n");
            //     method = true;
            //     writer.write("@Test\n");
                
            //     writer.write("public void " + camelCase(line.substring(line.indexOf(":") + 2, line.length())) + "() {\n");
            //     //TODO Write actual test methods
            // } else {
            //     if(!line.trim().equals(""))
            //         writer.write("// " + line + "\n");
            // }
                
        }
        writer.write("}\n}");
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

    private String camelCase(String methodName) {
        String[] words = methodName.split(" ");
        methodName = words[0].toLowerCase();
        for(int i = 1; i < words.length; i++) {
            methodName += words[i].substring(0, 1).toUpperCase() + words[i].substring(1);
        }
        return methodName.replaceAll("[^a-zA-Z0-9_-]", "");
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