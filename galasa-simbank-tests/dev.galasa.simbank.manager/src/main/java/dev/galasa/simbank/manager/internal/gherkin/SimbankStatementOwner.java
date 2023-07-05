/*
 * Copyright contributors to the Galasa project
 */
package dev.galasa.simbank.manager.internal.gherkin;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import dev.galasa.artifact.IBundleResources;
import dev.galasa.artifact.TestBundleResourceException;
import dev.galasa.framework.spi.IGherkinExecutable;
import dev.galasa.framework.spi.IStatementOwner;
import dev.galasa.framework.spi.language.gherkin.ExecutionMethod;
import dev.galasa.framework.spi.language.gherkin.GherkinKeyword;
import dev.galasa.http.HttpClientException;
import dev.galasa.http.IHttpClient;
import dev.galasa.simbank.manager.IAccount;
import dev.galasa.simbank.manager.SimBankManagerException;
import dev.galasa.simbank.manager.internal.SimBankImpl;
import dev.galasa.simbank.manager.internal.SimBankManagerImpl;

public class SimbankStatementOwner implements IStatementOwner {

    SimBankManagerImpl manager;
    IBundleResources resources;
    IHttpClient client;
    IAccount provisionedAccount;

    public SimbankStatementOwner(SimBankManagerImpl manager) {
        this.manager = manager;
    }

    public void setHttpClient(IHttpClient client) {
        this.client = client;
    }

    public void setBundleResources(IBundleResources resources) {
        this.resources = resources;
    }

    @ExecutionMethod(keyword = GherkinKeyword.GIVEN, regex = "The Simbank is available")
    public void generateSimBank(IGherkinExecutable executable, Map<String, Object> testVariables)
            throws SimBankManagerException {
        manager.generateSimBank(true, true);
    }

    @ExecutionMethod(keyword = GherkinKeyword.WHEN, regex = "I navigate to SimBank")
    public void navigateToMainMenu(IGherkinExecutable executable, Map<String, Object> testVariables)
            throws SimBankManagerException {
        SimBankImpl bank = manager.getSimBank();
        bank.start();
    }

    @ExecutionMethod(keyword = GherkinKeyword.THEN, regex = "I should see the main screen")
    public void onMainMenu(IGherkinExecutable executable, Map<String, Object> testVariables)
            throws SimBankManagerException {
        SimBankImpl bank = manager.getSimBank();
        String screen = bank.getControlTerminal().retrieveScreen();

        assertOnScreen(screen, "Options     Description        PFKey ");
        assertOnScreen(screen, "BROWSE      Browse Accounts    PF1");
        assertOnScreen(screen, "TRANSF      Transfer Money     PF4");
    }

    public void assertOnScreen(String screen, String expected) throws SimBankManagerException {
        if (!screen.contains(expected)) {
            throw new SimBankManagerException("Could not find:\n" + expected + "\n\nScreen:\n" + screen);
        }
    }

    @ExecutionMethod(keyword = GherkinKeyword.GIVEN, regex = "I have an account with a balance of (\\d+.\\d\\d)")
    public void generateAccount(IGherkinExecutable executable, Map<String, Object> testVariables)
            throws SimBankManagerException {
        if (manager.getSimBank() == null) {
            generateSimBank(executable, testVariables);
        }
        manager.getSimBank().start();

        String balance = executable.getRegexGroups().get(0);
        provisionedAccount = manager.generateSimBankAccount(false, null, balance);
    }

    @ExecutionMethod(keyword = GherkinKeyword.WHEN, regex = "The web API is called to credit the account with (\\d+.\\d\\d)")
    public void creditAccountWithWebApi(IGherkinExecutable executable, Map<String, Object> testVariables)
            throws SimBankManagerException {
        try {
            SimBankImpl bank = manager.getSimBank();
            client.setURI(new URI(bank.getFullAddress()));

            if (provisionedAccount == null) {
                throw new SimBankManagerException("A SimBank Account has not been provisioned");
            }

            Map<String, Object> params = new HashMap<>();
            params.put("ACCOUNT_NUMBER", provisionedAccount.getAccountNumber());
            params.put("AMOUNT", executable.getRegexGroups().get(0));
            String text = resources.retrieveSkeletonFileAsString("/webApi.skel", params);

            client.postText(bank.getUpdateAddress(), text);
        } catch (URISyntaxException e) {
            throw new SimBankManagerException("Unable to parse SimBank URI");
        } catch (HttpClientException e) {
            throw new SimBankManagerException("Issue posting data to SimBank web API");
        } catch (TestBundleResourceException | IOException e) {
            throw new SimBankManagerException("Issue reading Web Api skeleton file from manager bundle");
        }
    }

    @ExecutionMethod(keyword = GherkinKeyword.THEN, regex = "The balance of the account should be (\\d+.\\d\\d)")
    public void checkAccountBalance(IGherkinExecutable executable, Map<String, Object> testVariables)
            throws SimBankManagerException {
        if (provisionedAccount == null) {
            throw new SimBankManagerException("A SimBank Account has not been provisioned");
        }

        BigDecimal expected = new BigDecimal(executable.getRegexGroups().get(0));
        if(provisionedAccount.getBalance().compareTo(expected) != 0) {
            throw new SimBankManagerException("Balance not equal to expected. Expected " + expected + " Got " + provisionedAccount.getBalance());
        }
    }
}