package galasa.manager.internal.properties;

import javax.validation.constraints.NotNull;

import dev.galasa.framework.spi.ConfigurationPropertyStoreException;
import dev.galasa.framework.spi.cps.CpsProperties;
import galasa.manager.SimBankManagerException;

/**
 * SimBank Credentials ID for logging on with
 * <p>
 * The property is:-<br><br>
 * simbank.instance.[instance].credentials.id
 * </p>
 * <p>
 * Default = SIMFRAME
 * </p>
 * 
 * @author Michael Baylis
 *
 */
public class SimBankCredentials extends CpsProperties {
	
	public static String get(@NotNull String instance) throws ConfigurationPropertyStoreException, SimBankManagerException {
		return getStringWithDefault(SimBankPropertiesSingleton.cps(),
				               "SIMFRAME",
				               "instance", 
				               "credentials.id",
				               instance);
	}

}
