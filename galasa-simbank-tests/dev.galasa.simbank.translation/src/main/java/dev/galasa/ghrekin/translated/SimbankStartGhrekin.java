package dev.galasa.ghrekin.translated;

import dev.galasa.zos3270.spi.NetworkException;
import dev.galasa.zos3270.KeyboardLockedException;
import dev.galasa.zos3270.FieldNotFoundException;
import dev.galasa.core.manager.ICoreManager;
import dev.galasa.zos3270.ITerminal;
import dev.galasa.zos.ZosImage;
import dev.galasa.core.manager.CoreManager;
import dev.galasa.zos3270.TimeoutException;
import dev.galasa.zos3270.TerminalInterruptedException;
import dev.galasa.simbank.manager.ghrekin.CucumberSimbank;
import dev.galasa.Test;
import java.lang.String;
import dev.galasa.zos3270.Zos3270Terminal;
import dev.galasa.zos.IZosImage;
import dev.galasa.zos3270.TextNotFoundException;

@Test
public class SimbankStartGhrekin {
	//The Simbank is available
	@Zos3270Terminal(imageTag = "simbank")
	public ITerminal iterminal1;

	@ZosImage(imageTag = "simbank")
	public IZosImage iimage;

	@CoreManager
	public ICoreManager icoremanager;

	//Check if the SimBnk application is installed

	@Test
	public void simbankIsInstalled() throws FieldNotFoundException, TextNotFoundException, TerminalInterruptedException, KeyboardLockedException, NetworkException, TimeoutException {
		//I navigate to SimBank
		String string1 = CucumberSimbank.whenYouNavigateToBank(icoremanager, iterminal1);
		//I should see the main screen
		CucumberSimbank.thenTheMainScreenShouldBeVisible(string1);
	}
}