/*
 * Copyright contributors to the Galasa project
 */
package dev.galasa.simbank.manager.internal.properties;

import dev.galasa.framework.spi.ConfigurationPropertyStoreException;
import dev.galasa.framework.spi.cps.CpsProperties;
import dev.galasa.simbank.manager.SimBankManagerException;

/**
 * SimBank DSE Instance
 * <p>
 * The DSE SimBank instance name
 * </p>
 * <p>
 * The property is:-<br>
 * <br>
 * sim.dse.instance.name
 * </p>
 * <p>
 * There is no default as the manager would be expected to provision an instance
 * </p>
 * 
 * @author Michael Baylis
 *
 */
public class SimBankDseInstanceName extends CpsProperties {

    public static String get() throws ConfigurationPropertyStoreException, SimBankManagerException {
        return getStringNulled(SimBankPropertiesSingleton.cps(), "dse", "instance.name");
    }

}
