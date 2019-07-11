package dev.voras.simulframe.listener;

import java.net.Socket;

public class WebServiceListener implements IListener {
	private Socket socket;

	public void run() {
		System.out.println("Accepting data from: " + socket.getLocalAddress().toString());

	}

	public void setSocket(Socket socket) {
		this.socket = socket;

	}

}
