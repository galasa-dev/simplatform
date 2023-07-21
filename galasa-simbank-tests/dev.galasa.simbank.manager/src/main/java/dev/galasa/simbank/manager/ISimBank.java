/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.simbank.manager;

public interface ISimBank {

    public String getHost();

    public int getWebnetPort();

    public String getFullAddress();

    public String getUpdateAddress();

}
