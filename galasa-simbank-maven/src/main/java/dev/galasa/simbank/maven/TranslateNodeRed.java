package dev.galasa.simbank.maven;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

@Mojo(name = "translatenodered", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class TranslateNodeRed extends AbstractMojo {

    /**
     * An attribute that allows referencing to the Maven build itself that allows us to search for the necessary resources
     * Usage: Retrieving the projectname, the project's working directory
     */
    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    /**
     * An attribute to specify where the translated Java files need to go
     */
    @Parameter(defaultValue = "galasa.gherkin.translated", name = "packageName")
    private String packageName;

    /**
     * A list containing all JSON source files that are found within the maven build process
     */
    private List<File> sourceFiles = new ArrayList<File>();

    /**
     * An attribute for the creation of the directory where the translated JSON -> Java will be found
     */
    private File javaCode;

    /**
     * Arraylist for all the used variables for the Galasa-test
     * Here it is used to add the Account the variable in, this can be scalable
     */
    private ArrayList<String> usedVariables;

    /**
     * All the needed imports for the Galasa-test are stored in here
     */
    private HashSet<String> imports;
    
    /**
     * The initiator method to kick off the whole process of
     * finding resources -> regex'ing the JSON -> Translating to Galasa -> Galasa-test
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("TranslateNodeRed: Generating Sources " + project.getName());

        javaCode = new File(project.getBasedir() + "/src/main/java/" + packageName.replace(".", "/"));
        if (!javaCode.exists())
            javaCode.mkdirs();

        File testResources = new File(project.getBasedir() + "/src/main/test/resources");
        checkForSource(testResources, sourceFiles, new ExtensionFilter(".json"));

        for (File nodeRedSource : sourceFiles) {
            try {
                generateGalasaTest(nodeRedSource);
            } catch (Exception e) {
                throw new MojoExecutionException("Error processing feature file " + nodeRedSource.getName(), e);
            }
        }
    }

    /**
     * The full execution of the translation of a NodeRed Json file to a galasa test
     * @param source The source of a single NodeRed JSON file
     * @throws MojoExecutionException
     */
    private void generateGalasaTest(File source) throws MojoExecutionException {
        getLog().info("TranslateNodeRed: Generating Test Class " + source.getName());
        
        imports = new HashSet<String>();
        usedVariables = new ArrayList<String>();

        Gson gson = new Gson();
        JsonArray jsonArray;
        // Extracting an actual JSON Java object from the given source file
        try {
            jsonArray = gson.fromJson(new FileReader(source), JsonArray.class);
        } catch (FileNotFoundException e) {
            throw new MojoExecutionException("Feature file - " + source + " not found", e);
        }

        // Deletion of the previously translated .java files from other runs with the same JSON source name
        File[] oldVersions = javaCode.listFiles(new JavaFilter(source.getName().substring(0, source.getName().lastIndexOf('.'))));
        if(oldVersions != null) {
            for(File old : oldVersions) {
                old.delete();
            }
        }

        // Provision the new .java file where the translation will be put for this specific JSON source name
        String className = source.getName().substring(0, source.getName().indexOf('.'));
        File generated = new File(javaCode.getPath() + "/" + className + ".java");
        try {
            generated.createNewFile();
        } catch (IOException e) {
            throw new MojoExecutionException("Error generating file at path " + javaCode.getPath() + "/" + className + ".java", e);
        }

        FileWriter writer = null;
        try {
            writer = new FileWriter(generated);
        
            // A flow element refers to a single custom node, while a flowID refers to a complete flow page 
            // NodeRed can extract multiple flows at once
            ArrayList<JsonElement> flowElements = new ArrayList<JsonElement>();
            ArrayList<String> flowIds = new ArrayList<String>();

            for(JsonElement jsonElement : jsonArray) {
                if(jsonElement.getAsJsonObject().get("label") != null && jsonElement.getAsJsonObject().get("label").getAsString().contains("Flow")) {
                    flowElements.add(jsonElement);
                    flowIds.add(jsonElement.getAsJsonObject().get("id").getAsString());
                }
            }
            
            JsonObject organisedFlows = new JsonObject();

            // From the full JSON source remove all the available flow elements
            for(JsonElement flowElement : flowElements) {
                jsonArray.remove(flowElement);
            }

            // Extract the necessary flow object that need to be processed later on
            for(JsonElement flowElement : flowElements) {
                int attempts = 0;
                HashMap<String, JsonElement> extractedObjects = new HashMap<String, JsonElement>();
                for(JsonElement parsedElement : jsonArray) {
                    if(parsedElement.getAsJsonObject().get("z").getAsString().equals(flowElement.getAsJsonObject().get("id").getAsString())) {
                        if(parsedElement.getAsJsonObject().get("wires").getAsJsonArray().get(0).getAsJsonArray().size() == 0)
                            extractedObjects.put("start", parsedElement);
                        else
                            extractedObjects.put(parsedElement.getAsJsonObject().get("wires").getAsJsonArray().get(0).getAsJsonArray().get(0).getAsString(), parsedElement);
                    }
                }

                // Find the lead of every flow, so that the right order applies
                JsonElement leadingElement = extractedObjects.get("start");
                if(leadingElement == null)
                    throw new MojoExecutionException("Unable to find leading element for flow " + flowElement.getAsJsonObject().get("id").getAsString());

                // Reversal is needed for the way the JSON is structured and the way NodeRed encoded the wiring system between the nodes
                ArrayList<JsonElement> reversedElements = new ArrayList<JsonElement>();
                reversedElements.add(leadingElement);
                while(attempts <= extractedObjects.size()) {
                    leadingElement = extractedObjects.get(leadingElement.getAsJsonObject().get("id").getAsString());
                    reversedElements.add(leadingElement);
                    if(reversedElements.size() == extractedObjects.size())
                        break;
                    attempts ++;
                    if(attempts > extractedObjects.size())
                        throw new MojoExecutionException("Unable to interpret JSON");
                }
                Collections.reverse(reversedElements);

                JsonArray flowObjects = new JsonArray();
                for(JsonElement j : reversedElements)
                    flowObjects.add(j);
                organisedFlows.add(flowElement.getAsJsonObject().get("id").getAsString(), flowObjects);
            }

            // Start of the builder for the eventual Galasa-test
            StringBuilder builder = new StringBuilder();
            builder.append("package " + packageName + ";\n\n@ImportsHere@\n@Test\npublic class " + className + " {\n@AnnotationsHere@");
            imports.add("dev.galasa.Test");

            int methodCount = 0;

            StringBuilder givenBuilder = new StringBuilder();

            for(String flowId : flowIds) {
                builder.append("\t@Test\n\tpublic void method" + methodCount + "()@ExceptionsHere@ {\n");
                JsonArray jArray = organisedFlows.get(flowId).getAsJsonArray();
                ArrayList<String> methodExceptions = new ArrayList<String>();
                // Loop over the Custom nodes that have been found within the flow and process these
                for(JsonElement arrayObject : jArray) {
                    String value = arrayObject.getAsJsonObject().get("topic").getAsString();
                    String line = arrayObject.getAsJsonObject().get("name").getAsString();
                    switch(value) {
                        case "GIVEN":
                            processLine(line, givenBuilder, methodExceptions); break;
                        case "WHEN":
                        case "THEN":
                            processLine(line, builder, methodExceptions); break;
                        default:
                            builder.append("//" + line + "\n");
                    }
                }
                methodCount++;
                // Appending of the thrown exceptions that are used in the Galasa-test
                if(methodExceptions.size() > 0) {
                    StringBuilder exceptionBuilder = new StringBuilder();
                    exceptionBuilder.append(" throws ");
                    for(int i = 0; i < methodExceptions.size(); i++) {
                        exceptionBuilder.append(methodExceptions.get(i));
                        if(i != methodExceptions.size() -1)
                            exceptionBuilder.append(", ");
                    }
                    stringBuilderReplace(builder, "@ExceptionsHere@", exceptionBuilder.toString());
                } else {
                    stringBuilderReplace(builder, "@ExceptionsHere@", "");
                }

                builder.append("\t}\n\n");
            }

            builder.append("}");

            // Input of the necessary imports to the Galasa-test
            StringBuilder importBuilder = new StringBuilder();
            for(String imp : imports) {
                importBuilder.append("import " + imp + ";\n");
            }
            stringBuilderReplace(builder, "@ImportsHere@", importBuilder.toString());
            stringBuilderReplace(builder, "@AnnotationsHere@", givenBuilder.toString());

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

    /**
     * Regex'ing of a specific CustomNode (line here) against the actual Java necessary translation for this Node
     * @param line
     * @param patternBuilder
     * @param exceptionsList
     */
    private void processLine(String line, StringBuilder patternBuilder, ArrayList<String> exceptionsList) {

        Pattern pattern = Pattern.compile("Obtain account with (-?[0-9]+)");
        Matcher matcher = pattern.matcher(line);
        if(matcher.matches() && matcher.groupCount() > 0) {
            patternBuilder.append("\t//" + line + "\n");
            String variableWord = "\"" + matcher.group(1) + "\"";
            StringBuilder fieldBuilder = new StringBuilder();
            fieldBuilder.append("\t@Account(balance = @value_here@)\n\tpublic IAccount @name_here@;");
            stringBuilderReplace(fieldBuilder, "@value_here@", variableWord);
            stringBuilderReplace(fieldBuilder, "@name_here@", getVariableName("account"));
            patternBuilder.append(fieldBuilder.toString() + "\n\n");
            imports.add("dev.galasa.simbank.manager.Account");
            imports.add("dev.galasa.simbank.manager.IAccount");

            String[] uniques = {"simbank","artifactmanager","httpclient"};
            for(String dependency : uniques) {
                processLine(dependency, patternBuilder, exceptionsList);
            }
            return;
        }

        pattern = Pattern.compile("Credit with (-?[0-9]+)");
        matcher = pattern.matcher(line);
        if(matcher.matches() && matcher.groupCount() > 0) {
            patternBuilder.append("\t\t//" + line + "\n");
            String variableWord = "\"" + matcher.group(1) + "\"";
            StringBuilder fieldBuilder = new StringBuilder();
            String methodCode = "\t\tclient.build();\n" +
            "HashMap<String, Object> parameters = new HashMap<String, Object>();\n" +
            "parameters.put(\"ACCOUNT_NUMBER\", @account_name@.getAccountNumber());\n" +
            "parameters.put(\"AMOUNT\", @value_here@);\n" +
            "IBundleResources resources = artifacts.getBundleResources(this.getClass());\n" +
            "InputStream is = resources.retrieveSkeletonFile(\"/resources/skeletons/testSkel.skel\", parameters);\n" +
            "String textContent = resources.streamAsString(is);\n" +
            "client.setURI(new URI(bank.getFullAddress()));\n" +
            "client.postTextAsXML(bank.getUpdateAddress(), textContent, false);";
            fieldBuilder.append(methodCode.replaceAll("\n", "\n\t\t"));
            stringBuilderReplace(fieldBuilder, "@value_here@", variableWord);
            String accName = null;
            for(String vName : usedVariables) {
                if(vName.contains("account"))
                    accName = vName;
            }
            stringBuilderReplace(fieldBuilder, "@account_name@", accName);
            patternBuilder.append(fieldBuilder.toString() + "\n\n");
            imports.add("java.util.HashMap");
            imports.add("java.io.InputStream");
            imports.add("dev.galasa.artifact.IBundleResources");
            imports.add("java.net.URI");
            exceptionsList.add("TestBundleResourceException");
            exceptionsList.add("IOException");
            exceptionsList.add("URISyntaxException");
            exceptionsList.add("HttpClientException");
            imports.add("dev.galasa.artifact.TestBundleResourceException");
            imports.add("java.io.IOException");
            imports.add("java.net.URISyntaxException");
            imports.add("dev.galasa.http.HttpClientException");
            return;
        }

        pattern = Pattern.compile("simbank");
        matcher = pattern.matcher(line);
        if(matcher.matches() && patternBuilder.indexOf("ISimBank") == -1) {
            patternBuilder.append("\t@SimBank\n\tpublic ISimBank bank;\n\n");
            imports.add("dev.galasa.simbank.manager.SimBank");
            imports.add("dev.galasa.simbank.manager.ISimBank");
            return;
        }

        pattern = Pattern.compile("artifactmanager");
        matcher = pattern.matcher(line);
        if(matcher.matches() && patternBuilder.indexOf("IArtifactManager") == -1) {
            patternBuilder.append("\t@ArtifactManager\n\tpublic IArtifactManager artifacts;\n\n");
            imports.add("dev.galasa.artifact.ArtifactManager");
            imports.add("dev.galasa.artifact.IArtifactManager");
            return;
        }

        pattern = Pattern.compile("httpclient");
        matcher = pattern.matcher(line);
        if(matcher.matches() && patternBuilder.indexOf("IHttpClient") == -1) {
            patternBuilder.append("\t@HttpClient\n\tpublic IHttpClient client;\n\n");
            imports.add("dev.galasa.http.HttpClient");
            imports.add("dev.galasa.http.IHttpClient");
            return;
        }

        pattern = Pattern.compile("Check account is (-?[0-9]+)");
        matcher = pattern.matcher(line);
        if(matcher.matches() && matcher.groupCount() > 0) {
            patternBuilder.append("\t\t//" + line + "\n");
            String variableWord = "\"" + matcher.group(1) + "\"";
            StringBuilder fieldBuilder = new StringBuilder();
            fieldBuilder.append("\t\tassertThat(@account_name@.getBalance()).isEqualByComparingTo(new BigDecimal(@value_here@));");
            stringBuilderReplace(fieldBuilder, "@value_here@", variableWord);
            String accName = null;
            for(String vName : usedVariables) {
                if(vName.contains("account"))
                    accName = vName;
            }
            stringBuilderReplace(fieldBuilder, "@account_name@", accName);
            patternBuilder.append(fieldBuilder.toString() + "\n");
            imports.add("java.math.BigDecimal");
            imports.add("static org.assertj.core.api.Assertions.assertThat");
            exceptionsList.add("SimBankManagerException");
            imports.add("dev.galasa.simbank.manager.SimBankManagerException");
            return;
        }
    }

    /**
     * Extracting a specific prefix of a variable and finding out if it is needed 
     * Also adding a suffix number to be able to provide multiple instances of an object
     * @param prefix
     * @return
     */
    private String getVariableName(String prefix) {
        int i = 1;
        while(usedVariables.contains(prefix.toLowerCase() + i)) {
            i++;
        }
        usedVariables.add(prefix.toLowerCase() + i);
        return prefix.toLowerCase() + i;
    }

    /**
     * Generalisation of a stringbuilder.replace() function
     * @param builder
     * @param replacePattern
     * @param replaceWith
     */
    private void stringBuilderReplace(StringBuilder builder, String replacePattern, String replaceWith) {
        int indexReplacement = builder.indexOf(replacePattern, 0);
        if(indexReplacement >= 0)
            builder.replace(indexReplacement, indexReplacement + replacePattern.length(), replaceWith);
    }

    /**
     * A method that goes through the working directory in a recursive fashion to be able 
     * to extract all the applicable source files ( NodeRed JSON files)
     * @param directory
     * @param outputFiles
     * @param filter
     */
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
}