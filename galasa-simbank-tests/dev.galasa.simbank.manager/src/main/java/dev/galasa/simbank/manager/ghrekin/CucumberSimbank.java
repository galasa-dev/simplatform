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

public class CucumberSimbank {

    @When(regex = "I have an account with balance of -?([0-9])+")
    public static Exception theWebApiIsCalledToCreditTheAccountWith(BigDecimal amount, IAccount account, IArtifactManager artifacts, Class testClass, IHttpClient client, ISimBank bank) {
        HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("ACCOUNT_NUMBER", account.getAccountNumber());
        parameters.put("AMOUNT", amount.toString());

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

    @Then(regex = "the balance of the account should be -?(0-9)+")
    public static void theBalaceOfTheAccountShouldBe(BigDecimal amount, IAccount account) {
        try {
            assertThat(account.getBalance()).isEqualTo(amount);
        } catch (SimBankManagerException e) {
            e.printStackTrace();
        }
    }

    @Then(regex = "an? ([A-z])+ Exception is thrown")
    public static void aSpecificExceptionIsThrown(Exception thrown, Exception expected) {
        assertThat(thrown.getClass()).isExactlyInstanceOf(expected.getClass());
    }

}