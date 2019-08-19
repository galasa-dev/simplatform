package galasa.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import dev.galasa.ResultArchiveStoreContentType;
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
import dev.galasa.core.manager.StoredArtifactRoot;
import galasa.manager.Account;
import galasa.manager.IAccount;
import galasa.manager.ISimBank;
import galasa.manager.SimBank;

public class ProvisionedAccountCreditTests{ 

    @ZosImage(imageTag="A")
    public IZosImage image;

    @Zos3270Terminal(imageTag="A")
    public ITerminal terminal;

    @ArtifactManager
    public IArtifactManager artifacts;

    @HttpClient
    public IHttpClient client;

    @StoredArtifactRoot
    public Path artifactRoot;

    //Binding to provisioned bank endpoints
    @SimBank(imageTag="A")
    public ISimBank bank;

    //Provision account data
    @Account
    public IAccount account;

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
        //Obtain the initial balance
        BigDecimal userBalance = bank.getBalance(account.getAccountNumber());

        //Set the amount be credited and call web service
        BigDecimal amount = BigDecimal.valueOf(500.50);
        HashMap<String,Object> parameters = new HashMap<String,Object>();
        parameters.put("ACCOUNT_NUMBER", account.getAccountNumber());
        parameters.put("AMOUNT", amount.toString());

        //Load sample request with the given parameters
        IBundleResources resources = artifacts.getBundleResources(this.getClass());
        InputStream is = resources.retrieveSkeletonFile("/resources/skeletons/testSkel.skel", parameters);
        String textContent = resources.streamAsString(is);

        //Store the xml request in the test results archive
        storeOutput("webservice", "request.txt", textContent);

        //Invoke the web request
        client.setURI(new URI(bank.getFullAddress()));
        String response = (String) client.postTextAsXML(bank.getUpdateAddress(), textContent, false);

        //Store the response in the test results archive
        storeOutput("webservice", "response.txt", response);

        //Obtain the final balance
        BigDecimal newUserBalance = bank.getBalance(account.getAccountNumber());

        //Assert that the correct amount has been credited to the account
        assertThat(newUserBalance).isEqualTo(userBalance.add(amount));
    }
    
    private void storeOutput(String folder, String file, String content) throws IOException {
    	//Store the xml request in the test results archive
        Path requestPath = artifactRoot.resolve(folder).resolve(file);
        Files.createFile(requestPath, ResultArchiveStoreContentType.TEXT);
        Files.write(requestPath,content.getBytes());
    }
}