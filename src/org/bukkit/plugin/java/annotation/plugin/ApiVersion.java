// 
// Decompiled by Procyon v0.5.36
// 

package org.bukkit.plugin.java.annotation.plugin;

import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Retention;
import java.lang.annotation.Documented;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@java.lang.annotation.Target({ ElementType.TYPE })
public @interface ApiVersion {
    Target value() default Target.DEFAULT;
    
    public enum Target
    {
        DEFAULT("DEFAULT", 0, (String)null), 
        v1_13("v1_13", 1, "1.13"), 
        v1_14("v1_14", 2, "1.14"), 
        v1_15("v1_15", 3, "1.15"),
        v1_16("v1_16", 4, "1.16");
        
        private final String version;
        
        private Target(final String name, final int ordinal, final String version) {
            this.version = version;
        }
        
        public String getVersion() {
            return this.version;
        }
    }
}
