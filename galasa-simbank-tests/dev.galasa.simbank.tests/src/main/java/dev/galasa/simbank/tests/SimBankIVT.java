/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.simbank.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import dev.galasa.Test;
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

@Test
public class SimBankIVT {

    @ZosImage(imageTag = "SIMBANK")
    public IZosImage        image;

    @Zos3270Terminal(imageTag = "SIMBANK")
    public ITerminal        terminal;

    @IBundleResources
    public IBundleResources resources;

    @HttpClient
    public IHttpClient      client;

    @CoreManager
    public ICoreManager     coreManager;

    @Test
    public void testNotNull() {
        // Check all objects loaded
        assertThat(terminal).isNotNull();
        assertThat(resources).isNotNull();
        assertThat(client).isNotNull();
    }

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
    public void checkBankIsAvailable() throws TestBundleResourceException, URISyntaxException, IOException,
            HttpClientException, ZosManagerException, DatastreamException, TimeoutException, KeyboardLockedException,
            NetworkException, FieldNotFoundException, TextNotFoundException, InterruptedException {
        // Register the password to the confidential text filtering service
        coreManager.registerConfidentialText("SYS1", "IBMUSER password");

        // Logon through the session manager
        terminal.waitForKeyboard().positionCursorToFieldContaining("Userid").tab().type("IBMUSER")
                .positionCursorToFieldContaining("Password").tab().type("SYS1").enter().waitForKeyboard();

        // Assert that the session manager has a bank session available
        assertThat(terminal.retrieveScreen()).containsOnlyOnce("SIMPLATFORM MAIN MENU");
        assertThat(terminal.retrieveScreen()).containsOnlyOnce("BANKTEST");

        // Open banking application
        terminal.pf1().waitForKeyboard().clear().waitForKeyboard();
           
        terminal.type("bank").enter().waitForKeyboard();

        // Assert that the bank menu is showing
        assertThat(terminal.retrieveScreen()).containsOnlyOnce("Options     Description        PFKey ");
        assertThat(terminal.retrieveScreen()).containsOnlyOnce("BROWSE      Browse Accounts    PF1");
        assertThat(terminal.retrieveScreen()).containsOnlyOnce("TRANSF      Transfer Money     PF4");
    }
}
