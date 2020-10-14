/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.simplatform.data;

import java.math.BigDecimal;

import dev.galasa.simplatform.application.Bank;
import dev.galasa.simplatform.exceptions.InsufficientBalanceException;

public class Account {
    private String     accountNumber;
    private String     sortCode;
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

    public void creditAccount(double amount) throws InsufficientBalanceException {
        if (amount < 0 && balance.doubleValue() < (amount * -1))
            throw new InsufficientBalanceException("Account: " + accountNumber + "has insufficient funds");
        this.balance = this.balance.add(BigDecimal.valueOf(amount));
        new Bank().persistAccount(this);
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
}
