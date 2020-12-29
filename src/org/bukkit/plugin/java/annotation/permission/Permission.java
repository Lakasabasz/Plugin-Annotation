// 
// Decompiled by Procyon v0.5.36
// 

package org.bukkit.plugin.java.annotation.permission;

import org.bukkit.permissions.PermissionDefault;
import java.lang.annotation.Repeatable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Retention;
import java.lang.annotation.Documented;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
@Repeatable(Permissions.class)
public @interface Permission {
    String name();
    
    String desc() default "";
    
    PermissionDefault defaultValue() default PermissionDefault.OP;
    
    ChildPermission[] children() default {};
}
