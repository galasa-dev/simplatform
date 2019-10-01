package dev.galasa.simplatform.listener;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

public class Listener implements Runnable{
	
	private int port;
	private String className;
	private Logger log = Logger.getLogger("Simplatform");
	
	
	ServerSocket server;
	
	public Listener (int port, String className) {
		log.info("Loading service: " + className + " listening on port: " + port);
		this.port = port;
		this.className = className;
		try {
			server = new ServerSocket(port);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void run() {
		while(true) {
			Socket s = null;
			try {
				s = server.accept();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			IListener listener = getObject(className);
			listener.setSocket(s);
			new Thread(listener).start();
		}
		
	}
	
	private IListener getObject(String name) {
		Object obj = null;
		try {
			obj = Class.forName(name).newInstance();
		}catch(Exception e) {
			System.out.println("Could not load class: " + name);
			return null;
		}
		
		if(obj instanceof IListener)
			return (IListener)obj;
		else 
			return null;
	}

}
