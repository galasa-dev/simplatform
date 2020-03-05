/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.simbank.manager.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dev.galasa.ICredentialsUsernamePassword;
import dev.galasa.framework.spi.ConfigurationPropertyStoreException;
import dev.galasa.framework.spi.IFramework;
import dev.galasa.simbank.manager.ISimBankTerminal;
import dev.galasa.simbank.manager.SimBankManagerException;
import dev.galasa.simbank.manager.internal.properties.SimBankApplicationName;
import dev.galasa.zos3270.FieldNotFoundException;
import dev.galasa.zos3270.KeyboardLockedException;
import dev.galasa.zos3270.TextNotFoundException;
import dev.galasa.zos3270.TimeoutException;
import dev.galasa.zos3270.Zos3270ManagerException;
import dev.galasa.zos3270.spi.DatastreamException;
import dev.galasa.zos3270.spi.NetworkException;
import dev.galasa.zos3270.spi.Zos3270TerminalImpl;

public class SimBankTerminalImpl extends Zos3270TerminalImpl implements ISimBankTerminal {

    private Log                                logger = LogFactory.getLog(SimBankTerminalImpl.class);

    private final ICredentialsUsernamePassword credentials;
    private final String                       application;

    public SimBankTerminalImpl(String id, String host, String application, ICredentialsUsernamePassword credentials,
            int port, boolean tls, IFramework framework) throws Zos3270ManagerException, InterruptedException {
        super(id, host, port, tls, framework);
        this.credentials = credentials;
        this.application = application;
    }

    @Override
    public void gotoMainMenu() throws TimeoutException, KeyboardLockedException, DatastreamException, NetworkException,
            FieldNotFoundException, TextNotFoundException, ConfigurationPropertyStoreException, SimBankManagerException,
            InterruptedException {

        waitForKeyboard();

        while (!isTextInField("SIMBANK MAIN MENU")) {
            if (isTextInField("SIMBANK ACCOUNT MENU")) {
                pf3().waitForKeyboard();
            } else if (isTextInField("SIMBANK TRANSFER MENU")) {
                pf3().waitForKeyboard();
            } else if (isTextInField("DFHZC2312")) {
                enterCICSTransaction();
            } else if (isTextInField("SIMPLATFORM MAIN MENU")) {
                selectionApplication();
            } else if (isTextInField("SIMPLATFORM LOGON SCREEN")) {
                logonSessionManager();
            } else {
                logger.warn("Unable to determine position in application, reseting connection");
                disconnect();
                connect();
            }
        }
    }

    private void logonSessionManager() throws DatastreamException, TimeoutException, KeyboardLockedException,
            NetworkException, FieldNotFoundException, TextNotFoundException, ConfigurationPropertyStoreException,
            SimBankManagerException, InterruptedException {
        verifyTextInField(("SIMPLATFORM LOGON SCREEN")).positionCursorToFieldContaining("Userid").tab()
                .type(credentials.getUsername()).positionCursorToFieldContaining("Password").tab()
                .type(credentials.getPassword()).enter().waitForKeyboard();

        selectionApplication();
    }

    private void selectionApplication() throws DatastreamException, TimeoutException, KeyboardLockedException,
            NetworkException, FieldNotFoundException, TextNotFoundException, ConfigurationPropertyStoreException,
            SimBankManagerException, InterruptedException {
        verifyTextInField("SIMPLATFORM MAIN MENU").positionCursorToFieldContaining("===>").tab()
                .type(SimBankApplicationName.get(this.application)).enter().waitForKeyboard();

        enterCICSTransaction();
    }

    private void enterCICSTransaction() throws TimeoutException, KeyboardLockedException, NetworkException,
            TextNotFoundException, FieldNotFoundException, InterruptedException {
        verifyTextInField("DFHZC2312").clear().waitForKeyboard().type("bank").enter().waitForKeyboard();
    }

}
