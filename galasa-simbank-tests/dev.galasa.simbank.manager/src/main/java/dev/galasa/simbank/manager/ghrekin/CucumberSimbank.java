package dev.galasa.simbank.manager.ghrekin;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.util.HashMap;

import dev.galasa.artifact.IArtifactManager;
import dev.galasa.artifact.IBundleResources;
import dev.galasa.http.IHttpClient;
import dev.galasa.simbank.manager.IAccount;
import dev.galasa.simbank.manager.ISimBank;
import dev.galasa.simbank.manager.SimBankManagerException;

@CucumberTranslator
public class CucumberSimbank {

    @Given(regex = "I have an account with a balance of -?([0-9])+", type = "number", dependencies = "isimbank;iartifactmanager;ihttpclient", codeImports = "dev.galasa.simbank.manager.Account;dev.galasa.simbank.manager.IAccount")
    public static String iaccount = "@Account(balance = @value_here@)\npublic IAccount @name_here@;";

    @Given(regex = "I have an account that doesn't exist", type = "", dependencies = "isimbank;iartifactmanager;ihttpclient", codeImports = "dev.galasa.simbank.manager.Account;dev.galasa.simbank.manager.IAccount;dev.galasa.simbank.manager.AccountType")
    public static String iaccount1 = "@Account(accountType = AccountType.UnOpened)\npublic IAccount @name_here@;";

    @Given(regex = "", type = "", dependencies = "", codeImports = "dev.galasa.simbank.manager.SimBank;dev.galasa.simbank.manager.ISimBank")
    public static String isimbank = "@SimBank\npublic ISimBank @name_here@;";

    @Given(regex = "", type = "", dependencies = "", codeImports = "dev.galasa.artifact.ArtifactManager;dev.galasa.artifact.IArtifactManager")
    public static String iartifactmanager = "@ArtifactManager\npublic IArtifactManager @name_here@;";

    @Given(regex = "", type = "", dependencies = "", codeImports = "dev.galasa.http.HttpClient;dev.galasa.http.IHttpClient")
    public static String ihttpclient = "@HttpClient\npublic IHttpClient @name_here@;";

    @When(regex = "the web API is called to credit the account with -?([0-9])+", type = "number")
    public static Exception whenTheWebApiIsCalledToCreditTheAccountWith(String amount, IAccount account, @Unique IArtifactManager artifacts, Class<?> testClass, @Unique IHttpClient client, @Unique ISimBank bank) {

        client.build();

        HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("ACCOUNT_NUMBER", account.getAccountNumber());
        parameters.put("AMOUNT", amount);

        try {
            IBundleResources resources = artifacts.getBundleResources(testClass);
            InputStream is = resources.retrieveSkeletonFile("/resources/skeletons/testSkel.skel", parameters);
            String textContent = resources.streamAsString(is);

            client.setURI(new URI(bank.getFullAddress()));
            client.postTextAsXML(bank.getUpdateAddress(), textContent, false);
        } catch (Exception e) {
            return e;
        }
        return null;
    }

    @Then(regex = "the balance of the account should be -?([0-9])+", type = "number")
    public static void thenTheBalanceOfTheAccountShouldBe(String amount, IAccount account) {
        try {
            assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal(amount));
        } catch (SimBankManagerException e) {
            e.printStackTrace();
        }
    }

    @Then(regex = "an? ([A-z])+ Exception is thrown", type = "exception")
    public static void thenASpecificExceptionIsThrown(Exception thrown) {
        assertThat(thrown.getMessage()).contains("400: 'Method Not Allowed'");
    }

}