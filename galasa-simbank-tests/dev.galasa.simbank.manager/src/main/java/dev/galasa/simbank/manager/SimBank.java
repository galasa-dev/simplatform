/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.simbank.manager;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

import dev.galasa.framework.spi.ValidAnnotatedFields;
import dev.galasa.simbank.manager.internal.SimBankManagerField;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
@SimBankManagerField
@ValidAnnotatedFields({ ISimBank.class })
public @interface SimBank {

    boolean useTerminal() default true;

    boolean useJdbc() default true;

}
