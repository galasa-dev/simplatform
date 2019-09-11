package dev.galasa.simframe.t3270.screens;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import dev.galasa.common.zos3270.AttentionIdentification;
import dev.galasa.common.zos3270.internal.comms.NetworkServer;
import dev.galasa.common.zos3270.internal.datastream.CommandEraseWrite;
import dev.galasa.common.zos3270.internal.datastream.WriteControlCharacter;
import dev.galasa.common.zos3270.spi.Screen;

public class CICSGoodMorning extends AbstractScreen {

	private final Screen screen;
	
	private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");

	public CICSGoodMorning(NetworkServer network) throws ScreenException {
		super(network);

		try {
			this.screen = buildScreen(getClass().getSimpleName());
		} catch(Exception e) {
			throw new ScreenException("Problem building screen", e);
		}
	}

	@Override
	public IScreen run() {

		try {
			while(true) {
				writeScreen();

				AttentionIdentification aid = receiveScreen(screen);
				
				if (aid == AttentionIdentification.CLEAR) {
					return new CICSClearScreen(network);
				}

			}
		} catch(Exception e) {
			e.printStackTrace();
			System.err.println("Problem writing screen");
			return null;
		}
	}

	private void writeScreen() throws ScreenException {
		LocalTime time = LocalTime.now();
		
		screen.setBuffer(37, 0, time.format(dtf));
		
		writeScreen(new CommandEraseWrite(), 
				new WriteControlCharacter(false, false, false, false, false, false, true, true),
				screen
				);
	}

}
