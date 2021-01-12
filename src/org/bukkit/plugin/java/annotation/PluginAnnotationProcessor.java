package org.bukkit.plugin.java.annotation;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import javax.tools.Diagnostic.Kind;
import org.bukkit.command.CommandExecutor;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.annotation.command.Command;
import org.bukkit.plugin.java.annotation.command.Commands;
import org.bukkit.plugin.java.annotation.dependency.Dependency;
import org.bukkit.plugin.java.annotation.dependency.LoadBefore;
import org.bukkit.plugin.java.annotation.dependency.SoftDependency;
import org.bukkit.plugin.java.annotation.permission.ChildPermission;
import org.bukkit.plugin.java.annotation.permission.Permission;
import org.bukkit.plugin.java.annotation.permission.Permissions;
import org.bukkit.plugin.java.annotation.plugin.ApiVersion;
import org.bukkit.plugin.java.annotation.plugin.Description;
import org.bukkit.plugin.java.annotation.plugin.LoadOrder;
import org.bukkit.plugin.java.annotation.plugin.LogPrefix;
import org.bukkit.plugin.java.annotation.plugin.Plugin;
import org.bukkit.plugin.java.annotation.plugin.Website;
import org.bukkit.plugin.java.annotation.plugin.ApiVersion.Target;
import org.bukkit.plugin.java.annotation.plugin.author.Author;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.nodes.Tag;

@SupportedAnnotationTypes({"org.bukkit.plugin.java.annotation.*"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class PluginAnnotationProcessor extends AbstractProcessor {
	private boolean hasMainBeenFound = false;
	private static final DateTimeFormatter dFormat;

	static {
		dFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss", Locale.ENGLISH);
	}

	public boolean process(Set<? extends TypeElement> annots, RoundEnvironment rEnv) {
		Element mainPluginElement = null;
		this.hasMainBeenFound = false;
		Set<? extends Element> elements = rEnv.getElementsAnnotatedWith(Plugin.class);
		if (elements.size() > 1) {
			this.raiseError("Found more than one plugin main class");
			return false;
		} else if (elements.isEmpty()) {
			return false;
		} else if (this.hasMainBeenFound) {
			this.raiseError("The plugin class has already been located, aborting!");
			return false;
		} else {
			mainPluginElement = (Element) elements.iterator().next();
			this.hasMainBeenFound = true;
			if (!(mainPluginElement instanceof TypeElement)) {
				this.raiseError("Main plugin class is not a class", mainPluginElement);
				return false;
			} else {
				TypeElement mainPluginType = (TypeElement) mainPluginElement;
				if (!(mainPluginType.getEnclosingElement() instanceof PackageElement)) {
					this.raiseError("Main plugin class is not a top-level class", mainPluginType);
					return false;
				} else if (mainPluginType.getModifiers().contains(Modifier.STATIC)) {
					this.raiseError("Main plugin class cannot be static nested", mainPluginType);
					return false;
				} else {
					if (!this.processingEnv.getTypeUtils().isSubtype(mainPluginType.asType(),
							this.fromClass(JavaPlugin.class))) {
						this.raiseError("Main plugin class is not an subclass of JavaPlugin!", mainPluginType);
					}

					if (mainPluginType.getModifiers().contains(Modifier.ABSTRACT)) {
						this.raiseError("Main plugin class cannot be abstract", mainPluginType);
						return false;
					} else {
						this.checkForNoArgsConstructor(mainPluginType);
						Map<String, Object> yml = Maps.newLinkedHashMap();
						String mainName = mainPluginType.getQualifiedName().toString();
						yml.put("main", mainName);
						this.processAndPut(yml, "name", mainPluginType,
								mainName.substring(mainName.lastIndexOf(46) + 1), Plugin.class, String.class, "name");
						this.processAndPut(yml, "version", mainPluginType, "v0.0", Plugin.class, String.class,
								"version");
						this.processAndPut(yml, "description", mainPluginType, (Object) null, Description.class,
								String.class);
						this.processAndPut(yml, "load", mainPluginType, (Object) null, LoadOrder.class, String.class);
						Author[] authors = (Author[]) mainPluginType.getAnnotationsByType(Author.class);
						List<String> authorMap = Lists.newArrayList();
						Author[] var13 = authors;
						int var12 = authors.length;

						for (int var11 = 0; var11 < var12; ++var11) {
							Author auth = var13[var11];
							authorMap.add(auth.value());
						}

						if (authorMap.size() > 1) {
							yml.put("authors", authorMap);
						} else if (authorMap.size() == 1) {
							yml.put("author", authorMap.iterator().next());
						}

						this.processAndPut(yml, "website", mainPluginType, (Object) null, Website.class, String.class);
						this.processAndPut(yml, "prefix", mainPluginType, (Object) null, LogPrefix.class, String.class);
						Dependency[] dependencies = (Dependency[]) mainPluginType
								.getAnnotationsByType(Dependency.class);
						List<String> hardDependencies = Lists.newArrayList();
						Dependency[] var15 = dependencies;
						int i = dependencies.length;

						for (int var42 = 0; var42 < i; ++var42) {
							Dependency dep = var15[var42];
							hardDependencies.add(dep.value());
						}

						if (!hardDependencies.isEmpty()) {
							yml.put("depend", hardDependencies);
						}

						SoftDependency[] softDependencies = (SoftDependency[]) mainPluginType
								.getAnnotationsByType(SoftDependency.class);
						String[] softDepArr = new String[softDependencies.length];

						for (i = 0; i < softDependencies.length; ++i) {
							softDepArr[i] = softDependencies[i].value();
						}

						if (softDepArr.length > 0) {
							yml.put("softdepend", softDepArr);
						}

						LoadBefore[] loadBefore = (LoadBefore[]) mainPluginType.getAnnotationsByType(LoadBefore.class);
						String[] loadBeforeArr = new String[loadBefore.length];

						for (i = 0; i < loadBefore.length; ++i) {
							loadBeforeArr[i] = loadBefore[i].value();
						}

						if (loadBeforeArr.length > 0) {
							yml.put("loadbefore", loadBeforeArr);
						}

						Map<String, Map<String, Object>> commandMap = Maps.newLinkedHashMap();
						boolean validCommandExecutors = this.processExternalCommands(
								rEnv.getElementsAnnotatedWith(Commands.class), mainPluginType, commandMap);
						if (!validCommandExecutors) {
							return false;
						} else {
							Commands commands = (Commands) mainPluginType.getAnnotation(Commands.class);
							LinkedHashMap permissionMetadata;
							if (commands != null) {
								permissionMetadata = Maps.newLinkedHashMap();
								permissionMetadata.putAll(commandMap);
								permissionMetadata.putAll(this.processCommands(commands));
								commandMap = permissionMetadata;
							}

							yml.put("commands", commandMap);
							permissionMetadata = Maps.newLinkedHashMap();
							Set<? extends Element> permissionAnnotations = rEnv.getElementsAnnotatedWith(Command.class);
							if (permissionAnnotations.size() > 0) {
								Iterator var22 = permissionAnnotations.iterator();

								while (var22.hasNext()) {
									Element element = (Element) var22.next();
									if (!element.equals(mainPluginElement)
											&& element.getAnnotation(Permission.class) != null) {
										Permission permissionAnnotation = (Permission) element
												.getAnnotation(Permission.class);
										permissionMetadata.put(permissionAnnotation.name(),
												this.processPermission(permissionAnnotation));
									}
								}
							}

							Permissions permissions = (Permissions) mainPluginType.getAnnotation(Permissions.class);
							if (permissions != null) {
								Map<String, Map<String, Object>> joined = Maps.newLinkedHashMap();
								joined.putAll(permissionMetadata);
								joined.putAll(this.processPermissions(permissions));
								permissionMetadata = (LinkedHashMap) joined;
							}

							boolean validPermissions = this.processExternalPermissions(
									rEnv.getElementsAnnotatedWith(Permissions.class), mainPluginType,
									permissionMetadata);
							if (!validPermissions) {
								return false;
							} else {
								yml.put("permissions", permissionMetadata);
								if (mainPluginType.getAnnotation(ApiVersion.class) != null) {
									ApiVersion apiVersion = (ApiVersion) mainPluginType.getAnnotation(ApiVersion.class);
									if (apiVersion.value() != Target.DEFAULT) {
										yml.put("api-version", apiVersion.value().getVersion());
									}
								}

								try {
									Yaml yaml = new Yaml();
									FileObject file = this.processingEnv.getFiler().createResource(
											StandardLocation.CLASS_OUTPUT, "", "plugin.yml", new Element[0]);
									Throwable var25 = null;
									Object var26 = null;

									try {
										Writer w = file.openWriter();

										try {
											w.append("# Auto-generated plugin.yml, generated at ")
													.append(LocalDateTime.now().format(dFormat)).append(" by ")
													.append(this.getClass().getName()).append("\n\n");
											String raw = yaml.dumpAs(yml, Tag.MAP, FlowStyle.BLOCK);
											w.write(raw);
											w.flush();
											w.close();
										} finally {
											if (w != null) {
												w.close();
											}

										}

										return true;
									} catch (Throwable var36) {
										if (var25 == null) {
											var25 = var36;
										} else if (var25 != var36) {
											var25.addSuppressed(var36);
										}

										throw var25;
									}
								} catch (IOException var37) {
									throw new RuntimeException(var37);
								}
							}
						}
					}
				}
			}
		}
	}

	private boolean processExternalPermissions(Set<? extends Element> commandExecutors, TypeElement mainPluginType,
			Map<String, Map<String, Object>> yml) {
		Iterator var5 = commandExecutors.iterator();

		while (var5.hasNext()) {
			Element element = (Element) var5.next();
			if (!(element instanceof TypeElement)) {
				this.raiseError("Specified Command Executor class is not a class.");
				return false;
			}

			TypeElement typeElement = (TypeElement) element;
			if (!typeElement.equals(mainPluginType)) {
				TypeMirror mirror = this.processingEnv.getElementUtils().getTypeElement(CommandExecutor.class.getName())
						.asType();
				if (!this.processingEnv.getTypeUtils().isAssignable(typeElement.asType(), mirror)) {
					this.raiseError("Specified Command Executor class is not assignable from CommandExecutor ");
					return false;
				}

				Map<String, Map<String, Object>> newMap = Maps.newLinkedHashMap();
				Permissions annotation = (Permissions) typeElement.getAnnotation(Permissions.class);
				if (annotation != null && annotation.value().length > 0) {
					newMap.putAll(this.processPermissions(annotation));
				}

				yml.putAll(newMap);
			}
		}

		return true;
	}

	private void checkForNoArgsConstructor(TypeElement mainPluginType) {
		Iterator var3 = ElementFilter.constructorsIn(mainPluginType.getEnclosedElements()).iterator();

		while (var3.hasNext()) {
			ExecutableElement constructor = (ExecutableElement) var3.next();
			if (constructor.getParameters().isEmpty()) {
				return;
			}
		}

		this.raiseError("Main plugin class must have a no argument constructor.", mainPluginType);
	}

	private void raiseError(String message) {
		this.processingEnv.getMessager().printMessage(Kind.ERROR, message);
	}

	private void raiseError(String message, Element element) {
		this.processingEnv.getMessager().printMessage(Kind.ERROR, message, element);
	}

	private TypeMirror fromClass(Class<?> clazz) {
		return this.processingEnv.getElementUtils().getTypeElement(clazz.getName()).asType();
	}

	private <A extends Annotation, R> R processAndPut(Map<String, Object> map, String name, Element el, R defaultVal,
			Class<A> annotationType, Class<R> returnType) {
		return this.processAndPut(map, name, el, defaultVal, annotationType, returnType, "value");
	}

	private <A extends Annotation, R> R processAndPut(Map<String, Object> map, String name, Element el, R defaultVal,
			Class<A> annotationType, Class<R> returnType, String methodName) {
		R result = this.process(el, defaultVal, annotationType, returnType, methodName);
		if (result != null) {
			map.put(name, result);
		}

		return result;
	}

	private <A extends Annotation, R> R process(Element el, R defaultVal, Class<A> annotationType, Class<R> returnType,
			String methodName) {
		A ann = el.getAnnotation(annotationType);
		Object result;
		if (ann == null) {
			result = defaultVal;
		} else {
			try {
				Method value = annotationType.getMethod(methodName);
				Object res = value.invoke(ann);
				result = returnType == String.class ? res.toString() : returnType.cast(res);
			} catch (Exception var10) {
				throw new RuntimeException(var10);
			}
		}

		return (R) result;
	}

	private boolean processExternalCommands(Set<? extends Element> commandExecutors, TypeElement mainPluginType,
			Map<String, Map<String, Object>> commandMetadata) {
		Iterator var5 = commandExecutors.iterator();

		while (var5.hasNext()) {
			Element element = (Element) var5.next();
			if (!(element instanceof TypeElement)) {
				this.raiseError("Specified Command Executor class is not a class.");
				return false;
			}

			TypeElement typeElement = (TypeElement) element;
			if (!typeElement.equals(mainPluginType)) {
				TypeMirror mirror = this.processingEnv.getElementUtils().getTypeElement(CommandExecutor.class.getName())
						.asType();
				if (!this.processingEnv.getTypeUtils().isAssignable(typeElement.asType(), mirror)) {
					this.raiseError("Specified Command Executor class is not assignable from CommandExecutor ");
					return false;
				}

				Commands annotation = (Commands) typeElement.getAnnotation(Commands.class);
				if (annotation != null && annotation.value().length > 0) {
					commandMetadata.putAll(this.processCommands(annotation));
				}
			}
		}

		return true;
	}

	protected Map<String, Map<String, Object>> processCommands(Commands commands) {
		Map<String, Map<String, Object>> commandList = Maps.newLinkedHashMap();
		Command[] var6;
		int var5 = (var6 = commands.value()).length;

		for (int var4 = 0; var4 < var5; ++var4) {
			Command command = var6[var4];
			commandList.put(command.name(), this.processCommand(command));
		}

		return commandList;
	}

	protected Map<String, Object> processCommand(Command commandAnnotation) {
		Map<String, Object> command = Maps.newLinkedHashMap();
		if (commandAnnotation.aliases().length == 1) {
			command.put("aliases", commandAnnotation.aliases()[0]);
		} else if (commandAnnotation.aliases().length > 1) {
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

	protected Map<String, Object> processPermission(Permission permissionAnnotation) {
		Map<String, Object> permission = Maps.newLinkedHashMap();
		if (!"".equals(permissionAnnotation.desc())) {
			permission.put("description", permissionAnnotation.desc());
		}

		if (PermissionDefault.OP != permissionAnnotation.defaultValue()) {
			permission.put("default", permissionAnnotation.defaultValue().toString().toLowerCase());
		}

		if (permissionAnnotation.children().length > 0) {
			Map<String, Boolean> childrenList = Maps.newLinkedHashMap();
			ChildPermission[] var7;
			int var6 = (var7 = permissionAnnotation.children()).length;

			for (int var5 = 0; var5 < var6; ++var5) {
				ChildPermission childPermission = var7[var5];
				childrenList.put(childPermission.name(), childPermission.inherit());
			}

			permission.put("children", childrenList);
		}

		return permission;
	}

	protected Map<String, Map<String, Object>> processPermissions(Permissions permissions) {
		Map<String, Map<String, Object>> permissionList = Maps.newLinkedHashMap();
		Permission[] var6;
		int var5 = (var6 = permissions.value()).length;

		for (int var4 = 0; var4 < var5; ++var4) {
			Permission permission = var6[var4];
			permissionList.put(permission.name(), this.processPermission(permission));
		}

		return permissionList;
	}
}