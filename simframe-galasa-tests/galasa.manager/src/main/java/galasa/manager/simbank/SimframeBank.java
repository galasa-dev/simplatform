package galasa.manager.simbank;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

import dev.galasa.framework.spi.ValidAnnotatedFields;
import galasa.manager.simbank.SimBankManagerField;
import galasa.manager.simbank.ISimBank;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@SimBankManagerField
@ValidAnnotatedFields({ ISimBank.class })
public @interface SimframeBank {

}