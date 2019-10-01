
package dev.galasa.simbanks.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;

import dev.galasa.Test;
import dev.galasa.common.artifact.ArtifactManager;
import dev.galasa.common.artifact.IArtifactManager;
import dev.galasa.common.artifact.TestBundleResourceException;
import dev.galasa.common.http.HttpClient;
import dev.galasa.common.http.HttpClientException;
import dev.galasa.common.http.IHttpClient;
import dev.galasa.common.zos.IZosImage;
import dev.galasa.common.zos.ZosImage;
import dev.galasa.common.zos.ZosManagerException;
import dev.galasa.common.zos3270.FieldNotFoundException;
import dev.galasa.common.zos3270.ITerminal;
import dev.galasa.common.zos3270.KeyboardLockedException;
import dev.galasa.common.zos3270.TextNotFoundException;
import dev.galasa.common.zos3270.TimeoutException;
import dev.galasa.common.zos3270.Zos3270Terminal;
import dev.galasa.common.zos3270.spi.DatastreamException;
import dev.galasa.common.zos3270.spi.NetworkException;
import dev.galasa.core.manager.CoreManager;
import dev.galasa.core.manager.ICoreManager;

@Test
public class SimframeBankIVT{ 

    @ZosImage(imageTag="simframe")
    public IZosImage image;

    @Zos3270Terminal(imageTag="simframe")
    public ITerminal terminal;

    @ArtifactManager
    public IArtifactManager artifacts;

    @HttpClient
    public IHttpClient client;
    
    @CoreManager
    public ICoreManager coreManager;

    @Test
    public void testNotNull() {
        //Check all objects loaded
        assertThat(terminal).isNotNull();
        assertThat(artifacts).isNotNull();
        assertThat(client).isNotNull();
    }

    /**
     * Test which checks the initial balance of an account, uses the webservice to credit the account, then checks the balance again.
     * The test passes if the final balance is equal to the old balance + the credited amount.
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
     */
    @Test
    public void checkBankIsAvailable() throws TestBundleResourceException, URISyntaxException, IOException, HttpClientException, ZosManagerException, DatastreamException, TimeoutException, KeyboardLockedException, NetworkException, FieldNotFoundException, TextNotFoundException {
    	// Register the password to the confidential text filtering service
    	coreManager.registerConfidentialText("SYS1", "IBMUSER password");
    	
    	//Logon through the session manager
    	terminal.waitForKeyboard()
        .positionCursorToFieldContaining("Userid").tab().type("IBMUSER")
        .positionCursorToFieldContaining("Password").tab().type("SYS1")
        .enter().waitForKeyboard();
    	
    	//Assert that the session manager has a bank session available
        assertThat(terminal.retrieveScreen()).containsOnlyOnce("SIMFRAME MAIN MENU");
    	assertThat(terminal.retrieveScreen()).containsOnlyOnce("BANKTEST");
    	
        //Open banking application
        terminal.pf1().waitForKeyboard()
        .clear().waitForKeyboard()
        .tab().type("bank").enter().waitForKeyboard();
    	
        //Assert that the bank menu is showing
        assertThat(terminal.retrieveScreen()).containsOnlyOnce("Options     Description        PFKey ");
        assertThat(terminal.retrieveScreen()).containsOnlyOnce("BROWSE      Browse Accounts    PF1");
        assertThat(terminal.retrieveScreen()).containsOnlyOnce("UPDATE      Update Accounts    PF2");
        assertThat(terminal.retrieveScreen()).containsOnlyOnce("TRANSF      Transfer Money     PF4");
    }
}
