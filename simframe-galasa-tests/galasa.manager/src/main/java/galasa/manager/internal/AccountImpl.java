package galasa.manager.internal;

import galasa.manager.IAccount;

public class AccountImpl implements IAccount {

    private String accountNumber;

    public AccountImpl(String number) {

        accountNumber = number;

    }

    public String getAccountNumber() {
        return accountNumber;
    }

}