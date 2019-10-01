package dev.galasa.simbank.manager.internal.properties;

import javax.validation.constraints.NotNull;

import dev.galasa.framework.spi.ConfigurationPropertyStoreException;
import dev.galasa.framework.spi.cps.CpsProperties;
import dev.galasa.simbank.manager.SimBankManagerException;

/**
 * SimBank Database Port
 * <p>
 * The port the SimBank Database is listening on
 * </p><p>
 * The property is:-<br><br>
 * simbank.instance.[instance].database.port
 * </p>
 * <p>
 * Default = 2027
 * </p>
 * 
 * @author Michael Baylis
 *
 */
public class SimBankDatabasePort extends CpsProperties {
	
	public static int get(@NotNull String instance) throws ConfigurationPropertyStoreException, SimBankManagerException {
		return Integer.parseInt(getStringWithDefault(SimBankPropertiesSingleton.cps(),
				               "2027",
				               "instance", 
				               "database.port",
				               instance));
	}

}
