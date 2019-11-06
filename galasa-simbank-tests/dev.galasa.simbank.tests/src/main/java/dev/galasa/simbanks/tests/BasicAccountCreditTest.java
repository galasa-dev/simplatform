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
import java.net.URISyntaxException;
import java.util.HashMap;

import dev.galasa.Test;
import dev.galasa.artifact.ArtifactManager;
import dev.galasa.artifact.IArtifactManager;
import dev.galasa.artifact.IBundleResources;
import dev.galasa.artifact.TestBundleResourceException;
import dev.galasa.core.manager.CoreManager;
import dev.galasa.core.manager.ICoreManager;
import dev.galasa.http.HttpClient;
import dev.galasa.http.HttpClientException;
import dev.galasa.http.IHttpClient;
import dev.galasa.zos.IZosImage;
import dev.galasa.zos.ZosImage;
import dev.galasa.zos.ZosManagerException;
import dev.galasa.zos3270.FieldNotFoundException;
import dev.galasa.zos3270.ITerminal;
import dev.galasa.zos3270.KeyboardLockedException;
import dev.galasa.zos3270.TextNotFoundException;
import dev.galasa.zos3270.TimeoutException;
import dev.galasa.zos3270.Zos3270Terminal;
import dev.galasa.zos3270.spi.DatastreamException;
import dev.galasa.zos3270.spi.NetworkException;
// test pr
@Test
public class BasicAccountCreditTest {

    @ZosImage(imageTag = "simbank")
    public IZosImage        image;

    @Zos3270Terminal(imageTag = "simbank")
    public ITerminal        terminal;

    @ArtifactManager
    public IArtifactManager artifacts;

    @CoreManager
    public ICoreManager     coreManager;

    @HttpClient
    public IHttpClient      client;

    /**
     * Test which checks the initial balance of an account, uses the webservice to
     * credit the account, then checks the balance again. The test passes if the
     * final balance is equal to the old balance + the credited amount.
     * 
     * @throws TestBundleResourceException
     * @throws URISyntaxException
     * @throws IOException
     * @throws HttpClientException
     * @throws ZosManagerException
     * @throws TextNotFoundException
     * @throws FieldNotFoundException
     * @throws NetworkException
     * @throws KeyboardLockedException
     * @throws TimeoutException
     * @throws DatastreamException
     * @throws InterruptedException
     */
    @Test
    public void updateAccountWebServiceTest() throws TestBundleResourceException, URISyntaxException, IOException,
            HttpClientException, ZosManagerException, DatastreamException, TimeoutException, KeyboardLockedException,
            NetworkException, FieldNotFoundException, TextNotFoundException, InterruptedException {
        // Register the password to the confidential text filtering service
        coreManager.registerConfidentialText("SYS1", "IBMUSER password");

        // Initial actions to get into banking application
        terminal.waitForKeyboard().positionCursorToFieldContaining("Userid").tab().type("IBMUSER")
                .positionCursorToFieldContaining("Password").tab().type("SYS1").enter().waitForKeyboard()

                // Open banking application
                .pf1().waitForKeyboard().clear().waitForKeyboard().tab().type("bank").enter().waitForKeyboard();

        // Obtain the initial balance
        BigDecimal userBalance = getBalance("123456789");

        // Set the amount be credited and call web service
        BigDecimal amount = BigDecimal.valueOf(500.50);
        HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("ACCOUNT_NUMBER", "123456789");
        parameters.put("AMOUNT", amount.toString());

        // Load sample request with the given parameters
        IBundleResources resources = artifacts.getBundleResources(this.getClass());
        InputStream is = resources.retrieveSkeletonFile("/resources/skeletons/testSkel.skel", parameters);
        String textContext = resources.streamAsString(is);

        // Invoke the web request
        client.setURI(new URI("http://" + image.getDefaultHostname() + ":2080"));
        client.postTextAsXML("updateAccount", textContext, false);

        // Obtain the final balance
        BigDecimal newUserBalance = getBalance("123456789");

        // Assert that the correct amount has been credited to the account
        assertThat(newUserBalance).isEqualTo(userBalance.add(amount));
    }

    /**
     * Navigate through the banking application and extract the balance of a given
     * account
     * 
     * @param accountNum - Account Number of the account being queried
     * @return Balance of the account being queried
     * @throws TextNotFoundException
     * @throws FieldNotFoundException
     * @throws NetworkException
     * @throws KeyboardLockedException
     * @throws TimeoutException
     * @throws DatastreamException
     * @throws InterruptedException
     */
    private BigDecimal getBalance(String accountNum)
            throws DatastreamException, TimeoutException, KeyboardLockedException, NetworkException,
            FieldNotFoundException, TextNotFoundException, InterruptedException {
        BigDecimal amount = BigDecimal.ZERO;
        // Open account menu and enter account number
        terminal.pf1().waitForKeyboard().positionCursorToFieldContaining("Account Number").tab().type(accountNum)
                .enter().waitForKeyboard();

        // Retrieve balance from screen
        amount = new BigDecimal(terminal.retrieveFieldTextAfterFieldWithString("Balance").trim());

        // Return to bank menu
        terminal.pf3().waitForKeyboard();
        return amount;
    }
}
