/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.simplatform.t3270.screens;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    public Screen        screen;
    
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");

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
        
        String message = "";

        try {
            while (true) {
                writeScreen(message);

                AttentionIdentification aid = receiveScreen(screen);
                
                Field transactionField = screen.getFieldAt(0, 0);
                String text = transactionField.getFieldWithoutNulls().trim().toUpperCase();
                
                int length = text.length();
                if (length > 4) {
                    length = 4;
                }
                String transaction = text.substring(0, length).trim();

                if (aid == AttentionIdentification.ENTER) {
                    if ("BANK".equals(transaction)) {
                        return new BankMainMenu(network);
                    } else {
                        message = "DFHAC2001 " + getDate() + " SIMBANK Transaction '" + transaction + "' is not recognized. Check  that the transaction name is correct.";
                    }
                } else if (aid == AttentionIdentification.CLEAR) {
                    this.screen = buildScreen(getClass().getSimpleName());
                    message = "";
                } else if (aid == AttentionIdentification.PF3) {
                    return new SessionManagerMenu(network);
                } else {
                    message = "DFHAC2001 " + getDate() + " SIMBANK Transaction '" + aid.toString() + "' is not recognized. Check  that the transaction name is correct.";
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Problem writing screen", e);
            return null;
        }
    }

    private void writeScreen(String errorMessage) throws ScreenException {
        
        while(errorMessage.length() < 158) {
            errorMessage += " ";
        }
        
        screen.setBuffer(1, 22, errorMessage);
         
        writeScreen(new CommandEraseWrite(),
                new WriteControlCharacter(false, false, false, false, false, false, true, true), screen);
    }
    
    private String getDate() {
        LocalDateTime time = LocalDateTime.now();
        
        return dtf.format(time);
    }

}
