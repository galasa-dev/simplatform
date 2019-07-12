package dev.voras.simulframe.listener;

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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import dev.voras.simulframe.application.Bank;
import dev.voras.simulframe.exceptions.AccountNotFoundException;
import dev.voras.simulframe.exceptions.InsufficientBalanceException;

public class WebServiceListener implements IListener {
	private Socket socket;
	private List<String> headers = new ArrayList<>();
	private String payload = new String();
	
	private String accountNumber;
	private double value;

	public void run() {
		try {
			processInput();
			String path = findPath();
			if(!"/updateAccount".equals(path.trim())) {
				return404();
				return;
			}
			
			if(!"POST".equals(getMethod())) {
				return405();
				return;
			}
			
			try {
				parseRequest();
			}catch(Exception e) {
				return500();
				return;
			}
			
			try {
				updateAccount();
			}catch(Exception e) {
				return400();
				return;
			}
			
			
			OutputStream output = socket.getOutputStream();
			PrintStream ps = new PrintStream(output);
			ps.println("HTTP/1.1 200 OK");
			ps.println("Server: Simulframe 0.3.0");
			ps.println("Content-Length: "  + Double.toString(Bank.getBank().getBalance(accountNumber)).length());
			ps.println("Content-Type: text");
			ps.println("Connection: Closed");
			ps.println("");
			ps.println(Double.toString(Bank.getBank().getBalance(accountNumber)));
			ps.println("\r\n");
			ps.flush();
			socket.close();
				
		}catch(Exception e) {
			
		}

	}
	
	private void updateAccount() throws InsufficientBalanceException, AccountNotFoundException {
		Bank.getBank().creditAccount(accountNumber, value);
	}
	
	private void parseRequest() throws Exception{
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
		if(st.countTokens() == 3) {
			st.nextToken();
			return st.nextToken();
		}
		return "";
	}
	
	private void processInput() throws IOException{
		InputStream input = socket.getInputStream();
		BufferedReader br = new BufferedReader(new InputStreamReader(input));
		
		boolean readAllHeaders = false;
		while(br.ready()) {
			String data = br.readLine();
			if(readAllHeaders)
				payload+=data;
			else {
				if(data.equals("")) {
					readAllHeaders = true;
				}else {
					headers.add(data);
				}
					
			}
		}
	}

	public void setSocket(Socket socket) {
		this.socket = socket;

	}

}
