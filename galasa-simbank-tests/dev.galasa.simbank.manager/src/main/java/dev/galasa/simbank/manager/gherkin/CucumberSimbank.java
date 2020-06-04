package dev.galasa.simbank.manager.gherkin;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.util.HashMap;

import dev.galasa.artifact.IArtifactManager;
import dev.galasa.artifact.IBundleResources;
import dev.galasa.core.manager.ICoreManager;
import dev.galasa.http.IHttpClient;
import dev.galasa.simbank.manager.IAccount;
import dev.galasa.simbank.manager.ISimBank;
import dev.galasa.simbank.manager.SimBankManagerException;
import dev.galasa.zos3270.FieldNotFoundException;
import dev.galasa.zos3270.ITerminal;
import dev.galasa.zos3270.KeyboardLockedException;
import dev.galasa.zos3270.TerminalInterruptedException;
import dev.galasa.zos3270.TextNotFoundException;
import dev.galasa.zos3270.TimeoutException;
import dev.galasa.zos3270.spi.NetworkException;

@CucumberTranslator
public class CucumberSimbank {

    @Given(regex = "I have an account with a balance of (-?[0-9]+)", type = "number", dependencies = "isimbank;iartifactmanager;ihttpclient", codeImports = "dev.galasa.simbank.manager.Account;dev.galasa.simbank.manager.IAccount")
    public static String iaccount = "@Account(balance = @value_here@)\npublic IAccount @name_here@;";

    @Given(regex = "I have an account that doesn't exist", dependencies = "isimbank;iartifactmanager;ihttpclient", codeImports = "dev.galasa.simbank.manager.Account;dev.galasa.simbank.manager.IAccount;dev.galasa.simbank.manager.AccountType")
    public static String iaccount1 = "@Account(accountType = AccountType.UnOpened)\npublic IAccount @name_here@;";

    @Given(codeImports = "dev.galasa.zos.ZosImage;dev.galasa.zos.IZosImage")
    public static String iimage = "@ZosImage(imageTag = \"simbank\")\npublic IZosImage @name_here@;";

    @Given(regex = "The Simbank is available", dependencies = "iimage;icoremanager", codeImports = "dev.galasa.zos3270.Zos3270Terminal;dev.galasa.zos3270.ITerminal")
    public static String iterminal = "@Zos3270Terminal(imageTag = \"simbank\")\npublic ITerminal @name_here@;";

    @Given(codeImports = "dev.galasa.core.manager.CoreManager;dev.galasa.core.manager.ICoreManager")
    public static String icoremanager = "@CoreManager\npublic ICoreManager @name_here@;";

    @Given(codeImports = "dev.galasa.simbank.manager.SimBank;dev.galasa.simbank.manager.ISimBank")
    public static String isimbank = "@SimBank\npublic ISimBank @name_here@;";

    @Given(codeImports = "dev.galasa.artifact.ArtifactManager;dev.galasa.artifact.IArtifactManager")
    public static String iartifactmanager = "@ArtifactManager\npublic IArtifactManager @name_here@;";

    @Given(codeImports = "dev.galasa.http.HttpClient;dev.galasa.http.IHttpClient")
    public static String ihttpclient = "@HttpClient\npublic IHttpClient @name_here@;";

    @When(regex = "the web API is called to credit the account with (-?[0-9]+)", type = "number")
    public static Exception whenTheWebApiIsCalledToCreditTheAccountWith(String amount, IAccount account,
            @Unique IArtifactManager artifacts, Class<?> testClass, @Unique IHttpClient client, @Unique ISimBank bank) {

        client.build();

        HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("ACCOUNT_NUMBER", account.getAccountNumber());
        parameters.put("AMOUNT", amount);

        try {
            IBundleResources resources = artifacts.getBundleResources(testClass);
            InputStream is = resources.retrieveSkeletonFile("/resources/skeletons/testSkel.skel", parameters);
            String textContent = resources.streamAsString(is);

            client.setURI(new URI(bank.getFullAddress()));
            client.postText(bank.getUpdateAddress(), textContent);
        } catch (Exception e) {
            return e;
        }
        return null;
    }

    @When(regex = "I navigate to SimBank", type = "")
    public static String whenYouNavigateToBank(@Unique ICoreManager manager, ITerminal terminal)
            throws TimeoutException, KeyboardLockedException, NetworkException, FieldNotFoundException,
            TextNotFoundException, TerminalInterruptedException {

        manager.registerConfidentialText("SYS1", "IBMUSER password");

        terminal.waitForKeyboard().positionCursorToFieldContaining("Userid").tab().type("IBMUSER")
        .positionCursorToFieldContaining("Password").tab().type("SYS1").enter().waitForKeyboard();

        return terminal.retrieveScreen();
    }

    @Then(regex = "the balance of the account should be (-?[0-9]+)", type = "number")
    public static void thenTheBalanceOfTheAccountShouldBe(String amount, Exception exception, IAccount account) {
        assertThat(exception).isNull();
        try {
            assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal(amount));
        } catch (SimBankManagerException e) {
            e.printStackTrace();
        }
    }

    @Then(regex = "an? ([A-z]+) Exception is thrown", type = "exception")
    public static void thenASpecificExceptionIsThrown(Exception thrown) {
        assertThat(thrown.getMessage()).contains("400: 'Method Not Allowed'");
    }

    @Then(regex = "I should see the main screen", type = "")
    public static void thenTheMainScreenShouldBeVisible(String screen){
        assertThat(screen).containsOnlyOnce("SIMPLATFORM MAIN MENU");
        assertThat(screen).containsOnlyOnce("BANKTEST");
    }

}