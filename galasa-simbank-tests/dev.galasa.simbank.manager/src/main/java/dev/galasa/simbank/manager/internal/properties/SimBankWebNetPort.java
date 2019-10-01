package dev.galasa.simbank.manager.internal.properties;

import javax.validation.constraints.NotNull;

import dev.galasa.framework.spi.ConfigurationPropertyStoreException;
import dev.galasa.framework.spi.cps.CpsProperties;
import dev.galasa.simbank.manager.SimBankManagerException;

/**
 * SimBank Web Net Port
 * <p>
 * The port the SimBank Webservice is listening on
 * </p><p>
 * The property is:-<br><br>
 * simbank.instance.[instance].webnet.port
 * </p>
 * <p>
 * Default = 2080
 * </p>
 * 
 * @author Michael Baylis
 *
 */
public class SimBankWebNetPort extends CpsProperties {
	
	public static int get(@NotNull String instance) throws ConfigurationPropertyStoreException, SimBankManagerException {
		return Integer.parseInt(getStringWithDefault(SimBankPropertiesSingleton.cps(),
				               "2080",
				               "instance", 
				               "webnet.port",
				               instance));
	}

}
