package dev.galasa.simbank.manager.internal;

import dev.galasa.docker.DockerManagerException;
import dev.galasa.docker.IDockerContainer;
import dev.galasa.simbank.manager.ISimBankWebApp;
import dev.galasa.simbank.manager.SimBankManagerException;

public class SimBankWebAppImpl implements ISimBankWebApp{
	
	private IDockerContainer container;
	private String 			 hostname;
	
	public SimBankWebAppImpl(IDockerContainer container, String hostname) {
		this.container = container;
		this.hostname = hostname;
	}
	
	public static SimBankWebAppImpl provision(SimBankManagerImpl manager) throws SimBankManagerException {
		try {
			IDockerContainer container = manager.getDockerManager().provisionContainer("simbank-webapp", "simbank-webapp", true, "PRIMARY");
			String hostname = manager.getDockerManager().getEngineHostname("PRIMARY");
		
			return new SimBankWebAppImpl(container, hostname);
		} catch (DockerManagerException e) {
			throw new SimBankManagerException("Failed to provision webapp", e);
		}
	}

	@Override
	public String getHostName() throws SimBankManagerException {
		try {
			int port = container.getExposedPorts().get("8080/tcp").get(0).getPort();
			return this.hostname + ":" + String.valueOf(port);
		} catch (DockerManagerException e) {
			throw new SimBankManagerException("Failed to get webapp port", e);
		}
		
	}
	
	

}
