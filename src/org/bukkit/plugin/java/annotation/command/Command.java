// 
// Decompiled by Procyon v0.5.36
// 

package org.bukkit.plugin.java.annotation.command;

import java.lang.annotation.Repeatable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Retention;
import java.lang.annotation.Documented;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
@Repeatable(Commands.class)
public @interface Command {
    String name();
    
    String desc() default "";
    
    String[] aliases() default {};
    
    String permission() default "";
    
    String permissionMessage() default "";
    
    String usage() default "";
}
