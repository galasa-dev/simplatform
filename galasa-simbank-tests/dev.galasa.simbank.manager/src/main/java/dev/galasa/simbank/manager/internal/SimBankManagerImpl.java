/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.simbank.manager.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.annotations.Component;

import dev.galasa.ManagerException;
import dev.galasa.framework.spi.AbstractManager;
import dev.galasa.framework.spi.AnnotatedField;
import dev.galasa.framework.spi.GenerateAnnotatedField;
import dev.galasa.framework.spi.IConfigurationPropertyStoreService;
import dev.galasa.framework.spi.IDynamicStatusStoreService;
import dev.galasa.framework.spi.IFramework;
import dev.galasa.framework.spi.IManager;
import dev.galasa.framework.spi.ResourceUnavailableException;
import dev.galasa.simbank.manager.Account;
import dev.galasa.simbank.manager.IAccount;
import dev.galasa.simbank.manager.ISimBank;
import dev.galasa.simbank.manager.ISimBankTerminal;
import dev.galasa.simbank.manager.SimBank;
import dev.galasa.simbank.manager.SimBankManagerException;
import dev.galasa.simbank.manager.SimBankTerminal;
import dev.galasa.simbank.manager.internal.properties.SimBankDseInstanceName;
import dev.galasa.simbank.manager.internal.properties.SimBankPropertiesSingleton;
import dev.galasa.simbank.manager.spi.ISimBankManagerSpi;
import dev.galasa.zos.IZosImage;
import dev.galasa.zos.IZosManager;
import dev.galasa.zos.spi.IZosManagerSpi;
import dev.galasa.zos3270.IZos3270Manager;
import dev.galasa.zos3270.Zos3270ManagerException;
import dev.galasa.zos3270.spi.IZos3270ManagerSpi;

@Component(service = { IManager.class })
public class SimBankManagerImpl extends AbstractManager implements ISimBankManagerSpi {

    private static final Log                   logger        = LogFactory.getLog(SimBankManagerImpl.class);

    public final static String                 NAMESPACE     = "simbank";
    private IConfigurationPropertyStoreService cps;
    private IDynamicStatusStoreService         dss;

    private IZosManagerSpi                     zosManager;
    private IZos3270ManagerSpi                 z3270manager;

    private SimBankImpl                        simBankSingleInstance;

    private HashMap<String, AccountImpl>       accounts      = new HashMap<>();

    private int                                terminalCount = 0;
    private ArrayList<SimBankTerminalImpl>     terminals     = new ArrayList<>();

    @Override
    public void provisionGenerate() throws ManagerException, ResourceUnavailableException {
        // *** First locate the SimBank annotations and provision, before anything else
        // id done

        List<AnnotatedField> foundAnnotatedFields = findAnnotatedFields(SimBankManagerField.class);
        for (AnnotatedField annotatedField : foundAnnotatedFields) {
            Field field = annotatedField.getField();
            List<Annotation> annotations = annotatedField.getAnnotations();

            if (field.getType() == ISimBank.class) {
                SimBank annotation = field.getAnnotation(SimBank.class);
                if (annotation != null) {
                    ISimBank simBank = generateSimBank(field, annotations);
                    registerAnnotatedField(field, simBank);
                }
            }
        }

        // *** Now generate the rest of the fields
        generateAnnotatedFields(SimBankManagerField.class);
    }

    @GenerateAnnotatedField(annotation = SimBank.class)
    public ISimBank generateSimBank(Field field, List<Annotation> annotations) throws SimBankManagerException {
        SimBank bankAnnotation = field.getAnnotation(SimBank.class);

        if (simBankSingleInstance != null) {
            return simBankSingleInstance;
        }

        try {
            // *** Check to see if we have a dse for this tag
            String dseInstanceName = SimBankDseInstanceName.get();
            if (dseInstanceName == null) {
                throw new SimBankManagerException(
                        "The SimBank Manager does not support full provisioning at the moment, provide the DSE property sim.dse.instance.name to indicate the SimBank instance to run against");
            }

            // *** Retrieve the SimBank instance for the DSE tag
            SimBankImpl simBank = SimBankImpl.getDSE(this, dseInstanceName, bankAnnotation.useTerminal(),
                    bankAnnotation.useJdbc());
            this.simBankSingleInstance = simBank;

            logger.info("SimBank instance " + simBank.getInstanceId() + " provisioned for this run");

            return simBank;
        } catch (SimBankManagerException e) {
            throw e;
        } catch (Exception e) {
            throw new SimBankManagerException("Unable to generate Sim Bank", e);
        }
    }

    @GenerateAnnotatedField(annotation = Account.class)
    public IAccount generateSimBankAccount(Field field, List<Annotation> annotations) throws SimBankManagerException {
        Account accountAnnotation = field.getAnnotation(Account.class);
        AccountImpl account = AccountImpl.generate(this, accountAnnotation.existing(), accountAnnotation.accountType(), accountAnnotation.balance());

        if (simBankSingleInstance == null) {
            throw new SimBankManagerException("An instance of the SimBank has not been requested");
        }
        accounts.put(account.getAccountNumber(), account);

        logger.info("Provisioned account " + account.getAccountNumber());
        return account;
    }

    @GenerateAnnotatedField(annotation = SimBankTerminal.class)
    public ISimBankTerminal generateSimBankTerminal(Field field, List<Annotation> annotations)
            throws SimBankManagerException {
        if (simBankSingleInstance == null) {
            throw new SimBankManagerException("An instance of the SimBank has not been requested");
        }

        terminalCount++;
        SimBankTerminalImpl newTerminal = simBankSingleInstance.allocateTerminal(terminalCount);
        terminals.add(newTerminal);

        logger.info("Provisioned SimBank terminal " + newTerminal.getId());
        return newTerminal;
    }

    @Override
    public void provisionStart() throws ManagerException, ResourceUnavailableException {
        if (this.simBankSingleInstance != null) {
            this.simBankSingleInstance.start();
        }
    }

    @Override
    public void provisionDiscard() {

        try {
            for (AccountImpl account : accounts.values()) {
                account.discard();
            }

            for (SimBankTerminalImpl terminal : terminals) {
                terminal.flushTerminalCache();
                terminal.disconnect();
            }

            if (this.simBankSingleInstance != null) {
                this.simBankSingleInstance.discard();
            }
        } catch (InterruptedException e) {
            logger.error("Discard interrupted", e);
            Thread.currentThread().interrupt();
        }
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
            this.dss = framework.getDynamicStatusStoreService(NAMESPACE);
            SimBankPropertiesSingleton.setCps(cps);
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
            String temp = AbstractManager
                    .defaultString(this.cps.getProperty("image", "webnet.port", image.getImageID()), "2080");
            return Integer.parseInt(temp);
        } catch (Exception e) {
            throw new SimBankManagerException("Unable to find webnet port in CPS", e);
        }
    }

    public IZosManagerSpi getZosManager() {
        return zosManager;
    }

    public SimBankImpl getSimBank() {
        return this.simBankSingleInstance;
    }

    public IDynamicStatusStoreService getDSS() {
        return this.dss;
    }

    public IConfigurationPropertyStoreService getCPS() {
        return this.cps;
    }

    public void registerTerminal(SimBankTerminalImpl terminal) {
        this.terminals.add(terminal);
    }
}
