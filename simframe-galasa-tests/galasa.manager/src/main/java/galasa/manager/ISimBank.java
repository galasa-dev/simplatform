package galasa.manager;

import java.math.BigDecimal;

public interface ISimBank {

    public String getHost();

    public int getWebnetPort();

    public String getFullAddress();

    public String getUpdateAddress();

    public BigDecimal getBalance(String AccNum);

}