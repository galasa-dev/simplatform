package galasa.manager;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import dev.galasa.framework.spi.ValidAnnotatedFields;
import galasa.manager.internal.SimBankManagerField;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@SimBankManagerField
@ValidAnnotatedFields({ ISimBankTerminal.class })
public @interface SimBankTerminal {
	
}