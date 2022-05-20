/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.simbank.tests;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.logging.Log;

import dev.galasa.ICredentialsUsernamePassword;
import dev.galasa.Test;
import dev.galasa.artifact.BundleResources;
import dev.galasa.artifact.IBundleResources;
import dev.galasa.core.manager.CoreManager;
import dev.galasa.core.manager.ICoreManager;
import dev.galasa.core.manager.Logger;
import dev.galasa.http.HttpClient;
import dev.galasa.http.IHttpClient;
import dev.galasa.zos.IZosImage;
import dev.galasa.zos.ZosImage;
import dev.galasa.zos3270.ITerminal;
import dev.galasa.zos3270.Zos3270Terminal;

@Test
public class SimBankIVT {

    @ZosImage(imageTag = "SIMBANK")
    public IZosImage        image;

    @Zos3270Terminal(imageTag = "SIMBANK")
    public ITerminal        terminal;

    @BundleResources
    public IBundleResources resources;

    @HttpClient
    public IHttpClient      client;

    @CoreManager
    public ICoreManager     coreManager;
    
    @Logger
    public Log              logger;
    
    private ICredentialsUsernamePassword userPass;

    /**
     * Test which checks the initial balance of an account, uses the webservice to
     * credit the account, then checks the balance again. The test passes if the
     * final balance is equal to the old balance + the credited amount.
     * 
	 * @throws Exception 
     */
    
    @Test
    public void checkBankIsAvailable() throws Exception {
    	
        // Register the password to the confidential text filtering service
        userPass =  (ICredentialsUsernamePassword)image.getDefaultCredentials();
        coreManager.registerConfidentialText(userPass.getPassword(), "Password for SIMBANK application");

        // Logon through the session manager
        logger.info("Now entering username and password.");
        terminal.wfk(); //Wait for text in field perhaps?
        terminal.positionCursorToFieldContaining("Userid").tab().type(userPass.getUsername());
        
        terminal.positionCursorToFieldContaining("Password").tab().type(userPass.getPassword());
        terminal.enter().wfk();
        logger.info("Username and password have now been input");

        // Assert that the session manager has a bank session available
        assertThat(terminal.retrieveScreen()).containsOnlyOnce("SIMPLATFORM MAIN MENU");
        assertThat(terminal.retrieveScreen()).containsOnlyOnce("BANKTEST");
        
        logger.info("There is a Simbank Session available.");
        
        // Open banking application
        terminal.pf1().wfk().clear().wfk();
        
        terminal.type("bank").enter().wfk();

        // Assert that the bank menu is showing
        terminal.waitForTextInField("SIMBANK MAIN MENU");
        assertThat(terminal.retrieveScreen()).containsOnlyOnce("Options     Description        PFKey ");
        assertThat(terminal.retrieveScreen()).containsOnlyOnce("BROWSE      Browse Accounts    PF1");
        assertThat(terminal.retrieveScreen()).containsOnlyOnce("TRANSF      Transfer Money     PF4");
        logger.info("Simbank Main Menu has been reached");
    }
}