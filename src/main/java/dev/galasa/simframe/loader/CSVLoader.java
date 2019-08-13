package dev.galasa.simframe.loader;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import dev.galasa.simframe.application.Bank;
import dev.galasa.simframe.saf.SecurityAuthorizationFacility;

public class CSVLoader {

	public static void load(String pathToAccounts, String pathToSecurity){
		Bank bank = Bank.getBank();
		SecurityAuthorizationFacility saf = new SecurityAuthorizationFacility();
		Logger log = Logger.getLogger("Simframe");

		List<String> inputData;
		try {
			if (pathToAccounts == null) {
				inputData = readInternalFile("/accounts.csv");
			} else {
				inputData = Files.readAllLines(Paths.get(pathToAccounts));
			}
		}catch(Exception e) {
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
		}catch(Exception e) {
			log.severe("Unable to read file:  " + pathToSecurity);
			log.severe(e.getMessage());
			return;
		}
		parseSecurityAccounts(saf, inputData);
		

	}
	
	private static void parseBankAccounts(Bank bank, List<String> inputData) {
		for(String s : inputData) {
			StringTokenizer token = new StringTokenizer(s, ",");
			while(token.hasMoreTokens()) {
				try {
					bank.openAccount(token.nextToken(), token.nextToken(), Double.parseDouble(token.nextToken()));
				}catch(Exception e) {
					continue;
				}

			}
		}
	}
	
	private static void parseSecurityAccounts(SecurityAuthorizationFacility saf, List<String> inputData) {
		for(String s : inputData) {
			StringTokenizer token = new StringTokenizer(s, ",");
			while(token.hasMoreTokens()) {
				try {
					saf.addUser(token.nextToken(), token.nextToken());
				}catch(Exception e) {
					continue;
				}

			}
		}
	}
	
	private static List<String> readInternalFile(String path) throws Exception{
		List<String> inputData = new ArrayList<>();
		BufferedReader br = new BufferedReader(new InputStreamReader(CSVLoader.class.getResourceAsStream(path)));
		String line = null;
		while((line = br.readLine()) != null) {
			inputData.add(line);
		}
		return inputData;
	}
}
