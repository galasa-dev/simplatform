/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.simbank.manager.internal;

import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dev.galasa.ICredentials;
import dev.galasa.ICredentialsUsernamePassword;
import dev.galasa.simbank.manager.ISimBank;
import dev.galasa.simbank.manager.SimBankManagerException;
import dev.galasa.simbank.manager.internal.properties.SimBankApplicationName;
import dev.galasa.simbank.manager.internal.properties.SimBankCredentials;
import dev.galasa.simbank.manager.internal.properties.SimBankDatabasePort;
import dev.galasa.simbank.manager.internal.properties.SimBankWebNetPort;
import dev.galasa.simbank.manager.internal.properties.SimBankZosImage;
import dev.galasa.zos.IZosImage;

public class SimBankImpl implements ISimBank {

    private final Log                          logger        = LogFactory.getLog(SimBankImpl.class);

    private final SimBankManagerImpl           manager;
    private final String                       instanceId;
    private final String                       host;
    private final int                          webnetPort;
    private final int                          telnetPort;
    private final boolean                      telnetSecure;
    private final int                          databasePort;
    private final String                       updateAddress = "updateAccount";
    private final ICredentialsUsernamePassword credentials;
    private final boolean                      useTerminal;
    private final boolean                      useJdbc;
    private URI                                jdbcUri;

    private SimBankTerminalImpl                controlTerminal;
    private Connection                         controlJdbc;

    private SimBankImpl(SimBankManagerImpl manager, String instanceId, String hostname, int telnetPort,
            boolean telnetSecure, int databasePort, int webnetPort, String applicationName,
            ICredentialsUsernamePassword credentials, boolean useTerminal, boolean useJdbc) {
        this.instanceId = instanceId;
        this.manager = manager;
        this.host = hostname;
        this.webnetPort = webnetPort;
        this.telnetPort = telnetPort;
        this.telnetSecure = telnetSecure;
        this.databasePort = databasePort;
        this.credentials = credentials;
        this.useTerminal = useTerminal;
        this.useJdbc = useJdbc;

        this.manager.getFramework().getConfidentialTextService().registerText(credentials.getPassword(),
                credentials.getUsername() + " password");
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public int getWebnetPort() {
        return webnetPort;
    }

    @Override
    public String getFullAddress() {
        return "http://" + host + ":" + webnetPort;
    }

    @Override
    public String getUpdateAddress() {
        return updateAddress;
    }

    public static SimBankImpl getDSE(SimBankManagerImpl manager, String dseInstanceName, boolean useTerminal,
            boolean useJdbc) throws SimBankManagerException {

        try {
            String zosImageId = SimBankZosImage.get(dseInstanceName);

            IZosImage zosImage = manager.getZosManager().getUnmanagedImage(zosImageId);
            String hostname = zosImage.getDefaultHostname();
            int telnetPort = zosImage.getIpHost().getTelnetPort();
            boolean telnetSecure = zosImage.getIpHost().isTelnetPortTls();

            int webnetPort = SimBankWebNetPort.get(dseInstanceName);
            int databasePort = SimBankDatabasePort.get(dseInstanceName);

            String applicationName = SimBankApplicationName.get(dseInstanceName);
            String credentialsId = SimBankCredentials.get(dseInstanceName);

            ICredentials credentials = manager.getFramework().getCredentialsService().getCredentials(credentialsId);
            if (credentials == null) {
                throw new SimBankManagerException(
                        "Missing credentials for id " + credentialsId + " for SimBank instance " + dseInstanceName);
            }
            if (!(credentials instanceof ICredentialsUsernamePassword)) {
                throw new SimBankManagerException("Invalidcredentials for id " + credentialsId
                        + " for SimBank instance " + dseInstanceName + ", needs to Username and Password");
            }

            SimBankImpl simBank = new SimBankImpl(manager, dseInstanceName, hostname, telnetPort, telnetSecure,
                    databasePort, webnetPort, applicationName, (ICredentialsUsernamePassword) credentials, useTerminal,
                    useJdbc);

            if (useJdbc) {
                simBank.connectJdbc();
            }

            return simBank;
        } catch (Exception e) {
            throw new SimBankManagerException("Unable to create the SimBank DSE instance", e);
        }
    }

    public String getInstanceId() {
        return this.instanceId;
    }

    public void start() throws SimBankManagerException {
        // *** Connect a control terminal and make sure the application is up
        if (useTerminal) {
            this.controlTerminal = connectTerminal("simbank-ctrl");
            logger.info("Connected to SimBank Terminal");
        }

    }

    private void connectJdbc() throws SimBankManagerException {
        try {
            Class<?> load = org.apache.derby.jdbc.ClientDriver.class;
            load.newInstance();

            jdbcUri = new URI(
                    "jdbc:derby://" + host + ":" + Integer.toString(databasePort) + "/galasaBankDB;create=false");
            controlJdbc = DriverManager.getConnection(jdbcUri.toString(), new Properties());
        } catch (Exception e) {
            throw new SimBankManagerException("Unable to connect to SimBank database", e);
        }

    }

    public void discard() throws InterruptedException {
        if (this.controlTerminal != null) {
            this.controlTerminal.disconnect();
            this.controlTerminal = null;
        }

        if (this.controlJdbc != null) {
            try {
                this.controlJdbc.close();
            } catch (SQLException e) {
            }
            this.controlJdbc = null;
        }
    }

    private SimBankTerminalImpl connectTerminal(String id) throws SimBankManagerException {
        try {
            SimBankTerminalImpl terminal = new SimBankTerminalImpl(id, host, instanceId, credentials, telnetPort,
                    telnetSecure, manager.getFramework());
            manager.registerTerminal(terminal);
            terminal.connect();

            terminal.waitForKeyboard();

            terminal.gotoMainMenu();

            return terminal;
        } catch (SimBankManagerException e) {
            throw e;
        } catch (Exception e) {
            throw new SimBankManagerException("Unable to connect terminal", e);
        }

    }

    public boolean isUseJdbc() {
        return this.useJdbc;
    }

    public Connection getJdbc() {
        return this.controlJdbc;
    }

    public SimBankTerminalImpl getControlTerminal() {
        return this.controlTerminal;
    }

    public SimBankManagerImpl getManager() {
        return this.manager;
    }

    public URI getJdbcUri() {
        return this.jdbcUri;
    }

    public boolean isTerminal() {
        return this.useTerminal;
    }

    public SimBankTerminalImpl allocateTerminal(int terminalNumber) throws SimBankManagerException {

        try {
            String id = "simbank-" + terminalNumber;

            SimBankTerminalImpl terminal = new SimBankTerminalImpl(id, host, instanceId, credentials, telnetPort,
                    telnetSecure, manager.getFramework());
            manager.registerTerminal(terminal);
            terminal.connect();

            terminal.waitForKeyboard();

            terminal.gotoMainMenu();
            return terminal;
        } catch (Exception e) {
            throw new SimBankManagerException("Unable to provision a new SimBank terminal", e);
        }
    }
}
