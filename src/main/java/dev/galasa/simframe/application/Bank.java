package dev.galasa.simframe.application;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import dev.galasa.simframe.data.Account;
import dev.galasa.simframe.db.Database;
import dev.galasa.simframe.exceptions.AccountNotFoundException;
import dev.galasa.simframe.exceptions.DuplicateAccountException;
import dev.galasa.simframe.exceptions.InsufficientBalanceException;

public class Bank {
	
	private static Bank bank;
	
	public static Bank getBank() {
		if(Bank.bank == null) {
			bank = new Bank();
			System.out.println("Creating new Bank");
		}	
		return bank;
	}
	
	private Account getAccount(String accountNumber) throws AccountNotFoundException{
		if(!accountExists(accountNumber))
			throw new AccountNotFoundException("Account: " + accountNumber + " not found");
		
		ResultSet results = Database.getDatabase().getExecutionResults("SELECT * FROM ACCOUNTS WHERE ACCOUNT_NUM = '" + accountNumber + "'");
		try{
			results.next();
			return new Account(results.getString(1), results.getString(2), results.getBigDecimal(3));
		}catch(SQLException se) {
			return null;
		}
	}
	
	public void transferMoney(String sourceAccount, String targetAccount, double amount) throws AccountNotFoundException, InsufficientBalanceException{
		Account source = getAccount(sourceAccount);
		Account target = getAccount(targetAccount);
		
		source.creditAccount(amount * -1);
		target.creditAccount(amount);
	}
	
	public boolean accountExists(String account) {
		ResultSet results = Database.getDatabase().getExecutionResults("SELECT * FROM ACCOUNTS WHERE ACCOUNT_NUM = '" + account + "'");
		try {
			if(results.next())
				return true;
			else
				return false;
		} catch (SQLException e) {
			return false;
		}
	}
	
	public String getSortCode(String account) throws AccountNotFoundException {
		return getAccount(account).getSortCode();
	}
	
	public double getBalance(String account) throws AccountNotFoundException {
		return getAccount(account).getBalance().doubleValue();
	}
	
	public void openAccount(String account, String sortCode) throws DuplicateAccountException{
		this.openAccount(account, sortCode, 0);
	}
	
	public void openAccount(String account, String sortCode, double amount) throws DuplicateAccountException{
		if(accountExists(account))
			throw new DuplicateAccountException("Account: " + account + " already exists at this bank");
		Database.getDatabase().execute("INSERT INTO ACCOUNTS ( ACCOUNT_NUM, SORT_CODE, BALANCE) VALUES ('" + account + "','" + sortCode + "'," + amount + ")");
	}
	
	public void creditAccount(String account, double amount) throws InsufficientBalanceException, AccountNotFoundException {
		getAccount(account).creditAccount(amount);
	}
	

}
