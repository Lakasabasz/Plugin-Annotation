// 
// Decompiled by Procyon v0.5.36
// 

package org.bukkit.plugin.java.annotation.permission;

import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Retention;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.annotation.Documented;

@Documented
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ChildPermission {
    boolean inherit() default true;
    
    String name();
}
