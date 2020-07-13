package dev.galasa.simbank.manager.internal.gherkin;

import java.net.URISyntaxException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.galasa.ManagerException;
import dev.galasa.framework.TestRunException;
import dev.galasa.framework.spi.ConfigurationPropertyStoreException;
import dev.galasa.framework.spi.IGherkinExecutable;
import dev.galasa.framework.spi.IGherkinManager;
import dev.galasa.framework.spi.language.gherkin.GherkinKeyword;
import dev.galasa.framework.spi.language.gherkin.GherkinStatement;
import dev.galasa.http.IHttpManager;
import dev.galasa.simbank.manager.internal.SimBankManagerImpl;

public class GherkinWebApi {

    public final static GherkinKeyword keyword = GherkinKeyword.GIVEN;

    public final static Pattern pattern = Pattern.compile("The web API is called to credit the account with (\\d+)");

    public final static Class<?>[] dependencies = { IHttpManager.class };

    private final static String SIMBANK_SOAP = "<soapenv:Envelope xmlns:soapenv='http://schemas.xmlsoap.org/soap/envelope/'>"
            + "<soapenv:Body>" + "<ns1:UPDACCTOperation xmlns:ns1='http://www.UPDACCT.STCUSTN2.Request.com'>"
            + "<ns1:update_account_record>" + "<ns1:account_key>" + "<ns1:sort_code>00-00-00</ns1:sort_code>"
            + "<ns1:account_number>++ACCOUNT_NUMBER++</ns1:account_number>" + "</ns1:account_key>"
            + "<ns1:account_change>++AMOUNT++</ns1:account_change>"
            + "</ns1:update_account_record></ns1:UPDACCTOperation>" + "</soapenv:Body>" + "</soapenv:Envelope>\n";

    public static void execute(Matcher matcher, SimBankManagerImpl manager, Map<String, Object> testVariables)
            throws ConfigurationPropertyStoreException, URISyntaxException, TestRunException, ManagerException {
        String amount = matcher.group(1);
        String accountNumber = (String) testVariables.get(GherkinAccount.ACCOUNT_VARIABLE);

        String postText = SIMBANK_SOAP.replace("++ACCOUNT_NUMBER++", accountNumber).replace("++AMOUNT++", amount);
        testVariables.put("SIMBANK_SOAP", postText);

        String simBankUri = manager.getSimBank().getFullAddress();
        String simBankUpdate = manager.getSimBank().getUpdateAddress();
        IGherkinExecutable subStatement = GherkinStatement.get("When The Http Client posts text <SIMBANK_SOAP> to URI " + simBankUri + " at endpoint " + simBankUpdate);

        IGherkinManager httpManager = (IGherkinManager) testVariables.get("dev.galasa.http.internal.HttpManagerImpl");

        httpManager.executeGherkin(subStatement, testVariables);
    }
}