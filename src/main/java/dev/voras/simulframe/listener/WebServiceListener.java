package dev.voras.simulframe.listener;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;

public class WebServiceListener implements IListener {
	private Socket socket;

	public void run() {
		System.out.println("Accepting data from: " + socket.getLocalAddress().toString());
		try {
			InputStream input = socket.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(input));
			while(br.ready()) {
				System.out.println(br.readLine());
			}
			
			OutputStream output = socket.getOutputStream();
			PrintStream ps = new PrintStream(output);
			ps.println("HTTP/1.1 200 OK");
			ps.println("Server: Simulframe 0.3.0");
			ps.println("Content-Length: 5");
			ps.println("Content-Type: text");
			ps.println("Connection: Closed");
			ps.println("");
			ps.println("Hello");
			ps.println("\r\n");
			ps.flush();
			socket.close();
				
		}catch(IOException e) {
			
		}
		

	}

	public void setSocket(Socket socket) {
		this.socket = socket;

	}

}
