package dev.galasa.simbank.manager.ghrekin;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface When {

    String regex();

    String type();
    
}
