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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.reflections.ReflectionUtils;
import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;

@Mojo(name = "translatecucumber", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME,
                                 requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
@SuppressWarnings("unchecked")
public class TranslateCucumber extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${project.compileClasspathElements}", readonly = true, required = true)
    private List<String> classpathElements;

    private List<File> sourceFiles = new ArrayList<File>();
    private File javaCode;

    private Set<Class<?>> translatorClasses;
    private Set<Method> whenMethods;
    private Set<Method> thenMethods;
    private Set<Field> givenFields;

    private ArrayList<String> usedVariables;
    private HashSet<String> uniqueDependencies;
    private HashSet<String> imports;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("TranslateCucumber: Generating Sources " + project.getName());

        javaCode = new File(project.getBasedir() + "/src/main/java/dev/galasa/ghrekin");
        if(!javaCode.exists())
            javaCode.mkdirs();

        getTranslatorClass();

        File testResources = new File(project.getBasedir() + "/src/test/resources");
        checkForSource(testResources, sourceFiles, new FeatureFilter(".feature"));

        for (File cucumberSource : sourceFiles) {
            try {
                generateGalasaTest(cucumberSource);
            } catch (Exception e) {
                throw new MojoExecutionException(
                        "Error processing feature file " + cucumberSource.getName(), e);
            }
        }
    }

    private void generateGalasaTest(File source) throws IOException, IllegalArgumentException, IllegalAccessException {
        getLog().info("TranslateCucumber: Generating Test Class " + source.getName());

        usedVariables = new ArrayList<String>();
        uniqueDependencies = new HashSet<String>();
        imports = new HashSet<String>();
        
        for(Class<?> translator : translatorClasses) {
            imports.add(translator.getName());
        }

        List<String> sourceLines = new ArrayList<String>();
        BufferedReader br = new BufferedReader(new FileReader(source));
        String st;
        while((st = br.readLine()) != null) {
            sourceLines.add(st);
        }
        br.close();

        File[] oldVersions = javaCode.listFiles(new JavaFilter(source.getName().substring(0, source.getName().lastIndexOf('.'))));
        if(oldVersions != null) {
            for(File old : oldVersions) {
                old.delete();
            }
        }

        String className = source.getName().substring(0, source.getName().indexOf('.'));
        File generated = new File(javaCode.getPath() + "/" + className + ".java");
        generated.createNewFile();

        FileWriter writer = new FileWriter(generated);
        StringBuilder builder = new StringBuilder();
        Boolean method = false;
        String parsingLine;
        ArrayList<String> givenLines = new ArrayList<String>();
        for(String line : sourceLines) {
            if(line.trim().indexOf(" ") >= 0) {
                switch (line.trim().substring(0, line.trim().indexOf(" "))) {
                    case "Feature:":
                        builder.append("package dev.galasa.ghrekin;\n\n");
                        builder.append("@ImportsHere@\n");
                        imports.add("dev.galasa.Test");
                        builder.append("@Test\n");
                        builder.append("public class " + className + " {");
                        builder.append("\n");
                        builder.append("@AnnotationsHere@");
                        break;
                    case "Scenario:":
                        if(method)
                            builder.append("}\n");
                        method = true;
                        builder.append("@Test\n");
                        
                        builder.append("public void " + camelCase(line.substring(line.indexOf(":") + 2, line.length())) + "() {\n");
                        break;
                    case "When":
                        parsingLine = line.trim().substring(line.trim().indexOf(" ") + 1, line.trim().length());
                        for(Method parsingMethod : whenMethods) {
                            writeMethod(parsingMethod, parsingLine, builder, LineType.WHEN);
                        }
                        break;
                    case "Then":
                        parsingLine = line.trim().substring(line.trim().indexOf(" ") + 1, line.trim().length());
                        for(Method parsingMethod : thenMethods) {
                            writeMethod(parsingMethod, parsingLine, builder, LineType.THEN);
                        }
                        break;
                    case "Given":
                        givenLines.add(line.trim().substring(line.trim().indexOf(" ") + 1, line.trim().length()));
                        break;
                    default:
                        break;
                }
            }
        }
        StringBuilder givenBuilder = new StringBuilder();
        for(String givenLine: givenLines) {
            for(Field parsingField : givenFields) {
                for(Annotation parsingAnnotation : parsingField.getAnnotations()) {
                    String regex = parsingAnnotation.toString().substring(parsingAnnotation.toString().indexOf("regex=") + 6, parsingAnnotation.toString().indexOf("type") - 2);
                    String type = parsingAnnotation.toString().substring(parsingAnnotation.toString().indexOf("type=") + 5, parsingAnnotation.toString().indexOf("dependencies") - 2);
                    String dependencies = parsingAnnotation.toString().substring(parsingAnnotation.toString().indexOf("dependencies=") + 13, parsingAnnotation.toString().indexOf("codeImports") - 2);
                    String[] codeImports = parsingAnnotation.toString().substring(parsingAnnotation.toString().indexOf("codeImports=") + 12, parsingAnnotation.toString().length() - 1).split(",");
                    if(regex != "" && givenLine.matches(regex)) {
                        StringBuilder fieldBuilder = new StringBuilder();
                        fieldBuilder.append((String)parsingField.get(parsingField));
                        String subVariable = null;
                        for(String usedVariable : usedVariables) {
                            if(usedVariable.matches(parsingField.getName() + "(([A-z])+)?([0-9])+")){
                                subVariable = usedVariable;
                                break;
                            }
                        }
                        int indexFieldValue = fieldBuilder.indexOf("@value_here@", 0);
                        if(indexFieldValue >= 0)
                            fieldBuilder.replace(indexFieldValue, indexFieldValue + "@value_here@".length(), getVariableFromLine(givenLine, regex, type));
                        indexFieldValue = fieldBuilder.indexOf("@name_here@", 0);
                        if(indexFieldValue >= 0)
                            fieldBuilder.replace(indexFieldValue, indexFieldValue + "@name_here@".length(), subVariable);
                        usedVariables.remove(subVariable);
                        for(String dependency : dependencies.split(","))
                            uniqueDependencies.add(dependency);
                        for(String codeImport : codeImports)
                            imports.add(codeImport);
                            
                        givenBuilder.append(fieldBuilder.toString() + "\n\n");
                    }
                }
            }
        }

        for(String dependency : uniqueDependencies) {
            for(Field parsingField : givenFields) {
                if(dependency.equals(parsingField.getName())) {
                    StringBuilder fieldBuilder = new StringBuilder();
                    fieldBuilder.append((String)parsingField.get(parsingField));
                    String subVariable = null;
                    for(String usedVariable : usedVariables) {
                        
                        if(usedVariable.matches(parsingField.getName() + "(([A-z])+)?")){
                            subVariable = usedVariable;
                            break;
                        }
                    }
                    int indexFieldValue = fieldBuilder.indexOf("@name_here@", 0);
                    if(indexFieldValue >= 0)
                        fieldBuilder.replace(indexFieldValue, indexFieldValue + "@name_here@".length(), subVariable);
                    usedVariables.remove(subVariable);
                    for(Annotation parsingAnnotation : parsingField.getAnnotations()) {
                        String[] codeImports = parsingAnnotation.toString().substring(parsingAnnotation.toString().indexOf("codeImports=") + 12, parsingAnnotation.toString().length() - 1).split(",");
                        for(String codeImport : codeImports)
                            imports.add(codeImport);
                    }
                    givenBuilder.append(fieldBuilder.toString() + "\n\n");
                }
            }
        }
        builder.append("}\n}");
        int indexAnnotations = builder.indexOf("@AnnotationsHere@", 0);
        builder.replace(indexAnnotations, indexAnnotations + "@AnnotationsHere@".length(), givenBuilder.toString());
        StringBuilder importBuilder = new StringBuilder();
        for(String imp : imports) {
            importBuilder.append("import " + imp + ";\n");
        }
        int indexImports = builder.indexOf("@ImportsHere@", 0);
        builder.replace(indexImports, indexImports + "@ImportsHere@".length(), importBuilder.toString());
        writer.write(builder.toString());
        writer.close();
    }

    private void writeMethod(Method parsingMethod, String parsingLine, StringBuilder builder, LineType lineType) throws IOException {
        for(Annotation parsingAnnotation : parsingMethod.getAnnotations()) {
            String regex = parsingAnnotation.toString().substring(parsingAnnotation.toString().indexOf("regex=") + 6, parsingAnnotation.toString().indexOf("type") - 2);
            String type = parsingAnnotation.toString().substring(parsingAnnotation.toString().indexOf("type=") + 5, parsingAnnotation.toString().length() - 1);
            if(parsingLine.matches(regex)) {
                if(!parsingMethod.getReturnType().getSimpleName().contains("void")) {
                    String returnName = getVariableName(parsingMethod.getReturnType().getSimpleName());
                    imports.add(parsingMethod.getReturnType().getName());
                    builder.append(parsingMethod.getReturnType().getSimpleName() + " " + returnName + " = ");
                }
                builder.append(parsingMethod.getDeclaringClass().getSimpleName() + "." + parsingMethod.getName() + "(");
                java.lang.reflect.Parameter[] parsingParams = parsingMethod.getParameters();
                for(int i = 0; i < parsingParams.length; i++) {
                    Boolean regexSet = false;
                    if(i == 0) {
                        if(getVariableFromLine(parsingLine, regex, type) != null) {
                            builder.append(getVariableFromLine(parsingLine, regex, type));
                            regexSet = true;
                        }
                    }
                    if(!regexSet) {
                        if(parsingParams[i].getType().equals(Class.class)) {
                            builder.append("this.getClass()");
                        } else if(parsingParams[i].getAnnotations().length > 0) {
                            String variableName = parsingParams[i].getType().getSimpleName().toLowerCase();
                            if(!usedVariables.contains(variableName))
                                usedVariables.add(variableName);
                            builder.append(variableName);
                        } else {
                            if(lineType == LineType.WHEN) {
                                String variableName = getVariableName(parsingParams[i].getType().getSimpleName());
                                builder.append(variableName);
                            } else if(lineType == LineType.THEN) {
                                String variableName = null;
                                for(String usedName : usedVariables) {
                                    if(usedName.matches(parsingParams[i].getType().getSimpleName().toLowerCase() + "([0-9])+"))
                                        variableName = usedName;
                                }
                                builder.append(variableName);
                            }
                        }
                    }

                    if(i != parsingParams.length - 1)
                        builder.append(",");
                }
                builder.append(");\n");
                break;
            }
        }
    }

    private String getVariableFromLine(String line, String regex, String type) {
        String[] lineWords = line.split(" ");
        String[] regexWords = regex.split(" ");
        int i = 0;
        while(!regexWords[i].contains("(") || !regexWords[i].contains(")") || !regexWords[i].contains("+") || !regexWords[i].contains("[") || !regexWords[i].contains("]")) {
            i++;
            if(i == regexWords.length)
                return "";
        }
        String variableWord = lineWords[i];
        if(type.equals("number")) {
            return "\"" + variableWord + "\"";
        }
        return null;
    }

    private String getVariableName(String variableType) {
        int i = 1;
        while(usedVariables.contains(variableType.toLowerCase() + i)) {
            i++;
        }
        usedVariables.add(variableType.toLowerCase() + i);
        return variableType.toLowerCase() + i;
    }

    private void getTranslatorClass() {
        ArrayList<URL> classpathURLs = new ArrayList<URL>();
        try {
            for(String dependency : classpathElements) {
                File file = new File(dependency);
                classpathURLs.add(file.toURI().toURL());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        ClassLoader thisLoad = getClass().getClassLoader();
        ClassLoader load = new URLClassLoader(classpathURLs.toArray(new URL[classpathURLs.size()]), thisLoad);

        Class<?> classAnnotation = ReflectionUtils.forName("dev.galasa.simbank.manager.ghrekin.CucumberTranslator", load);
        Class<?> whenAnnotation = ReflectionUtils.forName("dev.galasa.simbank.manager.ghrekin.When", load);
        Class<?> thenAnnotation = ReflectionUtils.forName("dev.galasa.simbank.manager.ghrekin.Then", load);
        Class<?> givenAnnotation = ReflectionUtils.forName("dev.galasa.simbank.manager.ghrekin.Given", load);

        ConfigurationBuilder configuration = new ConfigurationBuilder();
        configuration.addClassLoaders(load);
        configuration.addUrls(classpathURLs);
        configuration.addScanners(new SubTypesScanner(), new TypeAnnotationsScanner(), new MethodAnnotationsScanner(), new FieldAnnotationsScanner());

        Reflections reflections = new Reflections(configuration);

        this.translatorClasses = reflections.getTypesAnnotatedWith((Class<? extends Annotation>) classAnnotation);
        this.whenMethods = reflections.getMethodsAnnotatedWith((Class<? extends Annotation>) whenAnnotation);
        this.thenMethods = reflections.getMethodsAnnotatedWith((Class<? extends Annotation>) thenAnnotation);
        this.givenFields = reflections.getFieldsAnnotatedWith((Class<? extends Annotation>) givenAnnotation);

    }

    private void checkForSource(File directory, List<File> outputFiles, FilenameFilter filter) {
        File[] features = directory.listFiles(filter);
        if(features != null)
            outputFiles.addAll(Arrays.asList(features));
        File[] children = directory.listFiles();
        if(children != null) {
            for(File child : children) {
                checkForSource(child, outputFiles, filter);
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
    
    private enum LineType {
        WHEN,
        THEN
    }
    
}