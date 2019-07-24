package dev.voras.simulframe.main;

import java.util.ArrayList;
import java.util.List;

import dev.voras.simulframe.application.Bank;
import dev.voras.simulframe.listener.Listener;
import dev.voras.simulframe.listener.TelnetServiceListener;
import dev.voras.simulframe.listener.WebServiceListener;
import dev.voras.simulframe.loader.CSVLoader;

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
