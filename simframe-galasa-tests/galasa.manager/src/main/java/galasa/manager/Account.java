package galasa.manager;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

import dev.galasa.framework.spi.ValidAnnotatedFields;
import galasa.manager.internal.SimBankManagerField;
import galasa.manager.IAccount;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@SimBankManagerField
@ValidAnnotatedFields({ IAccount.class })
public @interface Account {

}