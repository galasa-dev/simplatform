package dev.voras.simulframe.t3270.screens;

import dev.voras.common.zos3270.internal.datastream.AttentionIdentification;

public class ClientResponse {
	
	private final AttentionIdentification aid;
	private final String                  screen;

	public ClientResponse(AttentionIdentification aid, String screen) {
		this.aid    = aid;
		this.screen = screen;
	}
	
	public AttentionIdentification getAid() {
		return this.aid;
	}
	
	public String getScreen() {
		return this.screen;
	}
}
