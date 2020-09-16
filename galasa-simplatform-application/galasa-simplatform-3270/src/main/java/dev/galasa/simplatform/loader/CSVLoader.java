/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.simplatform.loader;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import dev.galasa.simplatform.application.Bank;
import dev.galasa.simplatform.saf.SecurityAuthorizationFacility;

public class CSVLoader {

    private static Logger log;

    public static void load(String pathToAccounts, String pathToSecurity) {
        Bank bank = new Bank();
        SecurityAuthorizationFacility saf = new SecurityAuthorizationFacility();
        log = Logger.getLogger("Simplatform");

        List<String> inputData;
        try {
            if (pathToAccounts == null) {
                inputData = readInternalFile("/accounts.csv");
            } else {
                inputData = Files.readAllLines(Paths.get(pathToAccounts));
            }
        } catch (Exception e) {
            log.severe("Unable to read file:  " + pathToAccounts);
            log.severe(e.getMessage());
            return;
        }
        parseBankAccounts(bank, inputData);

        inputData.clear();
        try {
            if (pathToSecurity == null) {
                inputData = readInternalFile("/security.csv");
            } else {
                inputData = Files.readAllLines(Paths.get(pathToSecurity));
            }
        } catch (Exception e) {
            log.severe("Unable to read file:  " + pathToSecurity);
            log.severe(e.getMessage());
            return;
        }
        parseSecurityAccounts(saf, inputData);

    }

    private static void parseBankAccounts(Bank bank, List<String> inputData) {
        for (String s : inputData) {
            StringTokenizer token = new StringTokenizer(s, ",");
            while (token.hasMoreTokens()) {
                try {
                    bank.openAccount(token.nextToken(), token.nextToken(), Double.parseDouble(token.nextToken()));
                } catch (Exception e) {
                    continue;
                }

            }
        }
    }

    private static void parseSecurityAccounts(SecurityAuthorizationFacility saf, List<String> inputData) {
        for (String s : inputData) {
            StringTokenizer token = new StringTokenizer(s, ",");
            while (token.hasMoreTokens()) {
                try {
                    String user = token.nextToken();
                    String password = token.nextToken();
                    saf.addUser(user, password);
                } catch (Exception e) {
                    continue;
                }

            }
        }
    }

    private static List<String> readInternalFile(String path) throws Exception {
        List<String> inputData = new ArrayList<>();
        BufferedReader br = new BufferedReader(new InputStreamReader(CSVLoader.class.getResourceAsStream(path)));
        String line = null;
        while ((line = br.readLine()) != null) {
            inputData.add(line);
        }
        return inputData;
    }
}
