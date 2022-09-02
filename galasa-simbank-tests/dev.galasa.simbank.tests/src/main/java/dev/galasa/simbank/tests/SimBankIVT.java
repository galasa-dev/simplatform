/*
 * Copyright contributors to the Galasa project
 */
package dev.galasa.simbank.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;

import dev.galasa.Test;
import dev.galasa.artifact.BundleResources;
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
import dev.galasa.zos3270.TerminalInterruptedException;
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

    @BundleResources
    public IBundleResources resources;

    @HttpClient
    public IHttpClient      client;

    @CoreManager
    public ICoreManager     coreManager;

    /**
     * Test which logs onto Galasa SimBank using Zos3270Terminal manager. Checks to see if the Simbank application is reachable.
     * 
     * Credentials are hard coded here which raises security issues.
     * Credentials should be given in Galasa configuration so they can be called in to the test when needed.
     * Log in is usually handled and defined in the image so that the log in process shouldn't have to be repeated in every test.
     * These details would be stored in credentials.properties and referenced in the cps.properties file.
     * See https://galasa.dev/docs/getting-started/ for more details. 
     * 
     * @throws Exception
     */
    @Test
    public void checkBankIsAvailable() throws Exception{
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
