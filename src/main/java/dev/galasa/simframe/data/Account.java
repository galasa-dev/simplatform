package dev.galasa.simframe.data;

import java.math.BigDecimal;

import dev.galasa.simframe.db.Database;
import dev.galasa.simframe.exceptions.InsufficientBalanceException;

public class Account {
	private String accountNumber;
	private String sortCode;
	private BigDecimal balance;
	
	public Account(String accountNumber, String sortCode, double balance) {
		this.accountNumber = accountNumber;
		this.sortCode = sortCode;
		this.balance = BigDecimal.valueOf(balance);
	}
	
	public Account(String accountNumber, String sortCode, BigDecimal balance) {
		this.accountNumber = accountNumber;
		this.sortCode = sortCode;
		this.balance = balance;
	}
	
	public void creditAccount(double amount) throws InsufficientBalanceException{
		if(amount < 0 && balance.doubleValue() < (amount * -1))
			throw new InsufficientBalanceException("Account: " + accountNumber + "has insufficient funds");
		this.balance = this.balance.add(BigDecimal.valueOf(amount));
		persistUpdateToDatabase();
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public String getSortCode() {
		return sortCode;
	}

	public BigDecimal getBalance() {
		return balance;
	}
	
	private void persistUpdateToDatabase() {
		String query = "UPDATE ACCOUNTS SET SORT_CODE = '" + this.sortCode + "', BALANCE = " + this.balance.toPlainString()+ " WHERE ACCOUNT_NUM = '" + this.accountNumber + "'";
		Database.getDatabase().execute(query);
	}
	

}
