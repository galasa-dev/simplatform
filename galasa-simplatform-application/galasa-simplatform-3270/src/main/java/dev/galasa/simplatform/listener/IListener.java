/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.simplatform.listener;

import java.net.Socket;

public interface IListener extends Runnable {

    public void setSocket(Socket socket);

}
