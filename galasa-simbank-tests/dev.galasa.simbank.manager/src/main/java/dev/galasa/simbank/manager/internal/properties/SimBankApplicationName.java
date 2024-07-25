/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.simbank.manager.internal.properties;

import javax.validation.constraints.NotNull;

import dev.galasa.framework.spi.ConfigurationPropertyStoreException;
import dev.galasa.framework.spi.cps.CpsProperties;
import dev.galasa.simbank.manager.SimBankManagerException;

/**
 * SimBank Application Name for the Session Manager logon
 * <p>
 * The property is:-<br>
 * <br>
 * simbank.instance.[instance].application.name
 * </p>
 * <p>
 * Default = BANKTEST
 * </p>
 * 
 *  
 *
 */
public class SimBankApplicationName extends CpsProperties {

    public static String get(@NotNull String instance)
            throws ConfigurationPropertyStoreException, SimBankManagerException {
        return getStringWithDefault(SimBankPropertiesSingleton.cps(), "BANKTEST", "instance", "application.name",
                instance);
    }

}
