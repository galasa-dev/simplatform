/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.simplatform.application;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

import dev.galasa.simplatform.data.Account;
import dev.galasa.simplatform.db.Database;
import dev.galasa.simplatform.exceptions.AccountNotFoundException;
import dev.galasa.simplatform.exceptions.DuplicateAccountException;
import dev.galasa.simplatform.exceptions.InsufficientBalanceException;

public class Bank {
    private Logger          log      = Logger.getLogger("Simplatform");
    private static Database database = null;

    public Bank() {
        if (database == null)
            database = new Database();
    }

    private Account getAccount(String accountNumber) throws AccountNotFoundException {
        log.info("Searching for account: " + accountNumber);
        if (!accountExists(accountNumber)) {
            log.info("Account: " + accountNumber + " not found");
            throw new AccountNotFoundException("Account: " + accountNumber + " not found");
        }
        ResultSet results = database
                .getExecutionResults("SELECT * FROM ACCOUNTS WHERE ACCOUNT_NUM = '" + accountNumber + "'");
        try {
            results.next();
            log.info("Account: " + accountNumber + " found");
            return new Account(results.getString(1), results.getString(2), results.getBigDecimal(3));
        } catch (SQLException se) {
            return null;
        }
    }

    public void transferMoney(String sourceAccount, String targetAccount, double amount)
            throws AccountNotFoundException, InsufficientBalanceException {
        log.info("Transfering  " + amount + " from account: " + sourceAccount + " to account: " + targetAccount);
        Account source = getAccount(sourceAccount);
        Account target = getAccount(targetAccount);

        source.creditAccount(amount * -1);
        target.creditAccount(amount);
    }

    public boolean accountExists(String account) {
        log.info("Checking if account: " + account + " exists");
        ResultSet results = database
                .getExecutionResults("SELECT * FROM ACCOUNTS WHERE ACCOUNT_NUM = '" + account + "'");
        try {
            if (results.next()) {
                log.info("Account exists");
                return true;
            } else {
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

    public void openAccount(String account, String sortCode) throws DuplicateAccountException {
        this.openAccount(account, sortCode, 0);
    }

    public boolean openAccount(String account, String sortCode, double amount) throws DuplicateAccountException {
        if (accountExists(account)) {
            log.info("Account: " + account + " already exists at this bank");
            throw new DuplicateAccountException("Account: " + account + " already exists at this bank");
        }
        log.info("Creating account: " + account);
        return database.execute("INSERT INTO ACCOUNTS ( ACCOUNT_NUM, SORT_CODE, BALANCE) VALUES ('" + account + "','"
                + sortCode + "'," + amount + ")");
    }

    public void creditAccount(String account, double amount)
            throws InsufficientBalanceException, AccountNotFoundException {
        log.info("Crediting account: " + account + " with: " + amount);
        getAccount(account).creditAccount(amount);
    }

    public void persistAccount(Account account) {
        String query = "UPDATE ACCOUNTS SET SORT_CODE = '" + account.getSortCode() + "', BALANCE = "
                + account.getBalance().toPlainString() + " WHERE ACCOUNT_NUM = '" + account.getAccountNumber() + "'";
        database.execute(query);
    }

	public String getDatabaseException() {
		return database.getExceptionMessage();
	}

}
