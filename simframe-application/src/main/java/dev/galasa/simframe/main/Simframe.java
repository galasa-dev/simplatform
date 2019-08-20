package dev.galasa.simframe.main;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;

import dev.galasa.simframe.listener.Listener;
import dev.galasa.simframe.listener.TelnetServiceListener;
import dev.galasa.simframe.listener.WebServiceListener;
import dev.galasa.simframe.loader.CSVLoader;

public class Simframe {

	public static void main(String[] args) {
		System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$s %2$s %5$s%6$s%n");
		
		Logger log = Logger.getLogger("Simframe");
		log.addHandler(new ConsoleHandler() {
		    {setOutputStream(System.out);}
		});
		
		log.info("Starting Simframe ...");
		
		CSVLoader.load(null,null);

		log.info("Loading services...");
		
		List<Listener> listeners = new ArrayList<>();
		
		listeners.add(new Listener(2080, WebServiceListener.class.getName()));
		listeners.add(new Listener(2023, TelnetServiceListener.class.getName()));
		
		log.info("... services loaded");
		
		for(Listener l : listeners)	
			new Thread(l).start();

	}

}