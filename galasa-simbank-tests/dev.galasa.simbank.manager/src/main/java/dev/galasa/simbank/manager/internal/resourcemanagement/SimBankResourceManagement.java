package dev.galasa.simbank.manager.internal.resourcemanagement;

import java.util.concurrent.TimeUnit;

import org.osgi.service.component.annotations.Component;

import dev.galasa.framework.spi.IConfigurationPropertyStoreService;
import dev.galasa.framework.spi.IDynamicStatusStoreService;
import dev.galasa.framework.spi.IFramework;
import dev.galasa.framework.spi.IResourceManagement;
import dev.galasa.framework.spi.IResourceManagementProvider;
import dev.galasa.framework.spi.ResourceManagerException;
import dev.galasa.simbank.manager.internal.SimBankManagerImpl;
import dev.galasa.simbank.manager.internal.properties.SimBankPropertiesSingleton;

@Component(service= {IResourceManagementProvider.class})
public class SimBankResourceManagement implements IResourceManagementProvider {
	
	private IFramework                         framework;
	private IResourceManagement                resourceManagement;
	private IDynamicStatusStoreService         dss;
	private IConfigurationPropertyStoreService cps;
	
	private AccountResourceMonitor             accountResourceMonitor;
	
	@Override
	public boolean initialise(IFramework framework, IResourceManagement resourceManagement)
			throws ResourceManagerException {
		this.framework = framework;
		this.resourceManagement = resourceManagement;
		try {
			this.cps = this.framework.getConfigurationPropertyService(SimBankManagerImpl.NAMESPACE);
			this.dss = this.framework.getDynamicStatusStoreService(SimBankManagerImpl.NAMESPACE);
			SimBankPropertiesSingleton.setCps(this.framework.getConfigurationPropertyService(SimBankManagerImpl.NAMESPACE));
			
			Class<?> load = org.apache.derby.jdbc.ClientDriver.class;
			load.newInstance();
		} catch (Exception e) {
			throw new ResourceManagerException("Unable to initialise SimBank resource monitor", e);
		}
		
		// TODO Must add a check every 5 minutes to tidy up all the properties that may have been left hanging
		// TODO Add scan of the OpenStack server to see if compute servers and floating ips have been left hanging around
		
		accountResourceMonitor = new AccountResourceMonitor(framework, resourceManagement, dss);
		
		return true;
	}

	@Override
	public void start() {
		this.resourceManagement.getScheduledExecutorService().scheduleWithFixedDelay(accountResourceMonitor, 
				this.framework.getRandom().nextInt(20),
				20, 
				TimeUnit.SECONDS);
	}

	@Override
	public void shutdown() {
	}

	@Override
	public void runFinishedOrDeleted(String runName) {
		this.accountResourceMonitor.runFinishedOrDeleted(runName);
	}

}
