// 
// Decompiled by Procyon v0.5.36
// 

package org.bukkit.plugin.java.annotation.permission;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Retention;
import java.lang.annotation.Documented;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface Permissions {
    Permission[] value() default {};
}
