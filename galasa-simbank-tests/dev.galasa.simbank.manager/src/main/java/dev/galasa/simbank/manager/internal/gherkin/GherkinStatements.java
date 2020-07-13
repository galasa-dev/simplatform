package dev.galasa.simbank.manager.internal.gherkin;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.galasa.ManagerException;
import dev.galasa.framework.TestRunException;
import dev.galasa.framework.spi.IGherkinExecutable;
import dev.galasa.framework.spi.IGherkinManager;
import dev.galasa.framework.spi.IManager;
import dev.galasa.framework.spi.language.GalasaTest;
import dev.galasa.simbank.manager.internal.SimBankManagerImpl;

public class GherkinStatements {

    public static void register(GalasaTest galasaTest, SimBankManagerImpl manager, List<IManager> allManagers, List<IManager> activeManagers) throws ManagerException {
		for(IGherkinExecutable gherkinExecutable : galasaTest.getGherkinTest().getAllExecutables()) {
            switch (gherkinExecutable.getKeyword()) {
                case GIVEN:
                    match(GherkinSimbank.pattern, gherkinExecutable, manager, allManagers, activeManagers, GherkinSimbank.dependencies);
                    match(GherkinAccount.pattern, gherkinExecutable, manager, allManagers, activeManagers, GherkinAccount.dependencies);
                    break;
                case WHEN:
                    match(GherkinNavigateBank.pattern, gherkinExecutable, manager, allManagers, activeManagers, GherkinNavigateBank.dependencies);
                    match(GherkinWebApi.pattern, gherkinExecutable, manager, allManagers, activeManagers, GherkinWebApi.dependencies);
                    break;
                case THEN:
                    match(GherkinMainMenu.pattern, gherkinExecutable, manager, allManagers, activeManagers, GherkinMainMenu.dependencies);
                    match(GherkinAccountBalance.pattern, gherkinExecutable, manager, allManagers, activeManagers, GherkinAccountBalance.dependencies);
                    break;
                default:
                    break;
            }
        }
    }
    
    private static void match(Pattern regexPattern, IGherkinExecutable gherkinExecutable, SimBankManagerImpl manager,
            List<IManager> allManagers, List<IManager> activeManagers, Class<?>[] dependencies) throws ManagerException {
        Matcher gherkinMatcher = regexPattern.matcher(gherkinExecutable.getValue());
        if(gherkinMatcher.matches()) {
            try {
                manager.youAreRequired(allManagers, activeManagers);
                gherkinExecutable.registerManager((IGherkinManager) manager);
                for(Class<?> dependencyManager : dependencies) {
                    for (IManager otherManager : allManagers) {
                        if (dependencyManager.isAssignableFrom(otherManager.getClass())) {
                            otherManager.youAreRequired(allManagers, activeManagers);
                        }
                    }
                }
            } catch (TestRunException e) {
                throw new ManagerException("Unable to register Manager for Gherkin Statement", e);
            }
        }
    }
    
}