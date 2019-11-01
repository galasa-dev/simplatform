/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.simbanks.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
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
import dev.galasa.artifact.ArtifactManager;
import dev.galasa.artifact.IArtifactManager;
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

    @ArtifactManager
    public IArtifactManager artifacts;

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
        IBundleResources resources = artifacts.getBundleResources(this.getClass());
        InputStream is = resources.retrieveSkeletonFile("/resources/skeletons/testSkel.skel", parameters);
        String textContent = resources.streamAsString(is);

        logger.info("Credit actioned");

        // Store the xml request in the test results archive
        storeOutput("webservice", "request.txt", textContent);

        // Invoke the web request
        client.setURI(new URI(bank.getFullAddress()));
        String response = (String) client.postTextAsXML(bank.getUpdateAddress(), textContent, false);

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
