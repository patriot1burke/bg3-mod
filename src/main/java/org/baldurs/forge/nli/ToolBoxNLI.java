package org.baldurs.forge.nli;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface ToolBoxNLI {
    /**
     * Tool classes
     * @return
     */
    Class<?>[] value();
}
