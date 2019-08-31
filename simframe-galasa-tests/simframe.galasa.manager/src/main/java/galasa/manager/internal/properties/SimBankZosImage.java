package galasa.manager.internal.properties;

import javax.validation.constraints.NotNull;

import dev.galasa.framework.spi.ConfigurationPropertyStoreException;
import dev.galasa.framework.spi.cps.CpsProperties;
import galasa.manager.SimBankManagerException;

/**
 * SimBank Zos Image
 * <p>
 * Get the zos image this instance of SimBank is running on
 * </p><p>
 * The property is:-<br><br>
 * simbank.instance.[instance].zos.image
 * </p>
 * <p>
 * Default = simframe
 * </p>
 * 
 * @author Michael Baylis
 *
 */
public class SimBankZosImage extends CpsProperties {
	
	public static String get(@NotNull String instance) throws ConfigurationPropertyStoreException, SimBankManagerException {
		return getStringWithDefault(SimBankPropertiesSingleton.cps(),
				               "SIMFRAME",
				               "instance", 
				               "zos.image",
				               instance);
	}

}
