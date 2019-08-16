package galasa.manager.bankaccount;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

import dev.galasa.framework.spi.ValidAnnotatedFields;
import galasa.manager.simbank.SimBankManagerField;
import galasa.manager.bankaccount.ISimBankAccount;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@SimBankManagerField
@ValidAnnotatedFields({ ISimBankAccount.class })
public @interface SimframeBankAccount {

}