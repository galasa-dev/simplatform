/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.simbank.manager;

import dev.galasa.ManagerException;

public class SimBankManagerException extends ManagerException {
    private static final long serialVersionUID = 1L;

    public SimBankManagerException() {
    }

    public SimBankManagerException(String message) {
        super(message);
    }

    public SimBankManagerException(Throwable cause) {
        super(cause);
    }

    public SimBankManagerException(String message, Throwable cause) {
        super(message, cause);
    }

    public SimBankManagerException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
