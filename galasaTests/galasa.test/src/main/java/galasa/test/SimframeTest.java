
package galasa.test;

import dev.voras.Test;
import dev.voras.common.http.HttpClient;
import dev.voras.common.zos.IZosImage;
import dev.voras.common.zos.ZosImage;
import dev.voras.common.zos3270.ITerminal;
import dev.voras.common.zos3270.Zos3270Terminal;
import dev.voras.common.zos3270.spi.Terminal;

public class SimframeTest { 

    @ZosImage(imageTag="A")
    public IZosImage image;

    @Zos3270Terminal(imageTag="A")
    public ITerminal terminal;

    @HttpClient
    public HttpClient client;

    @Test
    public void testNotNull() throws Exception{
        if(terminal == null) {
            throw new Exception();
        }

    }

    @Test
    public void test1() {

        try {

        terminal.waitForKeyboard();

        terminal.positionCursorToFieldContaining("Userid").tab()
                .type("boo")
                .positionCursorToFieldContaining("Password").tab()
                .type("eek")
                .enter()
                .waitForKeyboard()

                .pf1()
                .waitForKeyboard()
                .clear()
                .waitForKeyboard()
                .tab().type("bank")
                .enter()
                .waitForKeyboard()

                .pf1()
                .waitForKeyboard()
                .positionCursorToFieldContaining("Account Number").tab()
                .type("123456789")
                .enter()
                .waitForKeyboard();

        Double userBalance = Double.parseDouble(terminal.retrieveFieldTextAfterFieldWithString("Balance").trim());
        System.err.println(userBalance);

        terminal.positionCursorToFieldContaining("Account Number").tab();
        System.err.println(terminal.retrieveFieldAtCursor());

        System.err.println(terminal.retrieveScreen());

        terminal.pf3();

        Double amount = 500.50;

        sendRequest(amount, "http://127.0.0.1:2080/updateAccount");

        terminal.pf1()
                .waitForKeyboard()
                .positionCursorToFieldContaining("Account Number").tab()
                .type("123456789")
                .enter()
                .waitForKeyboard();

        terminal.reportScreenWithCursor()
                .reportScreen();
        
        //SOME CODE TO FIND THE BALANCE

        double newUserBalance = 500.50;

        //HOW EVER ASSERTION IS DONE IN GALASA(userBalance + amount == newUserBalance);

        } catch(Exception e) {
            System.out.println("Something gone wrong: " + e);
        }
    }

    private String sendRequest(Double amount, String url) {
        return null;
    }
}