package dev.voras.simulframe.listener;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;

public class WebServiceListener implements IListener {
	private Socket socket;

	public void run() {
		System.out.println("Accepting data from: " + socket.getLocalAddress().toString());
		try {
			InputStream input = socket.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(input));
			String data = br.readLine();
			while(data != null) {
				System.out.println(data);
				data = br.readLine();
			}
				
		}catch(IOException e) {
			
		}
		

	}

	public void setSocket(Socket socket) {
		this.socket = socket;

	}

}
