
package galasa.test;

import dev.galasa.Test;
import dev.galasa.common.artifact.ArtifactManager;
import dev.galasa.common.artifact.IArtifactManager;
import dev.galasa.common.artifact.IBundleResources;
import dev.galasa.common.artifact.TestBundleResourceException;
import dev.galasa.common.http.HttpClient;
import dev.galasa.common.http.HttpClientException;
import dev.galasa.common.http.IHttpClient;
import dev.galasa.common.zos.IZosImage;
import dev.galasa.common.zos.ZosImage;
import dev.galasa.common.zos.ZosManagerException;
import dev.galasa.common.zos3270.ITerminal;
import dev.galasa.common.zos3270.Zos3270Terminal;
import galasa.manager.Account;
import galasa.manager.IAccount;
import galasa.manager.ISimBank;
import galasa.manager.SimBank;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

@Test
public class SimframeTestWithManager{ 

    @ZosImage(imageTag="A")
    public IZosImage image;

    @Zos3270Terminal(imageTag="A")
    public ITerminal terminal;

    @ArtifactManager
    public IArtifactManager artifacts;

    @HttpClient
    public IHttpClient client;

    @SimBank
    public ISimBank bank;

    @Account
    public IAccount account;

    @Test
    public void testNotNull() {
        //Check all objects loaded
        assertThat(terminal).isNotNull();
        assertThat(artifacts).isNotNull();
        assertThat(client).isNotNull();
        assertThat(bank).isNotNull();
        assertThat(account).isNotNull();
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
     */
    @Test
    public void updateAccountWebServiceTest() throws TestBundleResourceException, URISyntaxException, IOException, HttpClientException, ZosManagerException {
        //Initial actions to get into banking application
        login();

        //Obtain the initial balance
        BigDecimal userBalance = getBalance(account.getAccountNumber());

        //Set the amount be credited and call web service
        BigDecimal amount = BigDecimal.valueOf(500.50);
        HashMap<String,Object> parameters = new HashMap<String,Object>();
        parameters.put("ACCOUNT_NUMBER", account.getAccountNumber());
        parameters.put("AMOUNT", amount.toString());

        //Load sample request with the given parameters
        IBundleResources resources = artifacts.getBundleResources(this.getClass());
        InputStream is = resources.retrieveSkeletonFile("/resources/skeletons/testSkel.skel", parameters);
        String textContext = resources.streamAsString(is);

        //Invoke the web request
        client.setURI(new URI(bank.getFullAddress()));
        Object response = client.postTextAsXML(bank.getUpdateAddress(), textContext, false);

        //Obtain the final balance
        BigDecimal newUserBalance = getBalance(account.getAccountNumber());

        //Assert that the correct amount has been credited to the account
        assertThat(newUserBalance).isEqualTo(userBalance.add(amount));
    }

    /**
     * Initial actions required to log in to system and open the banking application
     */
    private void login() {
        try {
            //Initial log in to system
            terminal.waitForKeyboard()
                    .positionCursorToFieldContaining("Userid").tab().type("IBMUSER")
                    .positionCursorToFieldContaining("Password").tab().type("SYS1")
                    .enter().waitForKeyboard()

            //Open banking application
                    .pf1().waitForKeyboard()
                    .clear().waitForKeyboard()
                    .tab().type("bank").enter().waitForKeyboard();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Navigate through the banking application and extract the balance of a given account
     * 
     * @param accountNum - Account Number of the accont being queried
     * @return Balance of the account being queried
     */
    private BigDecimal getBalance(String accountNum) {
        BigDecimal amount = BigDecimal.ZERO;
        try {
            //Open account menu and enter account number
            terminal.pf1().waitForKeyboard()
                    .positionCursorToFieldContaining("Account Number").tab()
                    .type(accountNum).enter().waitForKeyboard();

            //Retrieve balance from screen
            amount = BigDecimal.valueOf(Double.parseDouble(terminal.retrieveFieldTextAfterFieldWithString("Balance").trim()));

            //Return to bank menu
            terminal.pf3().waitForKeyboard();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return amount;
    }
}