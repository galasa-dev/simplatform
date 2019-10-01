package dev.galasa.simplatform.main;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.derby.drda.NetworkServerControl;

import dev.galasa.simplatform.listener.Listener;
import dev.galasa.simplatform.listener.TelnetServiceListener;
import dev.galasa.simplatform.listener.WebServiceListener;
import dev.galasa.simplatform.loader.CSVLoader;

public class Simplatform {

	public static void main(String[] args) {
		System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$s %2$s %5$s%6$s%n");

		Logger log = Logger.getLogger("Simplatform");
		log.addHandler(new ConsoleHandler() {
			{setOutputStream(System.out);}
		});

		log.info("Starting Simplatform ...");

		CSVLoader.load(null,null);

		log.info("Loading services...");

		List<Listener> listeners = new ArrayList<>();

		listeners.add(new Listener(2080, WebServiceListener.class.getName()));
		listeners.add(new Listener(2023, TelnetServiceListener.class.getName()));

		log.info("... services loaded");

		for(Listener l : listeners) {	
			new Thread(l).start();
		}

		log.info("Starting Derby Network server....");

		try {
			InetAddress addr = Inet4Address.getByName("0.0.0.0");
			NetworkServerControl server = new NetworkServerControl(addr, 2027);
			server.start (null);
		} catch(Exception e) {
			log.log(Level.SEVERE, "Failed to start the Derby server",e);
		}
		
		log.info("... Derby Network server started on port 2027");
		
		log.info("... Simplatform started");
	}

}
