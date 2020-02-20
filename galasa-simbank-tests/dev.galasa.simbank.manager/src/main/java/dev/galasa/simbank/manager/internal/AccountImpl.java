/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.simbank.manager.internal;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dev.galasa.framework.spi.DynamicStatusStoreException;
import dev.galasa.framework.spi.IDynamicResource;
import dev.galasa.framework.spi.IDynamicStatusStoreService;
import dev.galasa.simbank.manager.AccountType;
import dev.galasa.simbank.manager.IAccount;
import dev.galasa.simbank.manager.SimBankManagerException;

public class AccountImpl implements IAccount {

    private static Log        logger = LogFactory.getLog(AccountImpl.class);

    private final SimBankImpl simBank;
    private final String      accountNumber;
    private final boolean     created;

    public AccountImpl(SimBankImpl simBank, String number, boolean created) {
        this.simBank = simBank;
        this.accountNumber = number;
        this.created = created;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public static AccountImpl generate(SimBankManagerImpl manager, boolean existing, AccountType accountType, String balance)
            throws SimBankManagerException {

        if (!manager.getSimBank().isUseJdbc()) {
            throw new SimBankManagerException("Unable to provision account as useJdbc is false");
        }
        
        if(balance != null){
            return findAccountWithExactBalance(manager.getSimBank(), balance);
        }

        if (existing) {
            return findExistingAccount(manager.getSimBank(), accountType);
        } else {
            return provisionAccount(manager.getSimBank(), accountType);
        }
    }

    private static AccountImpl findAccountWithExactBalance(SimBankImpl simBank, String balance)
            throws SimBankManagerException{

        try{        

            Random random = simBank.getManager().getFramework().getRandom();

            String sqlStatement = "SELECT ACCOUNT_NUM FROM ACCOUNTS WHERE BALANCE = " + balance;
            String accountNumber = "";
            String runName = simBank.getManager().getFramework().getTestRunName();

            StringBuilder sb = new StringBuilder();
                sb.append(Integer.toString(random.nextInt(10)));
                sb.append(Integer.toString(random.nextInt(10)));
                sb.append("-");
                sb.append(Integer.toString(random.nextInt(10)));
                sb.append(Integer.toString(random.nextInt(10)));
                sb.append("-");
                sb.append(Integer.toString(random.nextInt(10)));
                sb.append(Integer.toString(random.nextInt(10)));

            String sortCode = sb.toString();

            BigDecimal balanceBD = new BigDecimal(balance);

            ResultSet res = null;
            Statement stmt = null;


            try{
                stmt = simBank.getJdbc().createStatement();
                stmt.execute(sqlStatement);
                res = stmt.getResultSet();

                while (res.next()) {
                    accountNumber = res.getString(1);
                }

                freeAccount(simBank.getManager().getDSS(), simBank.getInstanceId(), accountNumber, runName);
                }catch(SQLException e){
                    logger.error("Error finding account with " + balance + " balance, " + e);
                    res.close();
                    freeAccount(simBank.getManager().getDSS(), simBank.getInstanceId(), accountNumber, runName);
                }finally{
                    if(res!=null){
                        res.close();
                    }
                    if(stmt!=null){
                        stmt.close();
                    }
                }

                if(!accountNumber.isEmpty()){ // *** Test if we have found an account with the exact balance
                    if (lockAccount(simBank, accountNumber, false)) {
                    return new AccountImpl(simBank, accountNumber, false);
                    }
                }

                for (int retry = 0; retry < 1000; retry++) { // *** try a 1000 times to get an account then fail
                    // *** Generate an account number
                    sb = new StringBuilder();
                    for (int i = 0; i < 9; i++) {
                        sb.append(Integer.toString(random.nextInt(10)));
                    }

                    accountNumber = sb.toString();

                    if (!lockAccount(simBank, accountNumber, true)) { // *** Attempt to lock it
                        continue;
                    }

                    // *** Attempt to insert into the database (the account may pre-exist in which
                    // case we need to choose another
                    stmt = null;
                    PreparedStatement pstmt = null;
                    ResultSet rs = null;
                    try {
                        stmt = simBank.getJdbc().createStatement();
                        stmt.execute("SELECT ACCOUNT_NUM FROM ACCOUNTS WHERE ACCOUNT_NUM = '" + accountNumber + "'");
                        rs = stmt.getResultSet();

                        if (rs.next()) { // the account pre-exists
                            rs.close();
                            freeAccount(simBank.getManager().getDSS(), simBank.getInstanceId(), accountNumber, runName); // Release
                                                                                                                        // the
                                                                                                                        // lock
                            continue; // try a new account number
                        }

                        pstmt = simBank.getJdbc()
                                .prepareStatement("INSERT INTO ACCOUNTS(ACCOUNT_NUM, SORT_CODE, BALANCE) VALUES(?,?,?)");
                        pstmt.setString(1, accountNumber);
                        pstmt.setString(2, sortCode);
                        pstmt.setBigDecimal(3, balanceBD);

                        pstmt.execute();

                        logger.info("Provisioned new account number=" + accountNumber + ",sortcode=" + sortCode
                                + ",balance=" + balance);

                        return new AccountImpl(simBank, accountNumber, true);
                    } catch (SQLException e) {
                        logger.error("Error creating account " + accountNumber, e);
                        rs.close();
                        freeAccount(simBank.getManager().getDSS(), simBank.getInstanceId(), accountNumber, runName); // Release
                                                                                                                    // the
                                                                                                                    // lock
                        continue; // try a new account number
                    } finally {
                        if (rs != null) {
                            rs.close();
                        }
                        if (stmt != null) {
                            stmt.close();
                        }
                        if (pstmt != null) {
                            pstmt.close();
                        }
                    }
            }
            throw new SimBankManagerException("Unable to create an account after 1000 attempts");
        }catch (Exception e) {
          throw new SimBankManagerException("Failed to provision an account", e);
        }
    }
        

    private static AccountImpl findExistingAccount(SimBankImpl simBank, AccountType accountType)
            throws SimBankManagerException {
        Connection conn = simBank.getJdbc();

        try {
            String balance = "";
            switch (accountType) {
                case HighValue:
                    balance = "WHERE BALANCE >= 500.00";
                    break;
                case LowValue:
                    balance = "WHERE BALANCE >= 0.00 AND BALANCE < 500.0";
                    break;
                case InDebt:
                    balance = "WHERE BALANCE < 0.00";
                    break;
                default:
                    balance = "WHERE BALANCE >= 0.00";
                    break;
            }

            // *** Could use pagination here, but as simframe will have tiny database....
            Statement stmt = conn.createStatement();
            stmt.execute("SELECT ACCOUNT_NUM FROM ACCOUNTS " + balance);
            ResultSet rs = stmt.getResultSet();

            ArrayList<String> accountNumbers = new ArrayList<>();
            while (rs.next()) {
                accountNumbers.add(rs.getString(1));
            }
            rs.close();
            stmt.close();

            Random random = simBank.getManager().getFramework().getRandom();

            AccountImpl newAccount = null;
            while (!accountNumbers.isEmpty()) {
                String accountNumber = accountNumbers.remove(random.nextInt(accountNumbers.size()));

                if (lockAccount(simBank, accountNumber, false)) {
                    newAccount = new AccountImpl(simBank, accountNumber, false);
                    break;
                }
            }

            if (newAccount == null) {
                throw new SimBankManagerException("Unable to provision existing account with type " + accountType);
            }

            return newAccount;
        } catch (SimBankManagerException e) {
            throw e;
        } catch (Exception e) {
            throw new SimBankManagerException("Failed to privision existing account", e);
        }
    }

    private static AccountImpl provisionAccount(SimBankImpl simBank, AccountType accountType)
            throws SimBankManagerException {

        try {
            Random random = simBank.getManager().getFramework().getRandom();
            String runName = simBank.getManager().getFramework().getTestRunName();

            // *** Generate a sort code
            StringBuilder sb = new StringBuilder();
            sb.append(Integer.toString(random.nextInt(10)));
            sb.append(Integer.toString(random.nextInt(10)));
            sb.append("-");
            sb.append(Integer.toString(random.nextInt(10)));
            sb.append(Integer.toString(random.nextInt(10)));
            sb.append("-");
            sb.append(Integer.toString(random.nextInt(10)));
            sb.append(Integer.toString(random.nextInt(10)));

            String sortCode = sb.toString();

            // *** Generate a balance
            int rBalance = random.nextInt(50000);
            switch (accountType) {
                case HighValue:
                    rBalance += 50000;
                    break;
                case InDebt:
                    rBalance *= -1;
                    break;
                case LowValue:
                default:
                    break;

            }

            BigDecimal balance = new BigDecimal(rBalance);
            balance = balance.divide(new BigDecimal(100));

            // *** Generate and lock an account number

            for (int retry = 0; retry < 1000; retry++) { // *** try a 1000 times to get an account then fail
                // *** Generate an account number
                sb = new StringBuilder();
                for (int i = 0; i < 9; i++) {
                    sb.append(Integer.toString(random.nextInt(10)));
                }

                String accountNumber = sb.toString();

                if (!lockAccount(simBank, accountNumber, true)) { // *** Attempt to lock it
                    continue;
                }

                // *** Attempt to insert into the database (the account may pre-exist in which
                // case we need to choose another
                Statement stmt = null;
                PreparedStatement pstmt = null;
                ResultSet rs = null;
                try {
                    stmt = simBank.getJdbc().createStatement();
                    stmt.execute("SELECT ACCOUNT_NUM FROM ACCOUNTS WHERE ACCOUNT_NUM = '" + accountNumber + "'");
                    rs = stmt.getResultSet();

                    if (rs.next()) { // the account pre-exists
                        rs.close();
                        freeAccount(simBank.getManager().getDSS(), simBank.getInstanceId(), accountNumber, runName); // Release
                                                                                                                     // the
                                                                                                                     // lock
                        continue; // try a new account number
                    }

                    pstmt = simBank.getJdbc()
                            .prepareStatement("INSERT INTO ACCOUNTS(ACCOUNT_NUM, SORT_CODE, BALANCE) VALUES(?,?,?)");
                    pstmt.setString(1, accountNumber);
                    pstmt.setString(2, sortCode);
                    pstmt.setBigDecimal(3, balance);

                    pstmt.execute();

                    logger.info("Provisioned new account number=" + accountNumber + ",sortcode=" + sortCode
                            + ",balance=" + balance);

                    return new AccountImpl(simBank, accountNumber, true);
                } catch (SQLException e) {
                    logger.error("Error creating account " + accountNumber, e);
                    rs.close();
                    freeAccount(simBank.getManager().getDSS(), simBank.getInstanceId(), accountNumber, runName); // Release
                                                                                                                 // the
                                                                                                                 // lock
                    continue; // try a new account number
                } finally {
                    if (rs != null) {
                        rs.close();
                    }
                    if (stmt != null) {
                        stmt.close();
                    }
                    if (pstmt != null) {
                        pstmt.close();
                    }
                }
            }

            throw new SimBankManagerException("Unable to create an account after 1000 attempts");
        } catch (Exception e) {
            throw new SimBankManagerException("Failed to privision a new account", e);
        }
    }

    public void discard() {
        if (created) {
            try {
                deleteAccount(simBank.getJdbc(), this.accountNumber);
            } catch (Exception e) {
                logger.error("Failed to delete account " + this.accountNumber + " during discard", e);
                return;
            }
        }

        String runName = simBank.getManager().getFramework().getTestRunName();
        try {
            freeAccount(simBank.getManager().getDSS(), simBank.getInstanceId(), this.accountNumber, runName);
            logger.info("Freed account " + accountNumber);
        } catch (DynamicStatusStoreException e) {
            logger.error("Failed to free account " + accountNumber);
        }

    }

    public static void deleteAccount(Connection conn, String accountNumber) throws SQLException {
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            stmt.execute("DELETE FROM ACCOUNTS WHERE ACCOUNT_NUM = '" + accountNumber + "'");
        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    private static boolean lockAccount(SimBankImpl simBank, String accountNumber, boolean created)
            throws DynamicStatusStoreException {

        String runName = simBank.getManager().getFramework().getTestRunName();

        IDynamicStatusStoreService dss = simBank.getManager().getDSS();

        String prefix = "instance." + simBank.getInstanceId() + ".account." + accountNumber;

        HashMap<String, String> otherProps = new HashMap<>();
        otherProps.put("run." + runName + "." + prefix, "active");
        otherProps.put(prefix + ".created", Boolean.toString(created));
        if (created) {
            otherProps.put(prefix + ".database", simBank.getJdbcUri().toString());
        }

        if (!dss.putSwap(prefix, null, runName, otherProps)) {
            return false;
        }

        IDynamicResource resource = dss.getDynamicResource("instance." + simBank.getInstanceId());
        resource.put("account." + accountNumber + ".run", runName);

        return true;
    }

    public static void freeAccount(IDynamicStatusStoreService dss, String instanceId, String accountNumber,
            String runName) throws DynamicStatusStoreException {

        IDynamicResource resource = dss.getDynamicResource("instance." + instanceId);
        resource.deletePrefix("account." + accountNumber + ".");

        String prefix = "instance." + instanceId + ".account." + accountNumber;

        HashSet<String> deleteProps = new HashSet<>();
        deleteProps.add(prefix);
        deleteProps.add(prefix + ".created");
        deleteProps.add(prefix + ".database");
        deleteProps.add("run." + runName + "." + prefix);

        dss.delete(deleteProps);

        return;
    }

    @Override
    public BigDecimal getBalance() throws SimBankManagerException {
        if (simBank.isTerminal()) {
            return getBalancebyTerminal();
        } else if (simBank.isUseJdbc()) {
            return getBalancebyJdbc();
        } else {
            throw new SimBankManagerException("No interface available to get balance");
        }
    }

    private BigDecimal getBalancebyJdbc() throws SimBankManagerException {

        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = simBank.getJdbc().createStatement();
            stmt.execute("SELECT BALANCE FROM ACCOUNTS WHERE ACCOUNT_NUM = '" + accountNumber + "'");
            rs = stmt.getResultSet();

            if (!rs.next()) { // the account pre-exists
                throw new SimBankManagerException("The account " + accountNumber + " was missing");
            }

            return rs.getBigDecimal(1);
        } catch (Exception e) {
            throw new SimBankManagerException("Problem obtaining balance via database", e);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    public BigDecimal getBalancebyTerminal() {
        BigDecimal amount = BigDecimal.ZERO;
        SimBankTerminalImpl controlTerminal = simBank.getControlTerminal();
        try {
            controlTerminal.gotoMainMenu();

            controlTerminal.pf1().waitForKeyboard().verifyTextInField("SIMBANK ACCOUNT MENU")
                    .positionCursorToFieldContaining("Account Number").tab().type(this.accountNumber).enter()
                    .waitForKeyboard().verifyTextInField("Account Found");

            // Retrieve balance from screen
            amount = new BigDecimal(controlTerminal.retrieveFieldTextAfterFieldWithString("Balance").trim());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return amount;
    }

}
