package galasa.manager;

import dev.galasa.common.zos3270.FieldNotFoundException;
import dev.galasa.common.zos3270.ITerminal;
import dev.galasa.common.zos3270.KeyboardLockedException;
import dev.galasa.common.zos3270.TextNotFoundException;
import dev.galasa.common.zos3270.TimeoutException;
import dev.galasa.common.zos3270.spi.DatastreamException;
import dev.galasa.common.zos3270.spi.NetworkException;
import dev.galasa.framework.spi.ConfigurationPropertyStoreException;

public interface ISimBankTerminal extends ITerminal {

	void gotoMainMenu() throws TimeoutException, KeyboardLockedException, DatastreamException, NetworkException,
			FieldNotFoundException, TextNotFoundException, ConfigurationPropertyStoreException, SimBankManagerException;

}
