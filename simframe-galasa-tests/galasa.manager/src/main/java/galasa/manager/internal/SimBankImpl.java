package galasa.manager.internal;

import galasa.manager.ISimBank;

public class SimBankImpl implements ISimBank{

    private String host;
    private int webnetPort;
    private String updateAddress;

    public SimBankImpl(String hostAddress, int webnet) {
        host = hostAddress;
        webnetPort = webnet;
        updateAddress = "updateAccount";
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
    
}