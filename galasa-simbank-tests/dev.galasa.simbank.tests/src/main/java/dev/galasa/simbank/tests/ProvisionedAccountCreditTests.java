/*
 * Copyright contributors to the Galasa project
 */
package dev.galasa.simbank.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;

import org.apache.commons.logging.Log;

import dev.galasa.ResultArchiveStoreContentType;
import dev.galasa.SetContentType;
import dev.galasa.Test;
import dev.galasa.artifact.BundleResources;
import dev.galasa.artifact.IBundleResources;
import dev.galasa.core.manager.Logger;
import dev.galasa.core.manager.StoredArtifactRoot;
import dev.galasa.http.HttpClient;
import dev.galasa.http.IHttpClient;
import dev.galasa.simbank.manager.Account;
import dev.galasa.simbank.manager.AccountType;
import dev.galasa.simbank.manager.IAccount;
import dev.galasa.simbank.manager.ISimBank;
import dev.galasa.simbank.manager.SimBank;

@Test
public class ProvisionedAccountCreditTests {

    @SimBank
    public ISimBank         bank;

    @Account(existing = false, accountType = AccountType.HighValue)
    public IAccount         account;

    @BundleResources
    public IBundleResources resources;

    @HttpClient
    public IHttpClient      client;

    @StoredArtifactRoot
    public Path             artifactRoot;

    @Logger
    public Log              logger;

    /**
     * Test which checks the initial balance of an account, uses the webservice to
     * credit the account, then checks the balance again. The test passes if the
     * final balance is equal to the old balance + the credited amount.
     * 
     * This test is an improved version of BasicAccountCreditTest.
     * Log in is not hard coded and makes use of the credentials.properties file to get the credentials.
     * 
     * The test make use of the Simbank manager. The Simbank manager provides contextual knowledge for the Simbank application 
     * The tester does not need to know about how to log on or make an account, they can simply use the 
     * Simbank manager's annotations and their methods to accomplish this. 
     * The Simbank manager encapsulates the different technologies used to make up the application where 
     * Simbank's managers use other managers to perform a task such as "log on".
     * 
     * This test uses a provisioned account object.
     * The getBalance() method defined in the BasicAccountCreditTest has been moved and adapted into the Account implementation to declutter the test class.
     * 
     * Navigating through menus is not hard coded and makes use of methods such as '.getBalance()' to handle this
     * which makes the test focus on the test objective itself.
     * 
     * @throws Exception catchall exception
     */
    @Test
    public void updateAccountWebServiceTest() throws Exception {
        // Obtain the initial balance
        BigDecimal userBalance = account.getBalance();

        logger.info("Pre-test balance is " + userBalance.toString());

        // Set the amount be credited and call web service
        BigDecimal amount = BigDecimal.valueOf(500.50);
        logger.info("Will credit account with " + amount.toString());
        HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("ACCOUNT_NUMBER", account.getAccountNumber());
        parameters.put("AMOUNT", amount.toString());

        // Load sample request with the given parameters
        String textContent = resources.retrieveSkeletonFileAsString("/resources/skeletons/testSkel.skel", parameters);

        logger.info("Credit actioned");

        // Store the xml request in the test results archive
        storeOutput("webservice", "request.txt", textContent);

        // Invoke the web request
        client.setURI(new URI(bank.getFullAddress()));
        String response = client.postText(bank.getUpdateAddress(), textContent).getContent();

        // Store the response in the test results archive
        storeOutput("webservice", "response.txt", response);

        // Obtain the final balance
        BigDecimal newUserBalance = account.getBalance();
        logger.info("Post-test balance is " + newUserBalance.toString());

        // Assert that the correct amount has been credited to the account
        assertThat(newUserBalance).isEqualTo(userBalance.add(amount));

        logger.info("Balances matched");
    }

    private void storeOutput(String folder, String file, String content) throws IOException {
        // Store the xml request in the test results archive
        Path requestPath = artifactRoot.resolve(folder).resolve(file);
        Files.write(requestPath, content.getBytes(), new SetContentType(ResultArchiveStoreContentType.TEXT),
                StandardOpenOption.CREATE);
    }

}
