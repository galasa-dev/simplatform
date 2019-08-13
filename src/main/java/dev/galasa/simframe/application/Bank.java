package dev.galasa.simframe.application;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import dev.galasa.simframe.data.Account;
import dev.galasa.simframe.db.Database;
import dev.galasa.simframe.exceptions.AccountNotFoundException;
import dev.galasa.simframe.exceptions.DuplicateAccountException;
import dev.galasa.simframe.exceptions.InsufficientBalanceException;

public class Bank {
	
	private static Bank bank;
	private Logger log = Logger.getLogger("Simframe");
	
	public static Bank getBank() {
		if(Bank.bank == null) {
			bank = new Bank();
			bank.log.info("Creating new Bank");
		}	
		return bank;
	}
	
	private Account getAccount(String accountNumber) throws AccountNotFoundException{
		log.info("Searching for account: " + accountNumber);
		if(!accountExists(accountNumber)) {
			log.info("Account: " + accountNumber + " not found");
			throw new AccountNotFoundException("Account: " + accountNumber + " not found");
		}
		ResultSet results = Database.getDatabase().getExecutionResults("SELECT * FROM ACCOUNTS WHERE ACCOUNT_NUM = '" + accountNumber + "'");
		try{
			results.next();
			log.info("Account: " + accountNumber + " found");
			return new Account(results.getString(1), results.getString(2), results.getBigDecimal(3));
		}catch(SQLException se) {
			return null;
		}
	}
	
	public void transferMoney(String sourceAccount, String targetAccount, double amount) throws AccountNotFoundException, InsufficientBalanceException{
		log.info("Transfering  " + amount + " from account: " + sourceAccount + " to account: " + targetAccount);
		Account source = getAccount(sourceAccount);
		Account target = getAccount(targetAccount);
		
		source.creditAccount(amount * -1);
		target.creditAccount(amount);
	}
	
	public boolean accountExists(String account) {
		log.info("Checking if account: " + account + " exists");
		ResultSet results = Database.getDatabase().getExecutionResults("SELECT * FROM ACCOUNTS WHERE ACCOUNT_NUM = '" + account + "'");
		try {
			if(results.next()) {
				log.info("Account exists");
				return true;
			}
			else {
				log.info("Account doesn't exist");
				return false;
			}
		} catch (SQLException e) {
			log.info("Account doesn't exist");
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
		if(accountExists(account)) {
			log.info("Account: " + account + " already exists at this bank");
			throw new DuplicateAccountException("Account: " + account + " already exists at this bank");
		}
		log.info("Creating account: " + account);	
		Database.getDatabase().execute("INSERT INTO ACCOUNTS ( ACCOUNT_NUM, SORT_CODE, BALANCE) VALUES ('" + account + "','" + sortCode + "'," + amount + ")");
	}
	
	public void creditAccount(String account, double amount) throws InsufficientBalanceException, AccountNotFoundException {
		log.info("Crediting account: " + account + " with: " + amount);
		getAccount(account).creditAccount(amount);
	}
	

}
