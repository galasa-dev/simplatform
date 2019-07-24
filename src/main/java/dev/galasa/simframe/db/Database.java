package dev.galasa.simframe.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {

	private static String driver = "org.apache.derby.jdbc.EmbeddedDriver";
	private static String dbName = "galasaBankDB";
	private static String connectionURL = "jdbc:derby:" + dbName + ";create=true";

	private static final String TABLE_EXISTS = "X0Y32";
	
	private static Database database = null;
	
	private Connection conn = null;
	

	private Database() {
		try {
		    conn = DriverManager.getConnection(connectionURL);
		    createTable();
		}catch (SQLException e) {
			// TODO: handle exception
		}      
	}
	
	public static Database getDatabase() {
		if(Database.database == null) {
			database = new Database();
		}
		return database;
	}
	
	

	private void createTable() {
		try {
			Statement stmt = conn.createStatement();
			stmt.execute("CREATE TABLE ACCOUNTS  " + "(ACCOUNT_NUM VARCHAR(9) NOT NULL,"
					+ " SORT_CODE VARCHAR(8) NOT NULL," + " BALANCE FLOAT) ");
		}catch (SQLException e) {
			if(e.getSQLState().equals(TABLE_EXISTS)) {
				dropTable();
				createTable();
			}

		}
	}
	
	private void dropTable() {
		try {
			Statement stmt = conn.createStatement();
			stmt.execute("DROP TABLE ACCOUNTS");
		}catch (SQLException e) {
			if(e.getSQLState().equals(TABLE_EXISTS)) {
				return;
			}

		}
	}
	
	public void execute(String sql) {
		try {
			Statement stmt = conn.createStatement();
			stmt.execute(sql);
		}catch(SQLException se) {
			
		}
	}
	
	public ResultSet getExecutionResults(String sql) {
		try {
			Statement stmt = conn.createStatement();
			return stmt.executeQuery(sql);
		}catch(SQLException se) {
			return null;
		}
	}

}
