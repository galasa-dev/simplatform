/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.simplatform.listener;

import java.net.Socket;

public interface IListener extends Runnable {

    public void setSocket(Socket socket);

}
