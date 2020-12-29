// 
// Decompiled by Procyon v0.5.36
// 

package org.bukkit.plugin.java.annotation.dependency;

import java.lang.annotation.Repeatable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Retention;
import java.lang.annotation.Documented;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
@Repeatable(LoadBeforePlugins.class)
public @interface LoadBefore {
    String value();
}
