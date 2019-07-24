package dev.galasa.simframe.main;

import java.util.ArrayList;
import java.util.List;

import dev.galasa.simframe.application.Bank;
import dev.galasa.simframe.listener.Listener;
import dev.galasa.simframe.listener.TelnetServiceListener;
import dev.galasa.simframe.listener.WebServiceListener;
import dev.galasa.simframe.loader.CSVLoader;

public class Simulframe {

	public static void main(String[] args) {
		System.out.println("Starting Simulframe ...");
			
		Bank b = Bank.getBank();
		
		CSVLoader.load(null);
		
		System.out.println("Loading services");
		
		List<Listener> listeners = new ArrayList<>();
		
		listeners.add(new Listener(2080, WebServiceListener.class.getName()));
		listeners.add(new Listener(2023, TelnetServiceListener.class.getName()));
		
		for(Listener l : listeners)	
			new Thread(l).start();

	}

}
