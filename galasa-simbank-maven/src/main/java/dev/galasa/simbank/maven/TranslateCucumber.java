/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2020.
 */
package dev.galasa.simbank.maven;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

@Mojo(name = "translatecucumber", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
@SuppressWarnings("unchecked")
public class TranslateCucumber extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${project.compileClasspathElements}", readonly = true, required = true)
    private List<String> classpathElements;

    @Parameter(defaultValue = "galasa.gherkin.translated", name = "packageName")
    private String packageName;

    private List<File> sourceFiles = new ArrayList<File>();
    private File javaCode;

    private Set<Class<?>> translatorClasses;
    private Set<Method> whenMethods;
    private Set<Method> thenMethods;
    private Set<Field> givenFields;

    private ArrayList<String> usedVariables;
    private HashSet<String> uniqueDependencies;
    private HashSet<String> imports;
    private HashSet<String> generatedReturns;
    private HashSet<String> thrownMethodExceptions;
    private HashSet<String> processableVariables;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("TranslateCucumber: Generating Sources " + project.getName());

        javaCode = new File(project.getBasedir() + "/src/main/java/" + packageName.replace(".", "/"));
        if (!javaCode.exists())
            javaCode.mkdirs();

        getTranslatorClass();

        File testResources = new File(project.getBasedir() + "/src/test/resources");
        checkForSource(testResources, sourceFiles, new ExtensionFilter(".feature"));

        for (File cucumberSource : sourceFiles) {
            try {
                generateGalasaTest(cucumberSource);
            } catch (Exception e) {
                throw new MojoExecutionException("Error processing feature file " + cucumberSource.getName(), e);
            }
        }
    }

    private void generateGalasaTest(File source) throws MojoExecutionException {
        getLog().info("TranslateCucumber: Generating Test Class " + source.getName());

        usedVariables = new ArrayList<String>();
        uniqueDependencies = new HashSet<String>();
        imports = new HashSet<String>();
        generatedReturns = new HashSet<String>();
        thrownMethodExceptions = new HashSet<String>();

        for (Class<?> translator : translatorClasses) {
            imports.add(translator.getName());
        }

        List<String> sourceLines = new ArrayList<String>();
        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(source));
            String st;
            while((st = br.readLine()) != null) {
                sourceLines.add(st);
            }
            br.close();
        } catch (FileNotFoundException e) {
            throw new MojoExecutionException("Feature file - " + source + " not found", e);
        } catch (IOException e) {
            throw new MojoExecutionException("Error reading file - " + source, e);
        }

        File[] oldVersions = javaCode.listFiles(new JavaFilter(source.getName().substring(0, source.getName().lastIndexOf('.'))));
        if(oldVersions != null) {
            for(File old : oldVersions) {
                old.delete();
            }
        }

        String className = source.getName().substring(0, source.getName().indexOf('.'));
        File generated = new File(javaCode.getPath() + "/" + className + ".java");
        try {
            generated.createNewFile();
        } catch (IOException e) {
            throw new MojoExecutionException("Error generating file at path " + javaCode.getPath() + "/" + className + ".java", e);
        }

        FileWriter writer;
        try {
            writer = new FileWriter(generated);
        } catch (IOException e) {
            throw new MojoExecutionException("Error generating file writer for - " + javaCode.getPath() + "/" + className + ".java", e);
        }
        StringBuilder builder = new StringBuilder();
        Boolean methodStarted = false;
        ArrayList<String> givenLines = new ArrayList<String>();
        for(String line : sourceLines) {
            if(line.trim().indexOf(" ") >= 0) {
                switch (line.trim().substring(0, line.trim().indexOf(" "))) {
                    case "Feature:":
                        builder.append("package " + packageName + ";\n\n@ImportsHere@\n@Test\npublic class " + className + " {\n@AnnotationsHere@");
                        imports.add("dev.galasa.Test");
                        break;
                    case "Scenario:":
                        if(methodStarted) {
                            builder.append("\t}\n\n");
                            processExceptions(builder, thrownMethodExceptions);
                        }
                        processableVariables = new HashSet<String>();
                        thrownMethodExceptions = new HashSet<String>();
                        methodStarted = true;
                        builder.append("\t@Test\n");
                        
                        builder.append("\tpublic void " + camelCase(line.trim().substring(line.trim().indexOf(" ") + 1)) + "() @ExceptionsHere@{\n");
                        break;
                    case "When":
                        String whenParsingLine = line.trim().substring(line.trim().indexOf(" ") + 1);
                        for(Method parsingMethod : whenMethods) {
                            writeMethod(parsingMethod, whenParsingLine, builder, LineType.WHEN);
                        }
                        break;
                    case "Then":
                        String thenParsingLine = line.trim().substring(line.trim().indexOf(" ") + 1);
                        for(Method parsingMethod : thenMethods) {
                            writeMethod(parsingMethod, thenParsingLine, builder, LineType.THEN);
                        }
                        break;
                    case "Given":
                        givenLines.add(line.trim().substring(line.trim().indexOf(" ") + 1));
                        break;
                    default:
                        builder.append("\t//" + line.trim() + "\n\n");
                        break;
                }
            }
        }
        processExceptions(builder, thrownMethodExceptions);
        processGivenLines(givenLines, builder);

        builder.append("\t}\n}");

        for(String gen : generatedReturns)
            stringBuilderReplace(builder, gen, "");
        
        StringBuilder importBuilder = new StringBuilder();
        for(String imp : imports) {
            importBuilder.append("import " + imp + ";\n");
        }
        stringBuilderReplace(builder, "@ImportsHere@", importBuilder.toString());

        try {
            writer.write(builder.toString());
        } catch (IOException e) {
            throw new MojoExecutionException("Error writing String Builder to file", e);
        } finally {
            try {
                writer.close();
            } catch (IOException e) {
                throw new MojoExecutionException("Error closing File Writer", e);
            }
        }
    }

    private void processExceptions(StringBuilder builder, HashSet<String> exceptions) {
        StringBuilder exceptionString = new StringBuilder();
        for(int i = 0; i < exceptions.size(); i++) {
            if(exceptionString.length() == 0)
                exceptionString.append("throws ");
            exceptionString.append(exceptions.toArray()[i]);
            if(i != exceptions.size() - 1)
                exceptionString.append(",");
            exceptionString.append(" ");
        }
        stringBuilderReplace(builder, "@ExceptionsHere@", exceptionString.toString());
    }

    private String getAnnotationValue(Annotation annotation, String field) {
        String parsedField = annotation.toString().substring(annotation.toString().indexOf(field) + field.length() + 1);
        if(parsedField.indexOf(",") == -1)
            return parsedField.substring(0, parsedField.indexOf(")"));
        else
            return parsedField.substring(0, parsedField.indexOf(","));
    }

    private void stringBuilderReplace(StringBuilder builder, String replacePattern, String replaceWith) {
        int indexReplacement = builder.indexOf(replacePattern, 0);
        if(indexReplacement >= 0)
            builder.replace(indexReplacement, indexReplacement + replacePattern.length(), replaceWith);
    }

    private void processGivenLines(ArrayList<String> givenLines, StringBuilder builder) throws MojoExecutionException {
        StringBuilder givenBuilder = new StringBuilder();
        for(String givenLine: givenLines) {
            for(Field parsingField : givenFields) {
                for(Annotation parsingAnnotation : parsingField.getAnnotations()) {
                    String regex = getAnnotationValue(parsingAnnotation, "regex");
                    String type = getAnnotationValue(parsingAnnotation, "type");
                    String[] dependencies = getAnnotationValue(parsingAnnotation, "dependencies").split(";");
                    String[] codeImports = getAnnotationValue(parsingAnnotation, "codeImports").split(";");
                    if(regex != "" && givenLine.matches(regex)) {
                        StringBuilder fieldBuilder = new StringBuilder();
                        try {
                            fieldBuilder.append((String)parsingField.get(parsingField));
                        } catch(IllegalAccessException e) {
                            throw new MojoExecutionException("Error getting access to field - " + parsingField.toString(), e);
                        }
                        String subVariable = null;
                        for(String usedVariable : usedVariables) {
                            if(usedVariable.matches(parsingField.getName().replaceAll("[0-9]", "") + "([0-9])+")) {
                                subVariable = usedVariable;
                                break;
                            }
                        }
                        stringBuilderReplace(fieldBuilder, "@value_here@", getVariableFromLine(givenLine, regex, type));
                        stringBuilderReplace(fieldBuilder, "@name_here@", subVariable);
                        usedVariables.remove(subVariable);
                        for(String dependency : dependencies)
                            uniqueDependencies.add(dependency);
                        for(String codeImport : codeImports)
                            imports.add(codeImport);
                            
                        givenBuilder.append("\t//" + givenLine + "\n");
                        givenBuilder.append("\t" + fieldBuilder.toString().replace("\n", "\n\t") + "\n\n");
                    }
                }
            }
        }

        for(String dependency : uniqueDependencies) {
            for(Field parsingField : givenFields) {
                if(dependency.equals(parsingField.getName())) {
                    StringBuilder fieldBuilder = new StringBuilder();
                    try {
                        fieldBuilder.append((String)parsingField.get(parsingField));
                    } catch(IllegalAccessException e) {
                        throw new MojoExecutionException("Error getting access to field - " + parsingField.toString(), e);
                    }
                    String subVariable = parsingField.getName();
                    stringBuilderReplace(fieldBuilder, "@name_here@", subVariable);
                    for(Annotation parsingAnnotation : parsingField.getAnnotations()) {
                        String[] codeImports = getAnnotationValue(parsingAnnotation, "codeImports").split(";");
                        for(String codeImport : codeImports)
                            imports.add(codeImport);
                    }
                    givenBuilder.append("\t" + fieldBuilder.toString().replace("\n", "\n\t") + "\n\n");
                }
            }
        }
        stringBuilderReplace(builder, "@AnnotationsHere@", givenBuilder.toString());
    }

    private void writeMethod(Method parsingMethod, String parsingLine, StringBuilder builder, LineType lineType) {
        for(Annotation parsingAnnotation : parsingMethod.getAnnotations()) {
            String regex = getAnnotationValue(parsingAnnotation, "regex");
            String type = getAnnotationValue(parsingAnnotation, "type");
            if(parsingLine.matches(regex)) {
                builder.append("\t\t" + "//" + parsingLine + "\n\t\t");
                if(!parsingMethod.getReturnType().getSimpleName().contains("void")) {
                    imports.add(parsingMethod.getReturnType().getName());
                    String returnLine = parsingMethod.getReturnType().getSimpleName() + " " + getVariableName(parsingMethod.getReturnType().getSimpleName()) + " = ";
                    builder.append(returnLine);
                    generatedReturns.add(returnLine);
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
                            String variableName = null;
                            if(lineType == LineType.WHEN) {
                                for(String processableVariable : processableVariables) {
                                    if(processableVariable.matches(parsingParams[i].getType().getSimpleName().toLowerCase() + "([0-9])+"))
                                        variableName = processableVariable;
                                }
                                if(variableName == null) {
                                    variableName = getVariableName(parsingParams[i].getType().getSimpleName());
                                    processableVariables.add(variableName);
                                }
                            } else if(lineType == LineType.THEN) {
                                for(String usedName : usedVariables) {
                                    if(usedName.matches(parsingParams[i].getType().getSimpleName().toLowerCase() + "([0-9])+"))
                                        variableName = usedName;
                                }
                                String foundGen = null;
                                for(String gen : generatedReturns) {
                                    if(gen.contains(variableName))
                                        foundGen = gen;
                                }
                                generatedReturns.remove(foundGen);
                            }
                            builder.append(variableName);
                        }
                    }

                    if(i != parsingParams.length - 1)
                        builder.append(", ");
                }
                builder.append(");\n");
                for(Class<?> exceptionClass : parsingMethod.getExceptionTypes()) {
                    imports.add(exceptionClass.getName());
                    thrownMethodExceptions.add(exceptionClass.getSimpleName());
                }
                break;
            }
        }
    }

    private String getVariableFromLine(String line, String regex, String type) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(line);
        if(matcher.matches() && matcher.groupCount() > 0) {
            String variableWord = matcher.group(1);
            if(type.equals("number")) {
                return "\"" + variableWord + "\"";
            }
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

    private void getTranslatorClass() throws MojoExecutionException {
        ArrayList<URL> classpathURLs = new ArrayList<URL>();
        try {
            for(String dependency : classpathElements) {
                File file = new File(dependency);
                classpathURLs.add(file.toURI().toURL());
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Error in parsing dependency class URLs during getTranslatorClass", e);
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
}