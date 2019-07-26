package dev.galasa.simframe.t3270.screens;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import dev.galasa.simframe.application.Bank;
import dev.galasa.simframe.data.Account;
import dev.voras.common.zos3270.internal.comms.NetworkServer;
import dev.voras.common.zos3270.AttentionIdentification;
import dev.voras.common.zos3270.internal.datastream.CommandEraseWrite;
import dev.voras.common.zos3270.internal.datastream.WriteControlCharacter;
import dev.voras.common.zos3270.internal.terminal.fields.FieldText;
import dev.voras.common.zos3270.spi.Screen;

public class AccountScreen extends AbstractScreen {

	private final Screen screen;

	private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
	private boolean foundAccount = false;
	private Account account;

	public AccountScreen(NetworkServer network) throws ScreenException {
		super(network);

		try {
			this.screen = buildScreen(getClass().getSimpleName());

			makeTextField(screen, 19,4);
			
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

				if (aid == AttentionIdentification.PF3) {
					return new SessionManagerMenu(network);
				}
				
				if (aid == AttentionIdentification.PF1) {
					return new TransferScreen(network);
				}
				
				if(aid == AttentionIdentification.ENTER) {
					FieldText accountField   = (FieldText) screen.locateFieldsAt(calcPos(19,4));
					String accountNumber = accountField.getFieldWithoutNulls().trim().toUpperCase();
					this.foundAccount = Bank.getBank().accountExists(accountNumber);
					if(foundAccount) {
						double balance = Bank.getBank().getBalance(accountNumber);
						String sortCode = Bank.getBank().getSortCode(accountNumber);
						this.account = new Account(accountNumber, sortCode, balance);
					}
					
					
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

		FieldText timeField = (FieldText) screen.locateFieldsAt(calcPos(72, 0));
		timeField.setContents(time.format(dtf));
		
		if(foundAccount) {
			FieldText accountField = (FieldText) screen.locateFieldsAt(calcPos(19, 4));
			accountField.setContents(account.getAccountNumber());
			
			FieldText sortCodeField = (FieldText) screen.locateFieldsAt(calcPos(19, 5));
			sortCodeField.setContents(account.getSortCode());
			
			FieldText balanceField = (FieldText) screen.locateFieldsAt(calcPos(19, 6));
			balanceField.setContents(Double.toString(account.getBalance()));
		}

		writeScreen(new CommandEraseWrite(), 
				new WriteControlCharacter(false, false, false, false, false, false, true, true),
				screen
				);
	}

}
