package galasa.manager.internal;

import java.math.BigDecimal;

import dev.galasa.common.zos3270.ITerminal;
import galasa.manager.ISimBank;

public class SimBankImpl implements ISimBank{

    private final String host;
    private final int webnetPort;
    private final String updateAddress;
    private final String username;
    private final String password;

    public SimBankImpl(String hostAddress, int webnet, String username, String password) {
    	this.host = hostAddress;
        this.webnetPort = webnet;
        this.updateAddress = "updateAccount";
        this.username = username;
        this.password = password;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public int getWebnetPort() {
        return webnetPort;
    }

    @Override
    public String getFullAddress() {
        return "http://"+host+":"+webnetPort;
    }

    @Override
    public String getUpdateAddress() {
        return updateAddress;
    }

    private void login(ITerminal terminal) {
        try {
            //Initial log in to system
            terminal.waitForKeyboard()
                    .positionCursorToFieldContaining("Userid").tab().type(username)
                    .positionCursorToFieldContaining("Password").tab().type(password)
                    .enter().waitForKeyboard()

            //Open banking application
                    .pf1().waitForKeyboard()
                    .clear().waitForKeyboard()
                    .tab().type("bank").enter().waitForKeyboard();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public BigDecimal getBalance(String AccNum, ITerminal terminal) {
        BigDecimal amount = BigDecimal.ZERO;
        try {
            //Check if already logged into system
            if(terminal.waitForKeyboard().retrieveScreen().contains("LOGON"))
                login(terminal);
            //Open account menu and enter account number
            terminal.pf1().waitForKeyboard()
                    .positionCursorToFieldContaining("Account Number").tab()
                    .type(AccNum).enter().waitForKeyboard();

            //Retrieve balance from screen
            amount = new BigDecimal(terminal.retrieveFieldTextAfterFieldWithString("Balance").trim());

            //Return to bank menu
            terminal.pf3().waitForKeyboard();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return amount;
    }
}