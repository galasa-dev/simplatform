/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.simbank.manager;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.math.BigDecimal;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

import dev.galasa.framework.spi.ValidAnnotatedFields;
import dev.galasa.simbank.manager.IAccount;
import dev.galasa.simbank.manager.internal.SimBankManagerField;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
@SimBankManagerField
@ValidAnnotatedFields({ IAccount.class })
public @interface Account {

    boolean existing() default true;

    AccountType accountType() default AccountType.HighValue;

}
