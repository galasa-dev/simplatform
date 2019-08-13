
package galasa.test;

import dev.voras.Test;
import dev.voras.common.artifact.ArtifactManager;
import dev.voras.common.artifact.IArtifactManager;
import dev.voras.common.artifact.TestBundleResourceException;
import dev.voras.common.http.HttpClient;
import dev.voras.common.http.HttpClientException;
import dev.voras.common.http.IHttpClient;
import dev.voras.common.zos.IZosImage;
import dev.voras.common.zos.ZosImage;
import dev.voras.common.zos3270.ITerminal;
import dev.voras.common.zos3270.Zos3270Terminal;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

@Test
public class SimframeTest{ 

    @ZosImage(imageTag="A")
    public IZosImage image;

    @Zos3270Terminal(imageTag="A")
    public ITerminal terminal;

    @ArtifactManager
    public IArtifactManager artifacts;

    @HttpClient
    public IHttpClient client;

    @Test
    public void testNotNull() {
        //Check all objects loaded
        assertThat(terminal).isNotNull();
        assertThat(artifacts).isNotNull();
        assertThat(client).isNotNull();
    }

    @Test
    public void updateAccountWebServiceTest() throws TestBundleResourceException, URISyntaxException, IOException, HttpClientException {
        //Initial actions to get into banking application
        login();

        //Obtain the initial balance
        Double userBalance = getBalance("123456789");

        //Set the amount be crecited and call web service
        Double amount = 500.50;
        HashMap<String,Object> parameters = new HashMap<String,Object>();
        parameters.put("ACCOUNT_NUMBER", "123456789");
        parameters.put("SORT_CODE", "000000"); //Needed or XML Not generated correctly. ++ notation could be removed from XML to be cleaner
        parameters.put("AMOUNT", "500.50");

        //Load sample request with the given parameters
        InputStream is = artifacts.getBundleResources(this.getClass()).retrieveSkeletonFile("/resources/skeletons/testSkel.skel", parameters);
        String textContext = artifacts.getBundleResources(this.getClass()).streamAsString(is);

        //Invoke the web request
        client.setURI(new URI("http://127.0.0.1:2080"));
        Object response = client.postTextAsXML("updateAccount", textContext, false);

        //Obtain the final balance
        Double newUserBalance = getBalance("123456789");

        //Assert that the correct amount has been credited to the account
        assertThat(newUserBalance).isEqualTo(userBalance + amount);
    }

    private void login() {
        try {
            //Initial log in to system
            terminal.waitForKeyboard()
                    .positionCursorToFieldContaining("Userid").tab().type("boo")
                    .positionCursorToFieldContaining("Password").tab().type("eek")
                    .enter().waitForKeyboard()

            //Open banking application
                    .pf1().waitForKeyboard()
                    .clear().waitForKeyboard()
                    .tab().type("bank").enter().waitForKeyboard();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private Double getBalance(String accountNum) {
        Double amount = 0.0;
        try {
            //Open account menu and enter account number
            terminal.pf1().waitForKeyboard()
                    .positionCursorToFieldContaining("Account Number").tab()
                    .type(accountNum).enter().waitForKeyboard();

            //Retrieve balance from screen
            amount = Double.parseDouble(terminal.retrieveFieldTextAfterFieldWithString("Balance").trim());

            //Return to bank menu
            terminal.pf3().waitForKeyboard();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return amount;
    }
}