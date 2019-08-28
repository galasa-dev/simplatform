package galasa.manager;

import java.math.BigDecimal;

import dev.galasa.common.zos3270.ITerminal;

public interface ISimBank {

    public String getHost();

    public int getWebnetPort();

    public String getFullAddress();

    public String getUpdateAddress();

    public BigDecimal getBalance(String AccNum, ITerminal terminal);

}