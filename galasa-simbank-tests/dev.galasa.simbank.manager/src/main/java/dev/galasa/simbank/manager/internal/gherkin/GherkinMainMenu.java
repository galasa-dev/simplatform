package dev.galasa.simbank.manager.internal.gherkin;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Pattern;

import dev.galasa.framework.spi.language.gherkin.GherkinKeyword;
import dev.galasa.simbank.manager.SimBankManagerException;
import dev.galasa.simbank.manager.internal.SimBankImpl;
import dev.galasa.simbank.manager.internal.SimBankManagerImpl;
import dev.galasa.simbank.manager.internal.SimBankTerminalImpl;

public class GherkinMainMenu {

    public final static GherkinKeyword keyword = GherkinKeyword.THEN;

    public final static Pattern pattern = Pattern.compile("I should see the main screen");

    public final static Class<?>[] dependencies = {};

    public static void execute(SimBankManagerImpl manager) throws SimBankManagerException {
        SimBankImpl simBank = manager.getSimBank();
        if(simBank == null) {
            throw new SimBankManagerException("Unable to get provisioned SimBank Instance");
        }

        SimBankTerminalImpl terminal = simBank.getControlTerminal();

        assertThat(terminal.retrieveScreen()).containsOnlyOnce("Options     Description        PFKey ");
        assertThat(terminal.retrieveScreen()).containsOnlyOnce("BROWSE      Browse Accounts    PF1");
        assertThat(terminal.retrieveScreen()).containsOnlyOnce("TRANSF      Transfer Money     PF4");
    }
}