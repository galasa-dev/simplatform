/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
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
import java.util.logging.Level;
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
import dev.galasa.simplatform.main.Simplatform;

public class WebServiceListener implements IListener {
    private Socket       socket;
    private List<String> headers = new ArrayList<>();
    private String       payload = "";

    private String       accountNumber;
    private double       value;

    private Logger       log     = Logger.getLogger("Simplatform");

    public void run() {
        try {
            processInput();
            String path = findPath();
            if (!"/updateAccount".equals(path.trim())) {
                log.log(Level.WARNING, () -> String.format("Request was not sent to path /updateAccount, it was: %1$s returning 404", path));
                return404();
                return;
            }

            if (!"POST".equals(getMethod())) {
            	log.log(Level.WARNING, () -> String.format("Request was not using POST HTTP verb, it was: %1$s returning 405", getMethod()));
                return405();
                return;
            }

            try {
                parseRequest();
            } catch (Exception e) {
                log.warning("Exception found while reading request, returning 500");
                return500();
                return;
            }

            try {
                updateAccount();
            } catch (InsufficientBalanceException e) {
                log.warning("Account did not have adequate balance, returning 400");
                return400();
                return;
            } catch (AccountNotFoundException e) {
                log.warning("Account did not exist" + " returning 400");
                return400();
                return;
            }

            String balance = Double.toString(new Bank().getBalance(accountNumber));

            String xmlText = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<SOAP-ENV:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                    + "<SOAP-ENV:Body>\n"
                    + "<UPDACCTOperationResponse xmlns=\"http://www.UPDACCT.Account.Response.com\">\n"
                    + "<update_account_record_response>\n" + "<account_data>\n" + "<account_available_balance>"
                    + balance + "</account_available_balance>\n" + "<account_actual_balance>" + balance
                    + "</account_actual_balance>\n" + "</account_data>\n" + "</update_account_record_response>\n"
                    + "</UPDACCTOperationResponse>\n" + "</SOAP-ENV:Body>\n" + "</SOAP-ENV:Envelope>";

            OutputStream output = socket.getOutputStream();
            PrintStream ps = new PrintStream(output);
            ps.println("HTTP/1.1 200 OK");
            ps.println("Server: Simulplatform " + Simplatform.getVersion());
            ps.println("Content-Length: " + xmlText.length());
            ps.println("Content-Type: application/soap+xml; charset=\"utf-8\"\r\n");
            ps.println(xmlText);
            ps.println("\r\n");
            ps.flush();
            socket.close();

        } catch (Exception e) {
            log.severe("Stuff went really wrong");
            log.severe(e.getMessage());
        }

    }

    private void updateAccount() throws InsufficientBalanceException, AccountNotFoundException {
        new Bank().creditAccount(accountNumber, value);
    }

    private void parseRequest() throws Exception {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        Document doc = null;

        builder = builderFactory.newDocumentBuilder();
        doc = builder.parse(new ByteArrayInputStream(payload.getBytes()));

        Element soapEnvelope = doc.getDocumentElement();

        Node soapBody = soapEnvelope.getFirstChild();

        Node operation = soapBody.getFirstChild();

        Node accountRecord = operation.getFirstChild();

        Node accountkey = accountRecord.getFirstChild();

        NodeList dataItems = accountkey.getChildNodes();

        accountNumber = dataItems.item(1).getFirstChild().getNodeValue();

        Node accountChange = accountkey.getNextSibling();

        String accountValue = accountChange.getFirstChild().getNodeValue();

        value = Double.parseDouble(accountValue);
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

    private String getMethod() {
        StringTokenizer st = new StringTokenizer(headers.get(0), " ");
        return st.nextToken();
    }

    private String findPath() {
        StringTokenizer st = new StringTokenizer(headers.get(0), " ");
        if (st.countTokens() == 3) {
            st.nextToken();
            return st.nextToken();
        }
        return "";
    }

    private void processInput() throws InterruptedException {
        BufferedReader br = null;
        try {
        	log.log(Level.WARNING, () -> String.format("Received HTTP request from address: %1$s", socket.getInetAddress().toString()));
            InputStream input = socket.getInputStream();
            br = new BufferedReader(new InputStreamReader(input));
        } catch (IOException e) {
            log.warning("Unable to access input stream from HTTP");
            return;
        }

        boolean readAllHeaders = false;
        try {
            while (!br.ready()) {
            	Thread.sleep(1000);
            }
            while (br.ready()) {
                String data = br.readLine();
                if (readAllHeaders) {
                    log.info(data);
                    payload += data;
                } else {
                    if (data.equals("")) {
                        readAllHeaders = true;
                    } else {
						log.info(data);
                        headers.add(data);
                    }

                }
            }
        } catch (IOException e) {
            log.warning("Unable to access input stream from HTTP");
        }

    }

    public void setSocket(Socket socket) {
        this.socket = socket;

    }

}
