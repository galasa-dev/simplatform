package dev.galasa.simbank.manager.internal.gherkin;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;

import dev.galasa.framework.spi.ConfigurationPropertyStoreException;
import dev.galasa.framework.spi.language.gherkin.GherkinKeyword;
import dev.galasa.simbank.manager.SimBankManagerException;
import dev.galasa.simbank.manager.internal.AccountImpl;
import dev.galasa.simbank.manager.internal.SimBankManagerImpl;

public class GherkinAccount {

    public final static GherkinKeyword keyword = GherkinKeyword.GIVEN;

    public final static Pattern pattern = Pattern.compile("I have an account with a balance of (\\d+)");

    public final static Class<?>[] dependencies = {};

    public final static String ACCOUNT_VARIABLE = "PROVISIONED_BALANCE_ACCOUNT";

    public static void execute(Matcher matcher, SimBankManagerImpl manager, Log logger, Map<String, Object> testVariables) throws SimBankManagerException, ConfigurationPropertyStoreException {
        if(manager.getSimBank() == null) {
            GherkinSimbank.execute(manager, logger);
        }

        String balance = matcher.group(1);
        AccountImpl account = AccountImpl.generate(manager, false, null, balance);
        manager.addAccount(account.getAccountNumber(), account);
        logger.info("Provisioned account " + account.getAccountNumber());

        testVariables.put(ACCOUNT_VARIABLE, account.getAccountNumber());
    }
}