// 
// Decompiled by Procyon v0.5.36
// 

package org.bukkit.plugin.java.annotation;

import org.bukkit.plugin.java.annotation.permission.ChildPermission;
import org.bukkit.permissions.PermissionDefault;
import java.lang.reflect.Method;
import javax.tools.Diagnostic;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.type.TypeMirror;
import org.bukkit.command.CommandExecutor;
import java.io.Writer;
import javax.tools.FileObject;
import java.util.List;
import java.io.IOException;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.nodes.Tag;
import java.time.LocalDateTime;
import javax.tools.StandardLocation;
import org.yaml.snakeyaml.Yaml;
import org.bukkit.plugin.java.annotation.plugin.ApiVersion;
import org.bukkit.plugin.java.annotation.permission.Permissions;
import org.bukkit.plugin.java.annotation.permission.Permission;
import org.bukkit.plugin.java.annotation.command.Command;
import java.util.Map;
import org.bukkit.plugin.java.annotation.command.Commands;
import org.bukkit.plugin.java.annotation.dependency.LoadBefore;
import org.bukkit.plugin.java.annotation.dependency.SoftDependency;
import org.bukkit.plugin.java.annotation.dependency.Dependency;
import org.bukkit.plugin.java.annotation.plugin.LogPrefix;
import org.bukkit.plugin.java.annotation.plugin.Website;
import com.google.common.collect.Lists;
import org.bukkit.plugin.java.annotation.plugin.author.Author;
import org.bukkit.plugin.java.annotation.plugin.LoadOrder;
import org.bukkit.plugin.java.annotation.plugin.Description;
import com.google.common.collect.Maps;
import org.bukkit.plugin.java.JavaPlugin;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.Element;
import java.lang.annotation.Annotation;
import org.bukkit.plugin.java.annotation.plugin.Plugin;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;
import java.util.Set;
import java.util.Locale;
import java.time.format.DateTimeFormatter;
import javax.lang.model.SourceVersion;
import javax.annotation.processing.SupportedSourceVersion;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.AbstractProcessor;

@SupportedAnnotationTypes({ "org.bukkit.plugin.java.annotation.*" })
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class PluginAnnotationProcessor extends AbstractProcessor
{
    private boolean hasMainBeenFound;
    private static final DateTimeFormatter dFormat;
    
    static {
        dFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss", Locale.ENGLISH);
    }
    
    public PluginAnnotationProcessor() {
        this.hasMainBeenFound = false;
    }
    
    @Override
    public boolean process(final Set<? extends TypeElement> annots, final RoundEnvironment rEnv) {
        Element mainPluginElement = null;
        this.hasMainBeenFound = false;
        final Set<? extends Element> elements = rEnv.getElementsAnnotatedWith(Plugin.class);
        if (elements.size() > 1) {
            this.raiseError("Found more than one plugin main class");
            return false;
        }
        if (elements.isEmpty()) {
            return false;
        }
        if (this.hasMainBeenFound) {
            this.raiseError("The plugin class has already been located, aborting!");
            return false;
        }
        mainPluginElement = (Element)elements.iterator().next();
        this.hasMainBeenFound = true;
        if (!(mainPluginElement instanceof TypeElement)) {
            this.raiseError("Main plugin class is not a class", mainPluginElement);
            return false;
        }
        final TypeElement mainPluginType = (TypeElement)mainPluginElement;
        if (!(mainPluginType.getEnclosingElement() instanceof PackageElement)) {
            this.raiseError("Main plugin class is not a top-level class", mainPluginType);
            return false;
        }
        if (mainPluginType.getModifiers().contains(Modifier.STATIC)) {
            this.raiseError("Main plugin class cannot be static nested", mainPluginType);
            return false;
        }
        if (!this.processingEnv.getTypeUtils().isSubtype(mainPluginType.asType(), this.fromClass(JavaPlugin.class))) {
            this.raiseError("Main plugin class is not an subclass of JavaPlugin!", mainPluginType);
        }
        if (mainPluginType.getModifiers().contains(Modifier.ABSTRACT)) {
            this.raiseError("Main plugin class cannot be abstract", mainPluginType);
            return false;
        }
        this.checkForNoArgsConstructor(mainPluginType);
        final Map<String, Object> yml = Maps.newLinkedHashMap();
        final String mainName = mainPluginType.getQualifiedName().toString();
        yml.put("main", mainName);
        this.processAndPut(yml, "name", mainPluginType, mainName.substring(mainName.lastIndexOf(46) + 1), Plugin.class, String.class, "name");
        this.processAndPut(yml, "version", mainPluginType, "v0.0", Plugin.class, String.class, "version");
        this.processAndPut(yml, "description", mainPluginType, null, Description.class, String.class);
        this.processAndPut(yml, "load", mainPluginType, null, LoadOrder.class, String.class);
        final Author[] authors = mainPluginType.getAnnotationsByType(Author.class);
        final List<String> authorMap = Lists.newArrayList();
        Author[] array;
        for (int length = (array = authors).length, k = 0; k < length; ++k) {
            final Author auth = array[k];
            authorMap.add(auth.value());
        }
        if (authorMap.size() > 1) {
            yml.put("authors", authorMap);
        }
        else if (authorMap.size() == 1) {
            yml.put("author", authorMap.iterator().next());
        }
        this.processAndPut(yml, "website", mainPluginType, null, Website.class, String.class);
        this.processAndPut(yml, "prefix", mainPluginType, null, LogPrefix.class, String.class);
        final Dependency[] dependencies = mainPluginType.getAnnotationsByType(Dependency.class);
        final List<String> hardDependencies = Lists.newArrayList();
        Dependency[] array2;
        for (int length2 = (array2 = dependencies).length, l = 0; l < length2; ++l) {
            final Dependency dep = array2[l];
            hardDependencies.add(dep.value());
        }
        if (!hardDependencies.isEmpty()) {
            yml.put("depend", hardDependencies);
        }
        final SoftDependency[] softDependencies = mainPluginType.getAnnotationsByType(SoftDependency.class);
        final String[] softDepArr = new String[softDependencies.length];
        for (int i = 0; i < softDependencies.length; ++i) {
            softDepArr[i] = softDependencies[i].value();
        }
        if (softDepArr.length > 0) {
            yml.put("softdepend", softDepArr);
        }
        final LoadBefore[] loadBefore = mainPluginType.getAnnotationsByType(LoadBefore.class);
        final String[] loadBeforeArr = new String[loadBefore.length];
        for (int j = 0; j < loadBefore.length; ++j) {
            loadBeforeArr[j] = loadBefore[j].value();
        }
        if (loadBeforeArr.length > 0) {
            yml.put("loadbefore", loadBeforeArr);
        }
        Map<String, Map<String, Object>> commandMap = Maps.newLinkedHashMap();
        final boolean validCommandExecutors = this.processExternalCommands(rEnv.getElementsAnnotatedWith(Commands.class), mainPluginType, commandMap);
        if (!validCommandExecutors) {
            return false;
        }
        final Commands commands = mainPluginType.getAnnotation(Commands.class);
        if (commands != null) {
            final Map<String, Map<String, Object>> merged = Maps.newLinkedHashMap();
            merged.putAll(commandMap);
            merged.putAll(this.processCommands(commands));
            commandMap = merged;
        }
        yml.put("commands", commandMap);
        Map<String, Map<String, Object>> permissionMetadata = Maps.newLinkedHashMap();
        final Set<? extends Element> permissionAnnotations = rEnv.getElementsAnnotatedWith(Command.class);
        if (permissionAnnotations.size() > 0) {
            for (final Element element : permissionAnnotations) {
                if (element.equals(mainPluginElement)) {
                    continue;
                }
                if (element.getAnnotation(Permission.class) == null) {
                    continue;
                }
                final Permission permissionAnnotation = element.getAnnotation(Permission.class);
                permissionMetadata.put(permissionAnnotation.name(), this.processPermission(permissionAnnotation));
            }
        }
        final Permissions permissions = mainPluginType.getAnnotation(Permissions.class);
        if (permissions != null) {
            final Map<String, Map<String, Object>> joined = Maps.newLinkedHashMap();
            joined.putAll(permissionMetadata);
            joined.putAll(this.processPermissions(permissions));
            permissionMetadata = joined;
        }
        final boolean validPermissions = this.processExternalPermissions(rEnv.getElementsAnnotatedWith(Permissions.class), mainPluginType, permissionMetadata);
        if (!validPermissions) {
            return false;
        }
        yml.put("permissions", permissionMetadata);
        if (mainPluginType.getAnnotation(ApiVersion.class) != null) {
            final ApiVersion apiVersion = mainPluginType.getAnnotation(ApiVersion.class);
            if (apiVersion.value() != ApiVersion.Target.DEFAULT) {
                yml.put("api-version", apiVersion.value().getVersion());
            }
        }
        try {
            final Yaml yaml = new Yaml();
            final FileObject file = this.processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", "plugin.yml", new Element[0]);
            Throwable t = null;
            try {
                final Writer w = file.openWriter();
                try {
                    w.append((CharSequence)"# Auto-generated plugin.yml, generated at ").append((CharSequence)LocalDateTime.now().format(PluginAnnotationProcessor.dFormat)).append((CharSequence)" by ").append((CharSequence)this.getClass().getName()).append((CharSequence)"\n\n");
                    final String raw = yaml.dumpAs((Object)yml, Tag.MAP, DumperOptions.FlowStyle.BLOCK);
                    w.write(raw);
                    w.flush();
                    w.close();
                }
                finally {
                    if (w != null) {
                        w.close();
                    }
                }
            }
            finally {
                if (t == null) {
                    final Throwable exception = null;
                    t = exception;
                }
                else {
                    final Throwable exception = null;
                    if (t != exception) {
                        t.addSuppressed(exception);
                    }
                }
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        return true;
    }
    
    private boolean processExternalPermissions(final Set<? extends Element> commandExecutors, final TypeElement mainPluginType, final Map<String, Map<String, Object>> yml) {
        for (final Element element : commandExecutors) {
            if (!(element instanceof TypeElement)) {
                this.raiseError("Specified Command Executor class is not a class.");
                return false;
            }
            final TypeElement typeElement = (TypeElement)element;
            if (typeElement.equals(mainPluginType)) {
                continue;
            }
            final TypeMirror mirror = this.processingEnv.getElementUtils().getTypeElement(CommandExecutor.class.getName()).asType();
            if (!this.processingEnv.getTypeUtils().isAssignable(typeElement.asType(), mirror)) {
                this.raiseError("Specified Command Executor class is not assignable from CommandExecutor ");
                return false;
            }
            final Map<String, Map<String, Object>> newMap = Maps.newLinkedHashMap();
            final Permissions annotation = typeElement.getAnnotation(Permissions.class);
            if (annotation != null && annotation.value().length > 0) {
                newMap.putAll(this.processPermissions(annotation));
            }
            yml.putAll(newMap);
        }
        return true;
    }
    
    private void checkForNoArgsConstructor(final TypeElement mainPluginType) {
        for (final ExecutableElement constructor : ElementFilter.constructorsIn(mainPluginType.getEnclosedElements())) {
            if (constructor.getParameters().isEmpty()) {
                return;
            }
        }
        this.raiseError("Main plugin class must have a no argument constructor.", mainPluginType);
    }
    
    private void raiseError(final String message) {
        this.processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message);
    }
    
    private void raiseError(final String message, final Element element) {
        this.processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
    }
    
    private TypeMirror fromClass(final Class<?> clazz) {
        return this.processingEnv.getElementUtils().getTypeElement(clazz.getName()).asType();
    }
    
    private <A extends Annotation, R> R processAndPut(final Map<String, Object> map, final String name, final Element el, final R defaultVal, final Class<A> annotationType, final Class<R> returnType) {
        return this.processAndPut(map, name, el, defaultVal, annotationType, returnType, "value");
    }
    
    private <A extends Annotation, R> R processAndPut(final Map<String, Object> map, final String name, final Element el, final R defaultVal, final Class<A> annotationType, final Class<R> returnType, final String methodName) {
        final R result = this.process(el, defaultVal, annotationType, returnType, methodName);
        if (result != null) {
            map.put(name, result);
        }
        return result;
    }
    
    private <A extends Annotation, R> R process(final Element el, final R defaultVal, final Class<A> annotationType, final Class<R> returnType, final String methodName) {
        final A ann = el.getAnnotation(annotationType);
        R result;
        if (ann == null) {
            result = defaultVal;
        }
        else {
            try {
                final Method value = annotationType.getMethod(methodName, (Class<?>[])new Class[0]);
                final Object res = value.invoke(ann, new Object[0]);
                result = (R)((returnType == String.class) ? res.toString() : returnType.cast(res));
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }
    
    private boolean processExternalCommands(final Set<? extends Element> commandExecutors, final TypeElement mainPluginType, final Map<String, Map<String, Object>> commandMetadata) {
        for (final Element element : commandExecutors) {
            if (!(element instanceof TypeElement)) {
                this.raiseError("Specified Command Executor class is not a class.");
                return false;
            }
            final TypeElement typeElement = (TypeElement)element;
            if (typeElement.equals(mainPluginType)) {
                continue;
            }
            final TypeMirror mirror = this.processingEnv.getElementUtils().getTypeElement(CommandExecutor.class.getName()).asType();
            if (!this.processingEnv.getTypeUtils().isAssignable(typeElement.asType(), mirror)) {
                this.raiseError("Specified Command Executor class is not assignable from CommandExecutor ");
                return false;
            }
            final Commands annotation = typeElement.getAnnotation(Commands.class);
            if (annotation == null || annotation.value().length <= 0) {
                continue;
            }
            commandMetadata.putAll(this.processCommands(annotation));
        }
        return true;
    }
    
    protected Map<String, Map<String, Object>> processCommands(final Commands commands) {
        final Map<String, Map<String, Object>> commandList = Maps.newLinkedHashMap();
        Command[] value;
        for (int length = (value = commands.value()).length, i = 0; i < length; ++i) {
            final Command command = value[i];
            commandList.put(command.name(), this.processCommand(command));
        }
        return commandList;
    }
    
    protected Map<String, Object> processCommand(final Command commandAnnotation) {
        final Map<String, Object> command = Maps.newLinkedHashMap();
        if (commandAnnotation.aliases().length == 1) {
            command.put("aliases", commandAnnotation.aliases()[0]);
        }
        else if (commandAnnotation.aliases().length > 1) {
            command.put("aliases", commandAnnotation.aliases());
        }
        if (!commandAnnotation.desc().isEmpty()) {
            command.put("description", commandAnnotation.desc());
        }
        if (!commandAnnotation.permission().isEmpty()) {
            command.put("permission", commandAnnotation.permission());
        }
        if (!commandAnnotation.permissionMessage().isEmpty()) {
            command.put("permission-message", commandAnnotation.permissionMessage());
        }
        if (!commandAnnotation.usage().isEmpty()) {
            command.put("usage", commandAnnotation.usage());
        }
        return command;
    }
    
    protected Map<String, Object> processPermission(final Permission permissionAnnotation) {
        final Map<String, Object> permission = Maps.newLinkedHashMap();
        if (!"".equals(permissionAnnotation.desc())) {
            permission.put("description", permissionAnnotation.desc());
        }
        if (PermissionDefault.OP != permissionAnnotation.defaultValue()) {
            permission.put("default", permissionAnnotation.defaultValue().toString().toLowerCase());
        }
        if (permissionAnnotation.children().length > 0) {
            final Map<String, Boolean> childrenList = Maps.newLinkedHashMap();
            ChildPermission[] children;
            for (int length = (children = permissionAnnotation.children()).length, i = 0; i < length; ++i) {
                final ChildPermission childPermission = children[i];
                childrenList.put(childPermission.name(), childPermission.inherit());
            }
            permission.put("children", childrenList);
        }
        return permission;
    }
    
    protected Map<String, Map<String, Object>> processPermissions(final Permissions permissions) {
        final Map<String, Map<String, Object>> permissionList = Maps.newLinkedHashMap();
        Permission[] value;
        for (int length = (value = permissions.value()).length, i = 0; i < length; ++i) {
            final Permission permission = value[i];
            permissionList.put(permission.name(), this.processPermission(permission));
        }
        return permissionList;
    }
}
