package dev.galasa.simplatform.t3270.screens;

import java.util.logging.Level;
import java.util.logging.Logger;

import dev.galasa.zos3270.AttentionIdentification;
import dev.galasa.zos3270.internal.comms.NetworkServer;
import dev.galasa.zos3270.internal.datastream.CommandEraseWrite;
import dev.galasa.zos3270.internal.datastream.WriteControlCharacter;
import dev.galasa.zos3270.spi.Field;
import dev.galasa.zos3270.spi.Screen;

public class CICSClearScreen extends AbstractScreen {
	
	private static final Logger logger = Logger.getLogger("Simplatform");

	private final Screen screen;

	public CICSClearScreen(NetworkServer network) throws ScreenException {
		super(network);

		try {
			this.screen = buildScreen(getClass().getSimpleName());
		} catch (Exception e) {
			throw new ScreenException("Problem building screen", e);
		}
	}

	@Override
	public IScreen run() {

		try {
			while (true) {
				writeScreen();

				AttentionIdentification aid = receiveScreen(screen);

				if (aid == AttentionIdentification.ENTER) {
					Field transactionField = screen.getFieldAt(0, 0);
					String application = transactionField.getFieldWithoutNulls().trim().toUpperCase();

					if ("BANK".equals(application)) {
						return new BankMainMenu(network);
					}
				}
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Problem writing screen", e);
			return null;
		}
	}

	private void writeScreen() throws ScreenException {
		writeScreen(new CommandEraseWrite(),
				new WriteControlCharacter(false, false, false, false, false, false, true, true), screen);
	}

}
