/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.simbank.manager;

import java.math.BigDecimal;

public interface IAccount {

    public String getAccountNumber();

    public BigDecimal getBalance() throws SimBankManagerException;

}
