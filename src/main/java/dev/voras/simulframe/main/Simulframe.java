package dev.voras.simulframe.main;

import dev.voras.simulframe.application.Bank;
import dev.voras.simulframe.loader.CSVLoader;

public class Simulframe {

	public static void main(String[] args) {
		System.out.println("Starting Simulframe ...");
			
		Bank b = Bank.getBank();
		
		CSVLoader.load("data.csv");
		
		System.out.println("Loading services");

	}

}
