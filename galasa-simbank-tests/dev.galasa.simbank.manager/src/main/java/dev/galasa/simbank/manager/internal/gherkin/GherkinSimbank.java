package dev.galasa.simbank.manager.internal.gherkin;

import java.util.regex.Pattern;

import org.apache.commons.logging.Log;

import dev.galasa.framework.spi.ConfigurationPropertyStoreException;
import dev.galasa.framework.spi.language.gherkin.GherkinKeyword;
import dev.galasa.simbank.manager.SimBankManagerException;
import dev.galasa.simbank.manager.internal.SimBankImpl;
import dev.galasa.simbank.manager.internal.SimBankManagerImpl;
import dev.galasa.simbank.manager.internal.properties.SimBankDseInstanceName;

public class GherkinSimbank {

    public final static GherkinKeyword keyword = GherkinKeyword.GIVEN;

    public final static Pattern pattern = Pattern.compile("The Simbank is available");

    public final static Class<?>[] dependencies = {};

    public static void execute(SimBankManagerImpl manager, Log logger) throws SimBankManagerException, ConfigurationPropertyStoreException {
        if(manager.getSimBank() == null) {
            String dseInstanceName = SimBankDseInstanceName.get();
            if (dseInstanceName == null) {
                throw new SimBankManagerException(
                        "The SimBank Manager does not support full provisioning at the moment, provide the DSE property sim.dse.instance.name to indicate the SimBank instance to run against");
            }

            SimBankImpl simBank = SimBankImpl.getDSE(manager, dseInstanceName, true, true);
            manager.setSimBankInstance(simBank);

            logger.info("SimBank instance " + simBank.getInstanceId() + " provisioned for this run");
        }
    }
}