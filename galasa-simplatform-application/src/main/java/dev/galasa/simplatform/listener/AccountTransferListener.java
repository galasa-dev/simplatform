package dev.galasa.simplatform.listener;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
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

public class AccountTransferListener implements IListener{

    private List<String> headers = new ArrayList<>();
    private Socket       socket;
    private String       payload = new String();

    private String       sourceAccountNumber;
    private String		 targetAccountNumber;
    private double       value;
    
    private String		 sourceOrgBalance;
    private String		 targetOrgBalance;

    private Logger       log     = Logger.getLogger("Simplatform");
    
	public void run() {
		try {
			/* Attempts to read the request */
			processInput();
			
			/* Finding which listener path was intended */
            String path = findPath();
            
            /* Check whether this is the appropriate listener to utilise */
            if (!"/processTransfer".equals(path.trim())) {
                log.warning("Request was not sent to path /processTransfer, it was: " + path + " returning 404");
                return404();
                return;
            }
			
            /* Checking the type of Web Request */
            if (!"POST".equals(getMethod())) {
                log.warning("Request was not using POST HTTP verb, it was: " + getMethod() + " returning 405");
                return405();
                return;
            }
            
            try {
            	/* Read Web Request information */
                parseRequest();
            } catch (Exception e) {
            	/*  Output warning message and return an error code */
                log.warning("Exception found while reading request" + " returning 500");
                return500();
                return;
            }
            
            try {
            	/* Attempt to transfer from source to target account */
                accountTransfer();
            } catch (InsufficientBalanceException e) {
            	/* Source Account had insufficient funds */
                log.warning("Source Account did not have adequate balance" + " returning 400");
                return400();
                return;
                
            } catch (AccountNotFoundException e) {
            	/* An invalid account number was provided */
                log.warning("One or more account(s) did not exist" + " returning 400");
                return400();
                return;
            }
            
            /* Creates an XML Response to send back to the requester */
            sendResponse();
			
			
		} catch (Exception e) {
			log.severe("Bad Request - Severe Error");
            log.severe(e.getMessage());
		}
	}
	
	private void sendResponse() throws Exception {
		/* Retrieve new balances of both accounts */
		String sourceBalance = Double.toString(new Bank().getBalance(sourceAccountNumber));
		String targetBalance = Double.toString(new Bank().getBalance(targetAccountNumber));
		
		/* Formats XML Response Doc */
        String xmlText = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<SOAP-ENV:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                + "<SOAP-ENV:Body>\n"
                + "<TRFACCTOperationResponse xmlns=\"http://www.TRFACCT.Account.Response.com\">\n"
                + "<transfer_account_record_response>\n" + "<account_data>\n" + "<account_source_prev_bal>"
                + sourceOrgBalance + "</account_source_prev_bal>\n" + "<account_source_new_bal>" + sourceBalance
                + "</account_source_new_bal>\n" 
                + "<account_target_prev_bal>"
                + targetOrgBalance + "</account_target_prev_bal>\n" + "<account_target_new_bal>" + targetBalance
                + "</account_target_new_bal>\n" + "</account_data>\n" + "</transfer_account_record_response>\n"
                + "</TRFACCTOperationResponse>\n" + "</SOAP-ENV:Body>\n" + "</SOAP-ENV:Envelope>";
		
        /* Sends XML Response back to requester */
        OutputStream output = socket.getOutputStream();
        PrintStream ps = new PrintStream(output);
        ps.println("HTTP/1.1 200 OK");
        ps.println("Server: Simulplatform 0.3.0");
        ps.println("Content-Length: " + xmlText.length());
        ps.println("Content-Type: application/soap+xml; charset=\"utf-8\"\r\n");
        ps.println(xmlText);
        ps.println("\r\n");
        ps.flush();
        socket.close();
	}
	
	private void accountTransfer() throws InsufficientBalanceException, AccountNotFoundException {
		/* Initialise Bank */
		Bank bank = new Bank();
		
		/* Save original balances for comparison purposes */
		sourceOrgBalance = Double.toString(bank.getBalance(sourceAccountNumber));
		targetOrgBalance = Double.toString(bank.getBalance(targetAccountNumber));
		
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
    
	
    private void processInput() {
        BufferedReader br = null;
        try {
            log.info("Received HTTP request from address: " + socket.getInetAddress().toString());
            InputStream input = socket.getInputStream();
            br = new BufferedReader(new InputStreamReader(input));
        } catch (IOException e) {
            log.warning("Unable to access input stream from HTTP");
            return;
        }

        boolean readAllHeaders = false;
        try {
            while (!br.ready()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }

            }
            while (br.ready()) {
                String data = br.readLine();
                if (readAllHeaders)
                    payload += data;
                else {
                    if (data.equals("")) {
                        readAllHeaders = true;
                    } else {
                        headers.add(data);
                    }

                }
            }
        } catch (IOException e) {
            log.warning("Unable to access input stream from HTTP");
            return;
        }

    }
    
    private String findPath() {
        StringTokenizer st = new StringTokenizer(headers.get(0), " ");
        if (st.countTokens() == 3) {
            st.nextToken();
            return st.nextToken();
        }
        return "";
    }
    
    private String getMethod() {
        StringTokenizer st = new StringTokenizer(headers.get(0), " ");
        return st.nextToken();
    }
    
    private void return405() throws IOException {
        OutputStream output = socket.getOutputStream();
        PrintStream ps = new PrintStream(output);
        ps.println("HTTP/1.1 405 Method Not Allowed");
        ps.println("\r\n");
        ps.flush();
        socket.close();
    }

    private void return500() throws IOException {
        OutputStream output = socket.getOutputStream();
        PrintStream ps = new PrintStream(output);
        ps.println("HTTP/1.1 500 Internal Server Error");
        ps.println("\r\n");
        ps.flush();
        socket.close();
    }

    private void return404() throws IOException {
        OutputStream output = socket.getOutputStream();
        PrintStream ps = new PrintStream(output);
        ps.println("HTTP/1.1 404 Not Found");
        ps.println("\r\n");
        ps.flush();
        socket.close();
    }

    private void return400() throws IOException {
        OutputStream output = socket.getOutputStream();
        PrintStream ps = new PrintStream(output);
        ps.println("HTTP/1.1 400 Method Not Allowed");
        ps.println("\r\n");
        ps.flush();
        socket.close();
    }

	public void setSocket(Socket socket) {
		this.socket = socket;
	}
}
