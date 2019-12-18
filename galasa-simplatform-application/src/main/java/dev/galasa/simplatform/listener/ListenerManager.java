/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.simplatform.listener;

import java.io.BufferedReader;
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

/*
 * 	ListenerManager acts as a head listener for all current
 * 	and future listeners. It takes the request from the sender
 * 	and refers to the correct listener.
 * 
 * 	This solves the issue of having multiple listeners 
 * 	waiting for requests across multiple ports.
 * 
 *	Written by @Lewis James on 17/12/2019
 * 
 */

public class ListenerManager implements IListener{
	
	private Socket socket;
	private List<String> headers = new ArrayList<>();
	private String payload = new String();
	
    private Logger log = Logger.getLogger("Simplatform");

	public void run() {
		try {
			
			/* Attempt to read the request listened by the server */
			processInput();
			
			/* Interpret which sort of sub-listener we will need to call upon */
            String path = findPath().trim();
            
            /* Switching through the different path options */
            switch(path) {
            
            case "/updateAccount":   	
            	/* Initialize the CreditAccountListener and send payload */
            	new CreditAccountListener().sendRequest(this, payload);
            	break;
            	
            case "/processTransfer":
            	/* Initialize the AccountTransferListener and send payload */
            	new AccountTransferListener().sendRequest(this, payload);
            	break;
            	
            default:
            	/* Request has not been recognised */
            	log.warning("Illegal type of request, " + path + " was read, expected, /updateAccount, /processTransfer");
            	return;
            }
            
		} catch (Exception e) {
			log.severe("The request was not readable. Something has gone, very... wrong.");
			e.printStackTrace();
			return;
		}
	}
	
	/* Returns the path located in the header of the XML Request */
    private String findPath() {
        StringTokenizer st = new StringTokenizer(headers.get(0), " ");
        if (st.countTokens() == 3) {
            st.nextToken();
            return st.nextToken();
        }
        return "";
    }
    
    /* Prepares and sends the response passed back from the sub listener */
    public void sendResponse(String xmlText) throws Exception {
		OutputStream output;
		output = socket.getOutputStream();
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
	
    /* Reads the key information from the XML Request */
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
	
	public void setSocket(Socket socket) {
		this.socket = socket;
	}

	public void return500() throws IOException {
		OutputStream output = socket.getOutputStream();
		PrintStream ps = new PrintStream(output);
		ps.println("HTTP/1.1 500 Internal Server Error");
		ps.println("\r\n");
		ps.flush();
		socket.close();
	}

	public void return400() throws IOException {
		OutputStream output = socket.getOutputStream();
		PrintStream ps = new PrintStream(output);
		ps.println("HTTP/1.1 400 Method Not Allowed");
		ps.println("\r\n");
		ps.flush();
		socket.close();
	}

}
