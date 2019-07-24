package dev.galasa.simframe.main;

import java.util.ArrayList;
import java.util.List;

import dev.galasa.simframe.application.Bank;
import dev.galasa.simframe.listener.Listener;
import dev.galasa.simframe.listener.TelnetServiceListener;
import dev.galasa.simframe.listener.WebServiceListener;
import dev.galasa.simframe.loader.CSVLoader;

public class Simframe {

	public static void main(String[] args) {
		System.out.println("Starting Simframe ...");
			
		Bank b = Bank.getBank();
		
		CSVLoader.load(null);
		
		System.out.println("Loading services...");
		
		List<Listener> listeners = new ArrayList<>();
		
		listeners.add(new Listener(2080, WebServiceListener.class.getName()));
		listeners.add(new Listener(2023, TelnetServiceListener.class.getName()));
		
		System.out.println("... services loaded");
		
		for(Listener l : listeners)	
			new Thread(l).start();

	}

}
