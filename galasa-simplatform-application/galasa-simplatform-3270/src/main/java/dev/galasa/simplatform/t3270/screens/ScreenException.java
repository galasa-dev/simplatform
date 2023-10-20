/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.simplatform.t3270.screens;

public class ScreenException extends Exception {
    private static final long serialVersionUID = 1L;

    public ScreenException() {
    }

    public ScreenException(String message) {
        super(message);
    }

    public ScreenException(Throwable cause) {
        super(cause);
    }

    public ScreenException(String message, Throwable cause) {
        super(message, cause);
    }

    public ScreenException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
