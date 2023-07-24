/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.simbank.manager;

import java.math.BigDecimal;

public interface IAccount {

    public String getAccountNumber();

    public BigDecimal getBalance() throws SimBankManagerException;

}
