package dev.galasa.simframe.t3270.screens;

import dev.galasa.common.zos3270.AttentionIdentification;
import dev.galasa.common.zos3270.internal.comms.NetworkServer;
import dev.galasa.common.zos3270.internal.datastream.CommandEraseWrite;
import dev.galasa.common.zos3270.internal.datastream.WriteControlCharacter;
import dev.galasa.common.zos3270.internal.terminal.fields.FieldText;
import dev.galasa.common.zos3270.spi.Screen;

public class CICSClearScreen extends AbstractScreen {

	private final Screen screen;

	public CICSClearScreen(NetworkServer network) throws ScreenException {
		super(network);

		try {
			this.screen = buildScreen(getClass().getSimpleName());

			makeTextField(screen, 1, 0);
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
					FieldText transactionField = (FieldText) screen.locateFieldsAt(calcPos(1, 0));
					String application = transactionField.getFieldWithoutNulls().trim().toUpperCase();

					if ("BANK".equals(application)) {
						return new BankMainMenu(network);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Problem writing screen");
			return null;
		}
	}

	private void writeScreen() throws ScreenException {
		writeScreen(new CommandEraseWrite(),
				new WriteControlCharacter(false, false, false, false, false, false, true, true), screen);
	}

}
