package dev.voras.simulframe.main;

import dev.voras.simulframe.application.Bank;
import dev.voras.simulframe.listener.Listener;
import dev.voras.simulframe.loader.CSVLoader;

public class Simulframe {

	public static void main(String[] args) {
		System.out.println("Starting Simulframe ...");
			
		Bank b = Bank.getBank();
		
		CSVLoader.load("accounts.csv");
		
		System.out.println("Loading services");
		
		Listener  l = new Listener(80, "dev.voras.simulframe.listener.WebServiceListener");
		new Thread(l).start();
		
		System.out.println("Loading services");

	}

}
