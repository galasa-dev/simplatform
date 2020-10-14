/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.simplatform.t3270.screens;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import dev.galasa.simplatform.application.Bank;
import dev.galasa.simplatform.data.Account;
import dev.galasa.simplatform.exceptions.InsufficientBalanceException;
import dev.galasa.zos3270.AttentionIdentification;
import dev.galasa.zos3270.internal.comms.NetworkServer;
import dev.galasa.zos3270.internal.datastream.CommandEraseWrite;
import dev.galasa.zos3270.internal.datastream.WriteControlCharacter;
import dev.galasa.zos3270.spi.Field;
import dev.galasa.zos3270.spi.Screen;

public class TransferScreen extends AbstractScreen {

    private final Screen            screen;

    private final DateTimeFormatter dtf          = DateTimeFormatter.ofPattern("HH:mm:ss");
    private boolean                 validFields  = false;
    private Account                 account1;
    private Account                 account2;
    private String                  errorMessage = "";

    public TransferScreen(NetworkServer network) throws ScreenException {
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

                if (aid == AttentionIdentification.PF3) {
                    return new BankMainMenu(network);
                }

                if (aid == AttentionIdentification.ENTER) {
                    Field accountField1 = screen.getFieldAt(33, 4);
                    Field accountField2 = screen.getFieldAt(33, 5);
                    Field transferField = screen.getFieldAt(33, 6);
                    String accountNumber1 = accountField1.getFieldWithoutNulls().trim().toUpperCase();
                    String accountNumber2 = accountField2.getFieldWithoutNulls().trim().toUpperCase();
                    String transferAmount = transferField.getFieldWithoutNulls().trim();
                    this.validFields = new Bank().accountExists(accountNumber1)
                            && new Bank().accountExists(accountNumber2) && isNumeric(transferAmount);
                    if (validFields) {
                        this.account1 = new Account(accountNumber1, null, new Bank().getBalance(accountNumber1));
                        this.account2 = new Account(accountNumber2, null, new Bank().getBalance(accountNumber2));
                        try {
                            new Bank().transferMoney(accountNumber1, accountNumber2,
                                    Double.parseDouble(transferAmount));
                            errorMessage = "Transfer Successful";
                        } catch (InsufficientBalanceException e) {
                            errorMessage = "Transfer failed: Insufficient funds";
                        }
                    } else if (!accountNumber1.equals("_________") && !new Bank().accountExists(accountNumber1)) {
                        errorMessage = "Account 1 does not exist";
                    } else if (!accountNumber2.equals("_________") && !new Bank().accountExists(accountNumber2)) {
                        errorMessage = "Account 2 does not exist";
                    } else if (!transferAmount.equals("") && !isNumeric(transferAmount)) {
                        errorMessage = "Please enter a valid transfer value";
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
        LocalTime time = LocalTime.now();

        screen.setBuffer(72, 0, time.format(dtf));

        screen.setBuffer(3, 8, "                                   ");
        screen.setBuffer(3, 8, errorMessage);

        if (validFields) {
            screen.setBuffer(33, 4, account1.getAccountNumber());
            screen.setBuffer(33, 5, account2.getAccountNumber());
        }

        writeScreen(new CommandEraseWrite(),
                new WriteControlCharacter(false, false, false, false, false, false, true, true), screen);
    }

    private Boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
        } catch (NumberFormatException | NullPointerException e) {
            return false;
        }
        return true;
    }

}
