/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.simbank;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class SimBankActivator extends AbstractUIPlugin {

    public static final String      PLUGIN_ID   = "dev.galasa.smbank.ui"; //$NON-NLS-1$
    public static final String      PLUGIN_NAME = "Galasa:SimBank";       //$NON-NLS-1$

    private static SimBankActivator INSTANCE;

    public SimBankActivator() {
        INSTANCE = this;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
    }

    public static SimBankActivator getInstance() {
        return INSTANCE;
    }

    /**
     * Log a throwable
     * 
     * @param e
     */
    public static void log(Throwable e) {
        log(new Status(IStatus.ERROR, getPluginId(), IStatus.ERROR, "Error", e)); //$NON-NLS-1$
    }

    /**
     * Log a status
     * 
     * @param status
     */
    public static void log(IStatus status) {
        ILog log = getInstance().getLog();
        if (log != null) {
            log.log(status);
        }
    }

    /**
     * 
     * @return - plugin ID
     */
    public static String getPluginId() {
        return PLUGIN_ID;
    }
}
