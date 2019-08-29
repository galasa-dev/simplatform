package galasa.manager.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.annotations.Component;

import dev.galasa.ManagerException;
import dev.galasa.common.ipnetwork.IIpHost;
import dev.galasa.common.zos.IZosImage;
import dev.galasa.common.zos.IZosManager;
import dev.galasa.common.zos.spi.IZosManagerSpi;
import dev.galasa.common.zos3270.IZos3270Manager;
import dev.galasa.common.zos3270.Zos3270ManagerException;
import dev.galasa.common.zos3270.spi.IZos3270ManagerSpi;
import dev.galasa.framework.spi.AbstractManager;
import dev.galasa.framework.spi.AnnotatedField;
import dev.galasa.framework.spi.GenerateAnnotatedField;
import dev.galasa.framework.spi.IConfigurationPropertyStoreService;
import dev.galasa.framework.spi.IFramework;
import dev.galasa.framework.spi.IManager;
import dev.galasa.framework.spi.ResourceUnavailableException;
import galasa.manager.Account;
import galasa.manager.IAccount;
import galasa.manager.ISimBank;
import galasa.manager.SimBank;
import galasa.manager.SimBankManagerException;
import galasa.manager.spi.ISimBankManagerSpi;

@Component(service = { IManager.class })
public class SimBankManagerImpl extends AbstractManager implements ISimBankManagerSpi {
    
	private static final Log logger = LogFactory.getLog(SimBankManagerImpl.class);

	protected final static String NAMESPACE = "sim";
	private IConfigurationPropertyStoreService cps;
	
	private IZosManagerSpi zosManager;
	private IZos3270ManagerSpi z3270manager;

    @GenerateAnnotatedField(annotation = SimBank.class)
    public ISimBank generateSimBank(Field field, List<Annotation> annotations) throws Zos3270ManagerException {
		SimBank bankAnnotation = field.getAnnotation(SimBank.class);
		String tag = defaultString(bankAnnotation.imageTag(), "primary");

		try {
			IZosImage image = this.zosManager.getImageForTag(tag);
			IIpHost host = image.getIpHost();
			
			getFramework().getConfidentialTextService().registerText("SYS1", "IBMUSER password");

			ISimBank bank = new SimBankImpl(host.getHostname(), getWebnetPort(image), "IBMUSER", "SYS1");  // TODO add to credentials
			return bank;
		} catch(Exception e) {
			throw new Zos3270ManagerException("Unable to generate Bank for zOS Image tagged " + tag, e);
		}
	}
	
	@GenerateAnnotatedField(annotation = Account.class)
    public IAccount generateSimBankAccount(Field field, List<Annotation> annotations) {
        IAccount account = new AccountImpl("123456789");
		return account;
    }

    @Override
    public void provisionGenerate() throws ManagerException, ResourceUnavailableException {
		generateAnnotatedFields(SimBankManagerField.class);
    }
    
    @Override
	public void provisionStop() {
		
    }
    
    @Override
	public void initialise(@NotNull IFramework framework, @NotNull List<IManager> allManagers,
			@NotNull List<IManager> activeManagers, @NotNull Class<?> testClass) throws ManagerException {
		super.initialise(framework, allManagers, activeManagers, testClass);
		List<AnnotatedField> ourFields = findAnnotatedFields(SimBankManagerField.class);
		if (!ourFields.isEmpty()) {
			youAreRequired(allManagers, activeManagers);
		}

		try {
			this.cps = framework.getConfigurationPropertyService(NAMESPACE);
		} catch (Exception e) {
			throw new SimBankManagerException("Unable to request framework services", e);
		}
    }
    
    @Override
	public void youAreRequired(@NotNull List<IManager> allManagers, @NotNull List<IManager> activeManagers)
			throws ManagerException {
		if (activeManagers.contains(this)) {
			return;
		}

		activeManagers.add(this);
		zosManager = addDependentManager(allManagers, activeManagers, IZosManagerSpi.class);
		if (zosManager == null) {
			throw new Zos3270ManagerException("The zOS Manager is not available");
		}
		z3270manager = addDependentManager(allManagers, activeManagers, IZos3270ManagerSpi.class);
		if (z3270manager == null) {
			throw new Zos3270ManagerException("The zOS 3270 Manager is not available");
		}
	}

	@Override
	public boolean areYouProvisionalDependentOn(@NotNull IManager otherManager) {
		if (otherManager instanceof IZosManager || otherManager instanceof IZos3270Manager) {
			return true;
		}

		return super.areYouProvisionalDependentOn(otherManager);
	}

	public int getWebnetPort(IZosImage image) throws SimBankManagerException {
		try {
			String temp = AbstractManager.defaultString(this.cps.getProperty("image" , "webnet.port", image.getImageID()), "2080");
			return Integer.parseInt(temp);
		} catch(Exception e) {
			throw new SimBankManagerException("Unable to find webnet port in CPS", e);
		}
	}
}