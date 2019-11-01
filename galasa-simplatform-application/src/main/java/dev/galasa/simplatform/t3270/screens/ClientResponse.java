/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.simplatform.t3270.screens;

import dev.galasa.zos3270.AttentionIdentification;

public class ClientResponse {

    private final AttentionIdentification aid;
    private final String                  screen;

    public ClientResponse(AttentionIdentification aid, String screen) {
        this.aid = aid;
        this.screen = screen;
    }

    public AttentionIdentification getAid() {
        return this.aid;
    }

    public String getScreen() {
        return this.screen;
    }
}
