package dev.galasa.simframe.data;

import dev.galasa.simframe.db.Database;
import dev.galasa.simframe.exceptions.InsufficientBalanceException;

public class Account {
	private String accountNumber;
	private String sortCode;
	private double balance;
	
	public Account(String accountNumber, String sortCode, double balance) {
		this.accountNumber = accountNumber;
		this.sortCode = sortCode;
		this.balance = balance;
	}
	
	public void creditAccount(double amount) throws InsufficientBalanceException{
		if(amount < 0 && balance < (amount * -1))
			throw new InsufficientBalanceException("Account: " + accountNumber + "has insufficient funds");
		this.balance = this.balance + amount;
		persistUpdateToDatabase();
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public String getSortCode() {
		return sortCode;
	}

	public double getBalance() {
		return balance;
	}
	
	private void persistUpdateToDatabase() {
		String query = "UPDATE ACCOUNTS SET SORT_CODE = '" + this.sortCode + "', BALANCE = " + Double.toString(this.balance)+ " WHERE ACCOUNT_NUM = '" + this.accountNumber + "'";
		Database.getDatabase().execute(query);
	}
	

}
