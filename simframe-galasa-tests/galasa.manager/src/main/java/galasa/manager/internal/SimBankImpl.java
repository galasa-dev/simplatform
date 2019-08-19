package galasa.manager.internal;

import galasa.manager.ISimBank;

public class SimBankImpl implements ISimBank{

    private String host;
    private String webnetPort;
    private String updateAddress;

    public SimBankImpl() {
        host = "127.0.0.1";
        webnetPort = "2080";
        updateAddress = "updateAccount";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getWebnetPort() {
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
    
}