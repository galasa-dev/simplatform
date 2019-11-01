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
import dev.galasa.zos3270.spi.Field;
import dev.galasa.zos3270.spi.Screen;

public class SessionManagerMenu extends AbstractScreen {

    private final Screen            screen;

    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");

    public SessionManagerMenu(NetworkServer network) throws ScreenException {
        super(network);

        try {
            this.screen = buildScreen(getClass().getSimpleName());

            this.screen.setBuffer(1, 0, network.getDeviceName());
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
                    return new SessionManagerLogon(network);
                }

                if (aid == AttentionIdentification.PF1) {
                    return new CICSGoodMorning(network);
                }

                if (aid == AttentionIdentification.ENTER) {
                    Field applicationField = screen.getFieldAt(28, 21);
                    String application = applicationField.getFieldWithoutNulls().trim().toUpperCase();

                    if ("BANKTEST".equals(application)) {
                        return new CICSGoodMorning(network);
                    }
                }

                screen.nullify(1, 23, 79);
                screen.setBuffer(1, 23, "Invalid Application");
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
