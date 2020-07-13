package dev.galasa.simbank.manager.internal.gherkin;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.galasa.framework.spi.ConfigurationPropertyStoreException;
import dev.galasa.framework.spi.language.gherkin.GherkinKeyword;
import dev.galasa.simbank.manager.SimBankManagerException;
import dev.galasa.simbank.manager.internal.AccountImpl;
import dev.galasa.simbank.manager.internal.SimBankManagerImpl;

public class GherkinAccountBalance {

    public final static GherkinKeyword keyword = GherkinKeyword.THEN;

    public final static Pattern pattern = Pattern.compile("The balance of the account should be (\\d+)");

    public final static Class<?>[] dependencies = {};

    public static void execute(Matcher matcher, SimBankManagerImpl manager, Map<String, Object> testVariables) throws SimBankManagerException, ConfigurationPropertyStoreException {
        String accountNumber = (String) testVariables.get(GherkinAccount.ACCOUNT_VARIABLE);
        AccountImpl account = manager.getAccount(accountNumber);

        String expected = matcher.group(1);

        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal(expected));
    }
}