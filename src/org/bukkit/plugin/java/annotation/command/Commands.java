// 
// Decompiled by Procyon v0.5.36
// 

package org.bukkit.plugin.java.annotation.command;

import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Retention;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.annotation.Documented;

@Documented
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface Commands {
    Command[] value() default {};
}
