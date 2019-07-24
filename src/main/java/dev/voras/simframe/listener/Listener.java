package dev.voras.simframe.listener;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Listener implements Runnable{
	
	private int port;
	private String className;
	
	ServerSocket server;
	
	public Listener (int port, String className) {
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
