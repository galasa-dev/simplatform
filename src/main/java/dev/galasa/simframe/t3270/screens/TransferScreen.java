package dev.galasa.simframe.t3270.screens;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import dev.galasa.simframe.application.Bank;
import dev.galasa.simframe.data.Account;
import dev.galasa.simframe.exceptions.InsufficientBalanceException;
import dev.voras.common.zos3270.internal.comms.NetworkServer;
import dev.voras.common.zos3270.AttentionIdentification;
import dev.voras.common.zos3270.internal.datastream.CommandEraseWrite;
import dev.voras.common.zos3270.internal.datastream.WriteControlCharacter;
import dev.voras.common.zos3270.internal.terminal.fields.FieldText;
import dev.voras.common.zos3270.spi.Screen;

public class TransferScreen extends AbstractScreen {

	private final Screen screen;

	private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
	private boolean validFields = false;
	private Account account1;
	private Account account2;
	private String errorMessage = "";

	public TransferScreen(NetworkServer network) throws ScreenException {
		super(network);

		try {
			this.screen = buildScreen(getClass().getSimpleName());

			makeTextField(screen, 33,4);
			makeTextField(screen, 33,5);
			makeTextField(screen, 33,6);
			makeTextField(screen, 3,8);
			
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
					return new BankMainMenu(network);
				}
				
				if(aid == AttentionIdentification.ENTER) {
					FieldText accountField1 = (FieldText) screen.locateFieldsAt(calcPos(33,4));
					FieldText accountField2 = (FieldText) screen.locateFieldsAt(calcPos(33,5));
					FieldText transferField = (FieldText) screen.locateFieldsAt(calcPos(33,6));
					String accountNumber1 = accountField1.getFieldWithoutNulls().trim().toUpperCase();
					String accountNumber2 = accountField2.getFieldWithoutNulls().trim().toUpperCase();
					String transferAmount = transferField.getFieldWithoutNulls().trim();
					this.validFields = Bank.getBank().accountExists(accountNumber1) && Bank.getBank().accountExists(accountNumber2)
							&& isNumeric(transferAmount);
					if(validFields) {
						this.account1 = new Account(accountNumber1, null, Bank.getBank().getBalance(accountNumber1));
						this.account2 = new Account(accountNumber2, null, Bank.getBank().getBalance(accountNumber2));
						try {
							Bank.getBank().transferMoney(accountNumber1, accountNumber2, Double.parseDouble(transferAmount));
							errorMessage = "Transfer Successful";
						} catch (InsufficientBalanceException e) {
							errorMessage = "Transfer failed: Insufficient funds";
						}
					} else if(!accountNumber1.equals("_________") && !Bank.getBank().accountExists(accountNumber1)) {
						errorMessage = "Account 1 does not exist";
					} else if(!accountNumber2.equals("_________") && !Bank.getBank().accountExists(accountNumber2)) {
						errorMessage = "Account 2 does not exist";
					} else if(!transferAmount.equals("") && !isNumeric(transferAmount)) {
						errorMessage = "Please enter a valid transfer value";
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
		
		FieldText errorField = (FieldText) screen.locateFieldsAt(calcPos(3, 8));
		errorField.setContents("                                   ");
		errorField.setContents(errorMessage);
		
		if(validFields) {
			FieldText account1Field = (FieldText) screen.locateFieldsAt(calcPos(33, 4));
			account1Field.setContents(account1.getAccountNumber());
			
			FieldText account2Field = (FieldText) screen.locateFieldsAt(calcPos(33, 5));
			account2Field.setContents(account2.getAccountNumber());
		}

		writeScreen(new CommandEraseWrite(), 
				new WriteControlCharacter(false, false, false, false, false, false, true, true),
				screen
				);
	}
	
	private Boolean isNumeric(String str) {
		try {
			double d = Double.parseDouble(str);
		} catch(NumberFormatException | NullPointerException e) {
			return false;
		}
		return true;
	}

}
