/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2020.
 */
package dev.galasa.simbank.manager;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

import dev.galasa.framework.spi.ValidAnnotatedFields;
import dev.galasa.simbank.manager.ISimBankWebApp;
import dev.galasa.simbank.manager.internal.SimBankManagerField;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
@SimBankManagerField
@ValidAnnotatedFields({ ISimBankWebApp.class })
public @interface SimBankWebApp {


}
