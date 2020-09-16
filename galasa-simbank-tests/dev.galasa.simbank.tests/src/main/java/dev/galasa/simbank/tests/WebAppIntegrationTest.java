package dev.galasa.simbank.tests;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.assertj.core.api.Fail;

import static org.assertj.core.api.Assertions.assertThat;

import dev.galasa.Test;
import dev.galasa.artifact.BundleResources;
import dev.galasa.artifact.IBundleResources;
import dev.galasa.artifact.TestBundleResourceException;
import dev.galasa.core.manager.CoreManager;
import dev.galasa.core.manager.ICoreManager;
import dev.galasa.core.manager.Logger;
import dev.galasa.zos.IZosImage;
import dev.galasa.zos.ZosImage;
import dev.galasa.zos3270.FieldNotFoundException;
import dev.galasa.zos3270.ITerminal;
import dev.galasa.zos3270.KeyboardLockedException;
import dev.galasa.zos3270.TerminalInterruptedException;
import dev.galasa.zos3270.TextNotFoundException;
import dev.galasa.zos3270.TimeoutException;
import dev.galasa.zos3270.Zos3270Terminal;
import dev.galasa.zos3270.spi.NetworkException;
import dev.galasa.zosbatch.IZosBatch;
import dev.galasa.zosbatch.IZosBatchJob;
import dev.galasa.zosbatch.IZosBatchJobname;
import dev.galasa.zosbatch.ZosBatch;
import dev.galasa.zosbatch.ZosBatchException;
import dev.galasa.zosbatch.ZosBatchJobname;
import dev.galasa.selenium.ISeleniumManager;
import dev.galasa.selenium.IWebPage;
import dev.galasa.selenium.SeleniumManager;
import dev.galasa.selenium.SeleniumManagerException;
import dev.galasa.docker.DockerContainer;
import dev.galasa.docker.DockerManagerException;
import dev.galasa.docker.IDockerContainer;

@Test
public class WebAppIntegrationTest {

	@DockerContainer(image = "maven-web-app", dockerContainerTag = "a", start = true)
    public IDockerContainer container;
	
	@ZosImage(imageTag = "SIMBANK")
	public IZosImage        image;

	@Zos3270Terminal(imageTag = "SIMBANK")
	public ITerminal        terminal;
	
	@ZosBatch(imageTag="SIMBANK")
    public IZosBatch zosBatch;
    
    @ZosBatchJobname(imageTag="SIMBANK")
	public IZosBatchJobname zosBatchJobname;
	
	@CoreManager
    public ICoreManager     coreManager;
	
	@BundleResources
	public IBundleResources resources; 

	@SeleniumManager
    public ISeleniumManager seleniumManager;

	@Logger
	public Log logger;
	
	public static int account;
	
	@Test
	public void webAppIntegrationTest() throws SeleniumManagerException, DockerManagerException, TimeoutException, KeyboardLockedException, TerminalInterruptedException, TextNotFoundException, FieldNotFoundException, NetworkException, TestBundleResourceException, IOException, ZosBatchException{

		//Dianas code
		terminal.waitForKeyboard()
		.positionCursorToFieldContaining("Userid").tab().type("IBMUSER")
		.positionCursorToFieldContaining("Password").tab().type("SYS1")
		.enter().waitForKeyboard()

        // Open banking application
		.pf1().waitForKeyboard()
		.clear().waitForKeyboard().type("bank")
		.enter().waitForKeyboard();

		String accountNumber = String.valueOf((int)Math.random() * (999999999 - 100000000 + 1) + 100000000);

		terminal.pf1().waitForKeyboard()
		.positionCursorToFieldContaining("Account Number").tab().type(accountNumber)
		.enter().waitForKeyboard();
		
		
		while(!terminal.retrieveScreen().contains("Account Not Found")){
			
		accountNumber = String.valueOf((int)Math.random() * (999999999 - 100000000 + 1) + 100000000);

		terminal.pf3().pf1().waitForKeyboard()
		.positionCursorToFieldContaining("Account Number").tab().type(accountNumber)
		.enter().waitForKeyboard();
		}
		
		HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("CONTROL", "ACCOUNT_OPEN");
		parameters.put("DATAIN", accountNumber+",00-00-00,1");	
		String jcl = resources.retrieveSkeletonFileAsString("/resources/skeletons/SIMBANK.jcl", parameters);
		IZosBatchJob batchJob = zosBatch.submitJob(jcl, zosBatchJobname);
		logger.info("batchJob.toString() = " +  batchJob.toString());
		int rc = batchJob.waitForJob();
		if (rc != 0) {
			// Print the job output to the run log
			batchJob.retrieveOutput().forEach(jobOutput ->
				logger.info("batchJob.retrieveOutput(): " + jobOutput.getDdname() + "\n" + jobOutput.getRecords() + "\n")
			);
			Fail.fail("Batch job failed RETCODE=" + batchJob.getRetcode() + " Check batch job output");
			
		}
		logger.info("Batch job complete RETCODE=" + batchJob.getRetcode());
		
		//Dianas code
		
		//Charlies code
		
		Map<String, List<InetSocketAddress>> ports = container.getExposedPorts();
		
		int port = ports.get("8080/tcp").get(0).getPort();

		String webpage = "http://localhost:" + port + "galasa-simplatform-webapp/simbank";

		IWebPage page = seleniumManager.allocateWebPage(webpage);
		page.maximize();
		assertThat(page.getTitle()).containsOnlyOnce("Simbank");
		page.sendKeysToElementById("accnr", accountNumber);
		page.sendKeysToElementById("amount", "120");
		page.clickElementById("submit");
		assertThat(page.findElementById("output").getText()).contains("Transaction complete");
		page.close();
		
		//Charlies code
		
	}

}
