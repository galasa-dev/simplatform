/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.simplatform.listener;

import java.io.ByteArrayInputStream;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import dev.galasa.simplatform.application.Bank;
import dev.galasa.simplatform.exceptions.AccountNotFoundException;
import dev.galasa.simplatform.exceptions.InsufficientBalanceException;

/*
 * 	AccountTransferListener extends from ListenerManager
 * 	This 'Listener' is a sub-listener called upon from 
 * 	ListenerManager.
 */

public class AccountTransferListener extends ListenerManager {
	
	/* The manager variable is the instance of ListenerManager which is currently being talked to */
	/* This allows us to send our response to the listener which then sends it back to our requester */
	private ListenerManager manager;
	
	private String payload = new String();
	public String sourceAccountNumber;
	public String targetAccountNumber;

	public Double value;
	private String sourceOrgBalance;
	private String targetOrgBalance;

	private Logger log = Logger.getLogger("Simplatform");

    public void sendRequest(ListenerManager manager, String payload) {
    	/* Setting data passed from the ListenerManager */
    	this.payload = payload;
    	this.manager = manager;
    	
    	/* Starts the processing parsing of the request */
    	process();
    }
    
    public void process() {
        try {
            try {
            	/* Attempts to parse the key information from XML */
            	parseRequest();
            	
            } catch (Exception e) {
                log.warning("Exception found while reading request" + " returning 500");
                manager.return500();
                return;
            }
            
            /* Sets the balances of the two account numbers prior to processing */
        	sourceOrgBalance = Double.toString(new Bank().getBalance(sourceAccountNumber));
        	targetOrgBalance = Double.toString(new Bank().getBalance(targetAccountNumber));
        	
            try {
            	/* Attempts to transfer the given money from source to target */
                transferAccounts();
                
            } catch (InsufficientBalanceException e) {
            	/* Source account doesn't have a large enough balance to transfer */
                log.warning("Account did not have adequate balance" + " returning 400");
                manager.return400();
                return;
                
            } catch (AccountNotFoundException e) {
            	/* The account associated with the transfer do not exist... */
                log.warning("One or more accounts did not exist!" + " returning 400");
                manager.return400();
                return;
                
            }
            
            /* Formulates and sends the response back through the manager */
            manager.sendResponse(formResponse());

        } catch (Exception e) {
            log.severe("Stuff went really wrong");
            e.printStackTrace();
            return;
        }
    }
    
    /* Creates the XML response payload ready for manager to process */
	private String formResponse() throws AccountNotFoundException {
		/* Retrieve new balances of both accounts */
		String sourceBalance = Double.toString(new Bank().getBalance(sourceAccountNumber));
		String targetBalance = Double.toString(new Bank().getBalance(targetAccountNumber));

		/* Formats XML Response Doc */
		String xmlText = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
				+ "<SOAP-ENV:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
				+ "<SOAP-ENV:Body>\n" + "<TRFACCTOperationResponse xmlns=\"http://www.TRFACCT.Account.Response.com\">\n"
				+ "<transfer_account_record_response>\n" + "<account_data>\n" + "<account_source_prev_bal>"
				+ sourceOrgBalance + "</account_source_prev_bal>\n" + "<account_source_new_bal>" + sourceBalance
				+ "</account_source_new_bal>\n" + "<account_target_prev_bal>" + targetOrgBalance
				+ "</account_target_prev_bal>\n" + "<account_target_new_bal>" + targetBalance
				+ "</account_target_new_bal>\n" + "</account_data>\n" + "</transfer_account_record_response>\n"
				+ "</TRFACCTOperationResponse>\n" + "</SOAP-ENV:Body>\n" + "</SOAP-ENV:Envelope>";

		return xmlText;
	}

    private void transferAccounts() throws InsufficientBalanceException, AccountNotFoundException {
		/* Initialise Bank */
		Bank bank = new Bank();
		
		/* Transfer value from source to target account */
		bank.transferMoney(sourceAccountNumber, targetAccountNumber, value);
    }

    private void parseRequest() throws Exception {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        Document doc = null;
        
        builder = builderFactory.newDocumentBuilder();
        doc = builder.parse(new ByteArrayInputStream(payload.getBytes()));
        
        /* Finding different elements within the XML Request */
        Element soapEnvelope = doc.getDocumentElement();
        Node soapBody = soapEnvelope.getFirstChild();
        Node operation = soapBody.getFirstChild();
        Node accountRecord = operation.getFirstChild();
        Node accountkey = accountRecord.getFirstChild();
        NodeList dataItems = accountkey.getChildNodes();
        
        /* Extract and store the account numbers from the XML data set */
        sourceAccountNumber = dataItems.item(0).getFirstChild().getNodeValue();
        targetAccountNumber = dataItems.item(1).getFirstChild().getNodeValue();
        Node accountChange = accountkey.getNextSibling();
        String accountValue = accountChange.getFirstChild().getNodeValue();
        value = Double.parseDouble(accountValue);
    }

}
