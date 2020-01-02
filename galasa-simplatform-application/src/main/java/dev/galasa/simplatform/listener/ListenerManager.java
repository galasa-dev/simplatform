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
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
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
	
	/* Contains a list of all the listeners, along with their path names */
	/* Value is of type ListenerManager as they are children of our manager */
	private HashMap<String, ListenerManager> listeners = new HashMap<>();
	
	private String payload = new String();
	
    public Logger log = Logger.getLogger("Simplatform");
	
	/* The manager variable is the instance of ListenerManager which is currently being talked to */
	/* This allows us to send our response to the listener which then sends it back to our requester */
	protected ListenerManager manager;

	public void run() {
		
		/* Register the listeners within Listener Manager */
		/* Key - Path     Value - Listener Class */
		listeners.put("/updateAccount", new CreditAccountListener());
		listeners.put("/processTransfer", new AccountTransferListener());
		
		try {
			/* Attempt to read the request listened by the server */
			processInput();
			
			/* Interpret which sort of sub-listener we will need to call upon */
            String path = findPath().trim();
            
            for (String s : listeners.keySet()) {
            	if (s.equals(path)) {
            		ListenerManager listener = listeners.get(s);
            		listener.sendRequest(this, payload);
            	}
            }
            
		} catch (Exception e) {
			log.severe("The request was not readable. Severe Exception caught");
			this.return500();
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
    
    /* Dummy method which allows us to access our sub listeners init method */
    protected void sendRequest(ListenerManager manager, String payload) throws IOException{}
	
    /* Reads the key information from the XML Request */
	private void processInput() {
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
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					log.severe("HTTP Stream has been interrupted");
					return;
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

	public void return500() {
		try {
			OutputStream output = socket.getOutputStream();
			PrintStream ps = new PrintStream(output);
			ps.println("HTTP/1.1 500 Internal Server Error");
			ps.println("\r\n");
			ps.flush();
			socket.close();
		} catch (IOException e) {
			log.severe("IO Exception - Socket Failure");
		}
	}

	public void return400() {
		try {
			OutputStream output = socket.getOutputStream();
			PrintStream ps = new PrintStream(output);
			ps.println("HTTP/1.1 400 Method Not Allowed");
			ps.println("\r\n");
			ps.flush();
			socket.close();	
		} catch (IOException e) {
			log.severe("IO Exception - Socket Failure");
		}

	}

}
