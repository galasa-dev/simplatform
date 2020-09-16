/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.simplatform.listener;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Listener implements Runnable {

    private int    port;
    private String className;
    private Logger log = Logger.getLogger("Simplatform");

    ServerSocket   server;

    public Listener(int port, String className) throws IOException {
        log.info(() -> "Loading service: " + className);
        this.port = port;
        this.className = className;
        try {
            server = new ServerSocket(port);
            log.info(() -> "Service: " + className + " listening on port: " + this.port);
        } catch (IOException e) {
        	throw new IOException(String.format("Service: %1$s unable to listen on port: %2$d", className, this.port), e);
        }
    }

    public void run() {
        while (true) {
            Socket s = null;
            try {
                s = server.accept();
            } catch (IOException e) {
            	log.log(Level.SEVERE, "Problem with server", e);
            }
            IListener listener = getObject(className);
            if (listener == null) {
            	break;
            }
            listener.setSocket(s);
            new Thread(listener).start();
        }

    }

    private IListener getObject(String name) {
        Object obj = null;
        try {
            obj = Class.forName(name).newInstance();
        } catch (Exception e) {
        	log.log(Level.SEVERE, String.format("Could not load class: %1$s", name));
            return null;
        }

        if (obj instanceof IListener)
            return (IListener) obj;
        else
            return null;
    }

}
