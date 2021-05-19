/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.simplatform.saf;

import java.util.HashMap;
import java.util.logging.Logger;

public class SecurityAuthorizationFacility {

    private static HashMap<String, User> accounts = null;
    private Logger                       log;

    public SecurityAuthorizationFacility() {
        log = Logger.getLogger("Simplatform");
        if (accounts == null) {
            log.info("Creating SAF service");
            accounts = new HashMap<>();
        }

    }
    
    //In this demo only alphanumeric user and passwords are allowed
    private String validateString(String s) {
    	return s.replaceAll("[^a-zA-Z0-9]", "");
    }

    public boolean addUser(String user, String pass) {
    	user = validateString(user);
    	pass = validateString(pass);
        if (accounts.get(user) != null)
            return false;
        accounts.put(user, new User(user, pass));
        log.info("Added user: " + user);
        return true;
    }

    public boolean authenticate(String user, String pass) {
    	user = validateString(user);
    	pass = validateString(pass);
        if (accounts.get(user) == null) {
            log.info("User: " + user + " NOT authenticated");
            return false;
        } else {
            if (accounts.get(user).authenticate(pass)) {
                log.info("User: " + user + " authenticated");
                return true;
            } else {
                log.info("User: " + user + " NOT authenticated");
                return false;
            }
        }
    }

}
;