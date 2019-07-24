package dev.voras.simframe.main;

import java.util.ArrayList;
import java.util.List;

import dev.voras.simframe.application.Bank;
import dev.voras.simframe.listener.Listener;
import dev.voras.simframe.listener.TelnetServiceListener;
import dev.voras.simframe.listener.WebServiceListener;
import dev.voras.simframe.loader.CSVLoader;

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
