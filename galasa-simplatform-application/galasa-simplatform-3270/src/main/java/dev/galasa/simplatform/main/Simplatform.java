/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.simplatform.main;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.derby.drda.NetworkServerControl;
import org.w3c.dom.Document;

import dev.galasa.simplatform.listener.Listener;
import dev.galasa.simplatform.listener.ManagementFacilityListener;
import dev.galasa.simplatform.listener.TelnetServiceListener;
import dev.galasa.simplatform.listener.WebServiceListener;
import dev.galasa.simplatform.loader.CSVLoader;

public class Simplatform {
	
	private static String version;
	
	static {
		version = Simplatform.class.getPackage().getImplementationVersion();
		if (version == null ) {
			try {
				Path pomFile = Paths.get(System.getProperty("user.dir") + File.separator + "pom.xml");
				InputStream inputStream = Files.newInputStream(pomFile);
				Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream);
				document.getDocumentElement().normalize();
				version = (String) XPathFactory.newInstance().newXPath().compile("/project/version").evaluate(document, XPathConstants.STRING);
			} catch (Exception e) {
				// NOOP
			}
		}
		if (version != null) {
			version = " version " + version;
		} else {
			version = "";
		}
	}
	
	public static String getVersion() {
		return version;
	}
		

    public static void main(String[] args) throws IOException {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "%1$td/%1$tm/%1$tY %1$tH:%1$tM:%1$tS.%1$tL %4$s %2$s %5$s%6$s%n");

        Logger log = Logger.getLogger("Simplatform");
        log.addHandler(new ConsoleHandler() {
            {
                setOutputStream(System.out);
            }
        });

        log.info(() -> String.format("Starting Simplatform %1$s ...", version));

        CSVLoader.load(null, null);

        log.info("Loading services...");

        List<Listener> listeners = new ArrayList<>();

        listeners.add(new Listener(2080, WebServiceListener.class.getName()));
        listeners.add(new Listener(2023, TelnetServiceListener.class.getName()));
        listeners.add(new Listener(2040, ManagementFacilityListener.class.getName()));

        log.info("... services loaded");

        for (Listener l : listeners) {
            new Thread(l).start();
        }

        log.info("Starting Derby Network server....");

        try {
            InetAddress addr = InetAddress.getByName("0.0.0.0");
            NetworkServerControl server = new NetworkServerControl(addr, 2027);
            server.start(null);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to start the Derby server", e);
        }

        log.info("... Derby Network server started on port 2027");

        log.info("... Simplatform started");
    }

}
