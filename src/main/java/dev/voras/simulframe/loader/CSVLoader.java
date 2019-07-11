package dev.voras.simulframe.loader;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.StringTokenizer;

import dev.voras.simulframe.application.Bank;

public class CSVLoader {
	
	public static void load(String path){
		Bank bank = Bank.getBank();
		
		List<String> inputData;
		try {
			inputData = Files.readAllLines(Paths.get(path));
		}catch(Exception e) {
			System.out.println("Unable to read file" + path);
			return;
		}
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

}
