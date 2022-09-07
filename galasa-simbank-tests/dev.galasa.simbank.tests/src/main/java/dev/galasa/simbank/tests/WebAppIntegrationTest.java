package dev.galasa.simbank.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.assertj.core.api.Fail;

import dev.galasa.Test;
import dev.galasa.artifact.BundleResources;
import dev.galasa.artifact.IBundleResources;
import dev.galasa.core.manager.Logger;
import dev.galasa.selenium.IFirefoxOptions;
import dev.galasa.selenium.IWebDriver;
import dev.galasa.selenium.IWebPage;
import dev.galasa.selenium.WebDriver;
import dev.galasa.selenium.SeleniumManagerException;
import dev.galasa.simbank.manager.ISimBank;
import dev.galasa.simbank.manager.ISimBankTerminal;
import dev.galasa.simbank.manager.ISimBankWebApp;
import dev.galasa.simbank.manager.SimBank;
import dev.galasa.simbank.manager.SimBankManagerException;
import dev.galasa.simbank.manager.SimBankTerminal;
import dev.galasa.simbank.manager.SimBankWebApp;
import dev.galasa.zos.IZosImage;
import dev.galasa.zos.ZosImage;
import dev.galasa.zos3270.spi.NetworkException;
import dev.galasa.zosbatch.IZosBatch;
import dev.galasa.zosbatch.IZosBatchJob;
import dev.galasa.zosbatch.ZosBatch;

@Test
public class WebAppIntegrationTest {

	// Application specific objects
	@SimBank
	public ISimBank bank;
	@SimBankTerminal
	public ISimBankTerminal bankTerminal;
	@SimBankWebApp
	public ISimBankWebApp webApp;

	// z/OS objects
	@ZosImage(imageTag = "SIMBANK")
	public IZosImage image;
	@ZosBatch(imageTag = "SIMBANK")
	public IZosBatch zosBatch;

	// Test technology objects
	@WebDriver
	public IWebDriver webDriver;
	@BundleResources
	public IBundleResources resources;

	// Logging and reporting
	@Logger
	public Log logger;

	private final BigDecimal openingBalance = BigDecimal.valueOf(100.00);

	@Test
	public void webAppIntegrationTest() throws Exception {		
		/**
		 * The provisionAccount() method performs several actions:
		 * 	- Generates a new random account number to use in the test
		 * 	- Uses 3270 to interact with the bank application to ensure the
		 * 	  account doesn't already exist
		 *  - Submits a batch job to open the new account and all relevant data
		 */
		String accountNumber = provisionAccount(openingBalance);
		logger.info("Account number selected: " + accountNumber);
		
		/*
		 * Here we generate a random amount that we want to credit to the new account.
		 */
		BigDecimal creditAmount = generateRandomBigDecimalFromRange(
				new BigDecimal(0.00).setScale(2, RoundingMode.HALF_UP),
    		new BigDecimal(500.00).setScale(2, RoundingMode.HALF_UP));
		logger.info("Amount to be credited to account: "+ creditAmount.toString());

		/*
		 * Here we operate a Selenium driver to use a web browser to interact with
		 * a form inside our provisioned web application. It then submits the form 
		 * and waits for a response.
		 */
		IWebPage page = completeWebFormAndSubmit(accountNumber, creditAmount.toString());
		logger.info("Web from submitted");

		/*
		 * Our first assertion validates the response from our web app is as expected
		 */
		assertThat(page.waitForElementById("good").getText()).contains("You have successfully completed the transaction");
		page.quit();

		/*
		 * Our second assertion validates that the data has been updated throughout the whole
		 * application including the backing database. It does this again by interacting
		 * with the 3270 portion of our CICS application.
		 */
		logger.info("Response from servlet OK. Now validating the data has been updated in the database");
		BigDecimal balance = retrieveAccountBalance(accountNumber).setScale(2);
		assertThat(balance).isEqualTo(openingBalance.add(creditAmount));
		logger.info("Test method complete");
	}

	/**
	 * Utility Methods
	 */

	/*
	 * TECHNOLOGIES USED:
	 * 	- Selenium
	 * 	- Docker interactions
	 */
	public IWebPage completeWebFormAndSubmit(String accountNumber, String creditAmount)
			throws SimBankManagerException, SeleniumManagerException {
		String webpage = webApp.getHostName() + "/galasa-simplatform-webapp/simbank";
		// Selenium Options to run the driver headlessly
		IFirefoxOptions options = webDriver.getFirefoxOptions();
//		options.setHeadless(true);

		// Open the Simbank Web Application in a Firefox browser
		IWebPage page = webDriver.allocateWebPage(webpage, options);
		page.takeScreenShot();
		assertThat(page.getTitle()).containsOnlyOnce("Simbank");
		
		// Fill in the Form and submit
		page.sendKeysToElementById("accnr", accountNumber);
		page.sendKeysToElementById("amount", creditAmount);
		page.takeScreenShot();
		page.clickElementById("submit");
		
		// Report the result from the browser
		page.takeScreenShot();
		while(!"visibility: visible;".equals(page.findElementById("good").getAttribute("style"))) {
			logger.info("Waiting response");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				logger.error("Thread interupred", e);
			}
		}
		page.takeScreenShot();
		return page;
	}

	/*
	 * TECHNOLOGIES USED:
	 * 	- 3270 interactions
	 */
	public String provisionAccount(BigDecimal openingBalance) throws Exception {
		// Generate a random account number
		String accountNumber =  generateRandomAccountNumber();
		boolean searching = true;
		
		// A looped search to ensure we find a unique account that hasn't been used.
		while (searching) {
			if (doesAccountExist(accountNumber)) {
				accountNumber =  generateRandomAccountNumber();
			} else {
				searching = false;
			}
		}
		
		// Open the account and give it an opening balance
		openAccount(accountNumber, openingBalance);
		
		return accountNumber;
	}
	
	/*
	 * TECHNOLGIES USED:
	 * 	- 3270 interactions
	 */
	public boolean doesAccountExist(String accountNumber) throws Exception {
		// Ensure the 3270 emulator is connected to the application
		if (!bankTerminal.isConnected()) {
			try {
				bankTerminal.connect();
			} catch (NetworkException e) {
				logger.error("Failed to connect to simbank", e);
				throw e;
			}
		}
		
		try {
			// Use the application to search for the account number
			bankTerminal.pf1()
				.waitForKeyboard()
				.positionCursorToFieldContaining("Account Number").tab()
				.type(accountNumber)
				.enter();
			
			String responseScreen = bankTerminal.waitForKeyboard().retrieveScreen();
			
			// Reset back to main menu
			bankTerminal.gotoMainMenu();
			
			// Return boolean response if the account exists 
			return responseScreen.contains("Account Found");
		} catch (Exception e) {
			logger.error("Failed to check account exists");
			throw e;
		}
	}
	
	/*
	 * TECHNOLOGIES USED:
	 * 	- Batch job
	 */
	public void openAccount(String accountNumber, BigDecimal openingBalance) throws Exception {
		// Use a batch Job to open the new account
		HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("CONTROL", "ACCOUNT_OPEN");
		parameters.put("DATAIN", accountNumber+",20-24-09,"+openingBalance);	
		try {
			// Populate a Skeleton JCL file
			String jcl = resources.retrieveSkeletonFileAsString("/resources/skeletons/SIMBANK.jcl", parameters);
			IZosBatchJob batchJob = zosBatch.submitJob(jcl, null);
		
			// Wait for the job to complete as we want to check the RC
			int rc = batchJob.waitForJob();
			if (rc != 0) {
				Fail.fail("Batch job failed RETCODE=" + batchJob.getRetcode() + " Check batch job output");
			}
			logger.info("Batch job complete RETCODE=" + batchJob.getRetcode());
		} catch (Exception e) {
			logger.error("Failed to open account: " + accountNumber);
			throw e;
		}
	}
	
	/*
	 * TECHNOLOGIES USED:
	 * 	- 3270 interactions
	 */
	public BigDecimal retrieveAccountBalance(String accountNumber) throws Exception {
		// Ensure the 3270 emulator is connected to the application
		if (!bankTerminal.isConnected()) {
			try {
				bankTerminal.connect();
			} catch (NetworkException e) {
				logger.error("Failed to connect to simbank", e);
				throw e;
			}
		}
		try {
			// Use the application to search for the account number
			bankTerminal.pf1()
				.waitForKeyboard()
				.positionCursorToFieldContaining("Account Number").tab()
				.type(accountNumber)
				.enter();
			
			// Retrieve the current balance of the account
			if (bankTerminal.waitForKeyboard().retrieveScreen().contains("Account Found")) {
				String balance = (bankTerminal.retrieveFieldTextAfterFieldWithString("Balance"));
				
				// Reset back to main menu
				bankTerminal.gotoMainMenu();
				return BigDecimal.valueOf(Double.valueOf(balance));
			} else {
				Fail.fail("Failed to find account");
				return null;
			}
			
		} catch (Exception e) {
			logger.error("Failed to check account balance");
			throw e;
		}
	}
	
	public String generateRandomAccountNumber() {
		Random random = new Random();
		StringBuilder builder = new StringBuilder();
		
		for (int i=0;i<9;i++) {
			builder.append(random.nextInt(10));
		}
		return builder.toString();
	}

	public BigDecimal generateRandomBigDecimalFromRange(BigDecimal min, BigDecimal max) {
		BigDecimal randomBigDecimal = min.add(new BigDecimal(Math.random()).multiply(max.subtract(min)));
		return randomBigDecimal.setScale(2,RoundingMode.HALF_UP);
	}
}
