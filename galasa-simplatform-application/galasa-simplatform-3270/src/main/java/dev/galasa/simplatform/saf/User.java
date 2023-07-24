/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.simplatform.saf;

public class User {
    private String userName;
    private String password;

    public User(String user, String pass) {
        this.userName = user;
        this.password = pass;
    }

    public String getUserName() {
        return this.userName;
    }

    public boolean authenticate(String password) {
        return this.password.equals(password);
    }
}
