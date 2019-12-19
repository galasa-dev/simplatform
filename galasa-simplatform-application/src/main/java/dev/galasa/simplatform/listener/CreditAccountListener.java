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
 * 	CreditAccountListener extends from ListenerManager
 * 	This 'Listener' is a sub-listener called upon from 
 * 	ListenerManager.
 */

public class CreditAccountListener extends ListenerManager {
	
	/* The manager variable is the instance of ListenerManager which is currently being talked to */
	/* This allows us to send our response to the listener which then sends it back to our requester */
	private ListenerManager manager;
	
	private String payload = new String();
	private String accountNumber;
	private double value;
	
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

            try {
            	/* Attempts to update the account depending on the request */
                updateAccount();
                
            } catch (InsufficientBalanceException e) {
            	/* Error with the balance of the account we are trying to change */
                log.warning("Account did not have adequate balance" + " returning 400");
                manager.return400();
                return;
                
            } catch (AccountNotFoundException e) {
            	/* The account we are trying to update, does not exist */
                log.warning("Account did not exist" + " returning 400");
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
    	/* Sets the new balance of the account */
        String balance = Double.toString(new Bank().getBalance(accountNumber));
        
		/* Formats XML Response Doc */
        String xmlText = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<SOAP-ENV:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                + "<SOAP-ENV:Body>\n"
                + "<UPDACCTOperationResponse xmlns=\"http://www.UPDACCT.Account.Response.com\">\n"
                + "<update_account_record_response>\n" + "<account_data>\n" + "<account_available_balance>"
                + balance + "</account_available_balance>\n" + "<account_actual_balance>" + balance
                + "</account_actual_balance>\n" + "</account_data>\n" + "</update_account_record_response>\n"
                + "</UPDACCTOperationResponse>\n" + "</SOAP-ENV:Body>\n" + "</SOAP-ENV:Envelope>";
        
        return xmlText;
    }

    /* Credits the given account with the value requested */
    private void updateAccount() throws InsufficientBalanceException, AccountNotFoundException {
        new Bank().creditAccount(accountNumber, value);
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
        
        /* Extracting the key information from the XML Document */
        accountNumber = dataItems.item(1).getFirstChild().getNodeValue();
        Node accountChange = accountkey.getNextSibling();
        String accountValue = accountChange.getFirstChild().getNodeValue();
        value = Double.parseDouble(accountValue);
    }

}
