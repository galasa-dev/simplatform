package galasa.manager;

import java.math.BigDecimal;

public interface IAccount {

    public String getAccountNumber();

	public BigDecimal getBalance() throws SimBankManagerException;
    
}