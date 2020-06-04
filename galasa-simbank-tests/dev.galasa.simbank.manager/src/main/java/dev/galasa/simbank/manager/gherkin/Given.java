package dev.galasa.simbank.manager.gherkin;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Given {

    String regex() default "";

    String type() default "";

    String dependencies() default "";

    String codeImports() default "";
    
}
