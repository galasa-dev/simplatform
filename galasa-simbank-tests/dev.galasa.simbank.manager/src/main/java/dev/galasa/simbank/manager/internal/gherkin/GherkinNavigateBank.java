package dev.galasa.simbank.manager.internal.gherkin;

import java.util.regex.Pattern;

import dev.galasa.framework.spi.ConfigurationPropertyStoreException;
import dev.galasa.framework.spi.language.gherkin.GherkinKeyword;
import dev.galasa.simbank.manager.SimBankManagerException;
import dev.galasa.simbank.manager.internal.SimBankImpl;
import dev.galasa.simbank.manager.internal.SimBankManagerImpl;
import dev.galasa.zos3270.FieldNotFoundException;
import dev.galasa.zos3270.KeyboardLockedException;
import dev.galasa.zos3270.TerminalInterruptedException;
import dev.galasa.zos3270.TextNotFoundException;
import dev.galasa.zos3270.TimeoutException;
import dev.galasa.zos3270.spi.DatastreamException;
import dev.galasa.zos3270.spi.NetworkException;

public class GherkinNavigateBank {

    public final static GherkinKeyword keyword = GherkinKeyword.WHEN;

    public final static Pattern pattern = Pattern.compile("I navigate to SimBank");

    public final static Class<?>[] dependencies = {};

    public static void execute(SimBankManagerImpl manager) throws SimBankManagerException, DatastreamException,
            TimeoutException, KeyboardLockedException, NetworkException, FieldNotFoundException, TextNotFoundException,
            ConfigurationPropertyStoreException, TerminalInterruptedException {
        SimBankImpl simBank = manager.getSimBank();
        if(simBank == null) {
            throw new SimBankManagerException("Unable to get provisioned SimBank Instance");
        }

        simBank.start();
    }
}