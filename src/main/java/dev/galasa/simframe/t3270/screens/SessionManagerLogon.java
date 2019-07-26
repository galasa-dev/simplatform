package dev.galasa.simframe.t3270.screens;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import dev.voras.common.zos3270.AttentionIdentification;
import dev.voras.common.zos3270.internal.comms.NetworkServer;
import dev.voras.common.zos3270.internal.datastream.CommandEraseWrite;
import dev.voras.common.zos3270.internal.datastream.WriteControlCharacter;
import dev.voras.common.zos3270.internal.terminal.fields.FieldText;
import dev.voras.common.zos3270.spi.Screen;

public class SessionManagerLogon extends AbstractScreen {

	private final Screen screen;
	
	private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");

	public SessionManagerLogon(NetworkServer network) throws ScreenException {
		super(network);

		try {
			this.screen = buildScreen(getClass().getSimpleName());
			
			makeTextField(screen, 1,0);
			makeTextField(screen, 72,0);
			makeTextField(screen, 13,21);
			makeTextField(screen, 36,21);
			makeTextField(screen, 1,23);
			
			FieldText newTerminalField = (FieldText) screen.locateFieldsAt(calcPos(1, 0));
			newTerminalField.setContents(network.getDeviceName());
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

				if (aid != AttentionIdentification.ENTER) {
					continue;
				}

				FieldText useridField   = (FieldText) screen.locateFieldsAt(calcPos(13, 21));
				FieldText passwordField = (FieldText) screen.locateFieldsAt(calcPos(36, 21));

				String userid = useridField.getFieldWithoutNulls().trim().toUpperCase();
				String password = passwordField.getFieldWithoutNulls().trim().toUpperCase();

				if ("BOO".equals(userid) && "EEK".equals(password)) {
					return new SessionManagerMenu(network);
				}

				FieldText errorMessage = (FieldText) screen.locateFieldsAt(calcPos(1, 23));
				errorMessage.nullify();
				errorMessage.setContents("Invalid userid or password");
			}
		} catch(Exception e) {
			e.printStackTrace();
			System.err.println("Problem writing screen");
			return null;
		}
	}

	private void writeScreen() throws ScreenException {
		LocalTime time = LocalTime.now();
		
		FieldText timeField = (FieldText) screen.locateFieldsAt(calcPos(72, 0));
		timeField.setContents(time.format(dtf));
		
		writeScreen(new CommandEraseWrite(), 
				new WriteControlCharacter(false, false, false, false, false, false, true, true),
				screen
				);
	}

}
