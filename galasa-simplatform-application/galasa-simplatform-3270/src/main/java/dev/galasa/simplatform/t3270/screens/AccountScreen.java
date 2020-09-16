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
import dev.galasa.zos3270.AttentionIdentification;
import dev.galasa.zos3270.internal.comms.NetworkServer;
import dev.galasa.zos3270.internal.datastream.CommandEraseWrite;
import dev.galasa.zos3270.internal.datastream.WriteControlCharacter;
import dev.galasa.zos3270.spi.Field;
import dev.galasa.zos3270.spi.Screen;

public class AccountScreen extends AbstractScreen {

    private final Screen            screen;

    private final DateTimeFormatter dtf          = DateTimeFormatter.ofPattern("HH:mm:ss");
    private boolean                 foundAccount = false;
    private Account                 account;
    private String                  errorMessage = "";

    public AccountScreen(NetworkServer network) throws ScreenException {
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
                    Field accountField = screen.getFieldAt(19, 4);
                    String accountNumber = accountField.getFieldWithoutNulls().trim().toUpperCase();
                    this.foundAccount = new Bank().accountExists(accountNumber);
                    if (foundAccount) {
                        double balance = new Bank().getBalance(accountNumber);
                        String sortCode = new Bank().getSortCode(accountNumber);
                        this.account = new Account(accountNumber, sortCode, balance);
                        errorMessage = "Account Found";
                    } else if (!accountNumber.equals("_________")) {
                        errorMessage = "Account Not Found";
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

        screen.setBuffer(3, 8, "                 ");
        screen.setBuffer(3, 8, errorMessage);

        if (foundAccount) {
            screen.setBuffer(19, 4, account.getAccountNumber());
            screen.setBuffer(19, 5, account.getSortCode());
            screen.setBuffer(19, 6, account.getBalance().toPlainString());
        } else {
            screen.setBuffer(19, 5, "         ");
            screen.setBuffer(19, 6, "         ");
        }

        writeScreen(new CommandEraseWrite(),
                new WriteControlCharacter(false, false, false, false, false, false, true, true), screen);
    }

}
