/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.simplatform.db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Database {

    private static String       driver        = "org.apache.derby.jdbc.EmbeddedDriver";
    private static String       dbName        = "galasaBankDB";
    private static String       connectionURL = "jdbc:derby:" + dbName + ";create=true";

    private static final String TABLE_EXISTS  = "X0Y32";

    private Connection          conn          = null;

    private Logger              log;
	private String exceptionMessage;

    public Database() {
        log = Logger.getLogger("Simplatform");
        setDerbyHome();
        try {
            conn = DriverManager.getConnection(connectionURL);
            conn.setAutoCommit(true);
            createTable();
        } catch (SQLException e) {
            log.severe("Unable to connect to embedded DB - exit");
            System.exit(1);
        }
    }

    /**
     * If running in eclipse, on windows under a account that is non admin, derby
     * will by default try to write to c:\windows\System 32\ which could cause
     * permission problems, so this function changes the derby home to be a temp dir
     * that should work across platforms
     */
    private void setDerbyHome() {
        try {
            Path tempDir = Files.createTempDirectory("galasaSimplatform");
            log.info("Setting Derby home to " + tempDir.toString());
            System.setProperty("derby.system.home", tempDir.toString());
        } catch (IOException e) {
            log.warning("Unable to calculate temp dir, will use default");
        }

    }

    private void createTable() {
        try {
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE ACCOUNTS  " + "(ACCOUNT_NUM VARCHAR(9) NOT NULL,"
                    + " SORT_CODE VARCHAR(8) NOT NULL," + " BALANCE DECIMAL(10,2)) ");
        } catch (SQLException e) {
            if (e.getSQLState().equals(TABLE_EXISTS)) {
                dropTable();
                createTable();
            }

        }
    }

    private void dropTable() {
        try {
            Statement stmt = conn.createStatement();
            stmt.execute("DROP TABLE ACCOUNTS");
        } catch (SQLException e) {
            if (e.getSQLState().equals(TABLE_EXISTS)) {
                return;
            }

        }
    }

    public boolean execute(String sql) {
    	boolean sucess = true;
    	exceptionMessage = null;
        try {
            Statement stmt = conn.createStatement();
            stmt.execute(sql);
        } catch (SQLException se) {
        	logException(sql, se);
        	sucess = false;
        }
        return sucess;
    }

    public ResultSet getExecutionResults(String sql) {
        try {
            Statement stmt = conn.createStatement();
            return stmt.executeQuery(sql);
        } catch (SQLException se) {
            return null;
        }
    }

	public String getExceptionMessage() {
		return exceptionMessage;
	}

	private void logException(String sql, SQLException e) {
    	exceptionMessage = "SQLCODE=" + e.getErrorCode() + "; SQLSTATE=" + e.getSQLState() + "; " + e.getMessage();
    	log.log(Level.SEVERE, exceptionMessage, e);
	}

}
