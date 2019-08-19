package galasa.manager.internal;

import galasa.manager.IAccount;

public class AccountImpl implements IAccount {

    private String accountNumber;

    public AccountImpl(String number, String tag) {

        accountNumber = number;

    }

    public String getAccountNumber() {
        return accountNumber;
    }

}