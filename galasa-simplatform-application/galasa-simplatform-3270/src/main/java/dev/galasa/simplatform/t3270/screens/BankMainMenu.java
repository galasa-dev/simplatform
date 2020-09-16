/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.simplatform.t3270.screens;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import dev.galasa.zos3270.AttentionIdentification;
import dev.galasa.zos3270.internal.comms.NetworkServer;
import dev.galasa.zos3270.internal.datastream.CommandEraseWrite;
import dev.galasa.zos3270.internal.datastream.WriteControlCharacter;
import dev.galasa.zos3270.spi.Screen;

public class BankMainMenu extends AbstractScreen {

    private final Screen            screen;

    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");

    public BankMainMenu(NetworkServer network) throws ScreenException {
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
                if (aid == AttentionIdentification.PF1) {
                    return new AccountScreen(network);
                }
                if (aid == AttentionIdentification.PF3) {
                    return new SessionManagerMenu(network);
                }
                if (aid == AttentionIdentification.PF4) {
                    return new TransferScreen(network);
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

        writeScreen(new CommandEraseWrite(),
                new WriteControlCharacter(false, false, false, false, false, false, true, true), screen);
    }

}
