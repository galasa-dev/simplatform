package galasa.manager.internal.resourcemanagement;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dev.galasa.framework.spi.AbstractManager;
import dev.galasa.framework.spi.DynamicStatusStoreException;
import dev.galasa.framework.spi.IDynamicStatusStoreService;
import dev.galasa.framework.spi.IFramework;
import dev.galasa.framework.spi.IResourceManagement;
import galasa.manager.SimBankManagerException;
import galasa.manager.internal.AccountImpl;

public class AccountResourceMonitor implements Runnable {

	private final IFramework                 framework;
	private final IResourceManagement        resourceManagement;
	private final IDynamicStatusStoreService dss;
	private final Log                        logger = LogFactory.getLog(this.getClass());

	private final Pattern                    serverPattern = Pattern.compile("^run\\.(\\w+)\\.instance\\.(\\w+)\\.account\\.(\\w+)$");

	public AccountResourceMonitor(IFramework framework, 
			IResourceManagement resourceManagement,
			IDynamicStatusStoreService dss) {
		this.framework          = framework;
		this.resourceManagement = resourceManagement;
		this.dss                = dss;
		this.logger.info("SimBank Account resource monitor initialised");
	}

	@Override
	public void run() {
		logger.info("Starting SimBank Account search");
		try {
			//*** Find all the runs with slots
			Map<String, String> computeServers = dss.getPrefix("run.");

			Set<String> activeRunNames = this.framework.getFrameworkRuns().getActiveRunNames();

			for(String key : computeServers.keySet()) {
				Matcher matcher = serverPattern.matcher(key);
				if (matcher.find()) {
					String runName       = matcher.group(1);
					String instance      = matcher.group(2);
					String accountNumber = matcher.group(3);

					if (!activeRunNames.contains(runName)) {
						discardAccount(instance, accountNumber, runName);
					}
				}
			}
		} catch(Throwable t) {
			logger.error("Failure during SimBank account scan",t);
		}

		this.resourceManagement.resourceManagementRunSuccessful();
		logger.info("Finished SimBank account search");
	}

	private void discardAccount(String instance, String accountNumber, String runName) throws DynamicStatusStoreException {
		logger.info("Discarding SimBank Account " + accountNumber + " on instance " + instance + " as run " + runName + " has gone");

		try {
			String key = "instance." + instance + ".account." + accountNumber + ".created";
			boolean created = Boolean.parseBoolean(dss.get(key));
			if (created) {
				deleteAccount(instance, accountNumber);
			}
			
			AccountImpl.freeAccount(dss, instance, accountNumber, runName);
		} catch(Throwable e) {
			logger.error("Failure during discard account " + instance + "/" + accountNumber);
		}
	}

	private void deleteAccount(String instance, String accountNumber) {
		logger.trace("Deleting account " + accountNumber + " from instance + " + instance);

		Connection conn = null;
		try {
			String jdbcUri = AbstractManager.nulled(dss.get("instance." + instance + ".account." + accountNumber + ".database"));

			if (jdbcUri == null) {
				throw new SimBankManagerException("Database URI is missing on created account");
			}

			conn = DriverManager.getConnection(jdbcUri.toString(), new Properties());

			AccountImpl.deleteAccount(conn, accountNumber);

		} catch(Exception e) {
			logger.error("Failure during delete account " + instance + "/" + accountNumber);
		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					logger.error("Unable to close JDBC connection",e);
				}
			}
		}
	}

	public void runFinishedOrDeleted(String runName) {
		try {
			Map<String, String> serverRuns = dss.getPrefix("run." + runName + ".");
			for(String key : serverRuns.keySet()) {
				Matcher matcher = serverPattern.matcher(key);
				if (matcher.find()) {
					String instance      = matcher.group(2);
					String accountNumber = matcher.group(3);

					discardAccount(instance, accountNumber, runName);

					//					try {
					//						OpenstackServerImpl.deleteServerByName(serverName, runName, dss, this.openstackHttpClient);
					//					} catch(Exception e) {
					//						logger.error("Failed to discard OpenStack server " + serverName + " for run " + runName);
					//					}
				}
			}
		} catch(Exception e) {
			logger.error("Failed to discard SimBank account for run " + runName);
		}
	}


}
