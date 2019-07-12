package dev.voras.simulframe.application;
import java.util.HashMap;
import java.util.Map;

import dev.voras.simulframe.data.Account;
import dev.voras.simulframe.exceptions.AccountNotFoundException;
import dev.voras.simulframe.exceptions.DuplicateAccountException;
import dev.voras.simulframe.exceptions.InsufficientBalanceException;

public class Bank {
	
	private static Bank bank;
	
	private Map<String,Account> accounts;
	
	public static Bank getBank() {
		if(Bank.bank == null) {
			bank = new Bank();
			System.out.println("Creating new Bank");
		}	
		return bank;
	}
	
	private Bank() {
		this.accounts = new HashMap<String, Account>();
	}
	
	private Account getAccount(String accountNumber) throws AccountNotFoundException{
		Account a = accounts.get(accountNumber);
		if(a == null)
			throw new AccountNotFoundException("Account: " + accountNumber + " not found");
		return a;
	}
	
	public void transferMoney(String sourceAccount, String targetAccount, double amount) throws AccountNotFoundException, InsufficientBalanceException{
		Account source = getAccount(sourceAccount);
		Account target = getAccount(targetAccount);
		
		source.creditAccount(amount * -1);
		target.creditAccount(amount);
	}
	
	public boolean accountExists(String account) {
		return accounts.containsKey(account);
	}
	
	public String getSortCode(String account) throws AccountNotFoundException {
		return accounts.get(account).getSortCode();
	}
	
	public double getBalance(String account) throws AccountNotFoundException {
		return accounts.get(account).getBalance();
	}
	
	public void closeAccount(String account) {
		if(accounts.containsKey(account))
			accounts.remove(account);
	}
	
	public void openAccount(String account, String sortCode) throws DuplicateAccountException{
		this.openAccount(account, sortCode, 0);
	}
	
	public void openAccount(String account, String sortCode, double amount) throws DuplicateAccountException{
		if(accounts.containsKey(account))
			throw new DuplicateAccountException("Account: " + account + " already exists at this bank");
		accounts.put(account, new Account(account,sortCode,amount));
	}
	
	public void creditAccount(String account, double amount) throws InsufficientBalanceException, AccountNotFoundException {
		getAccount(account).creditAccount(amount);
	}
	

}
