package dev.slfc.epilogue.processor;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import com.google.auto.service.AutoService;
import dev.slfc.epilogue.Epilogue;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

@SupportedAnnotationTypes("dev.slfc.epilogue.Epilogue")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@AutoService(Processor.class)
public class AnnotationProcessor extends AbstractProcessor {
  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    List<String> loggerClassNames = new ArrayList<>();
    TypeElement mainRobotClass = null;

    // Used to check for a main robot class
    var robotBaseClass = processingEnv.getElementUtils().getTypeElement("edu.wpi.first.wpilibj.RobotBase").asType();

    Predicate<Element> notSkipped = (e) -> {
      var epilogue = e.getAnnotation(Epilogue.class);
      // Skipping must be done through the annotation, so no annotation means it's not skipped
      return epilogue == null || epilogue.importance() != Epilogue.Importance.NONE;
    };

    for (TypeElement annotation : annotations) {
      var annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);

      boolean validFields = validateFields(annotatedElements);
      boolean validMethods = validateMethods(annotatedElements);

      if (!(validFields && validMethods)) {
        // Generate nothing and bail
        // Return `true` to mark the annotations as claimed so they're not processed again
        return true;
      }

      var classes = annotatedElements.stream().filter(e -> e instanceof TypeElement).map(e -> (TypeElement) e).toList();
      for (TypeElement clazz : classes) {
        var epilogue = clazz.getAnnotation(Epilogue.class);
        boolean requireExplicitOptIn = epilogue.strategy() == Epilogue.Strategy.OPT_IN;

        Predicate<Element> optedIn = e -> !requireExplicitOptIn || e.getAnnotation(Epilogue.class) != null;

        var fieldsToLog =
            clazz.getEnclosedElements().stream()
                .filter(e -> e instanceof VariableElement)
                .map(e -> (VariableElement) e)
                .filter(notSkipped)
                .filter(optedIn)
                .filter(e -> !e.getModifiers().contains(Modifier.STATIC))
                .filter(e -> isLoggable(e, e.asType(), false))
                .toList();

        var methodsToLog =
            clazz.getEnclosedElements().stream()
                .filter(e -> e instanceof ExecutableElement)
                .map(e -> (ExecutableElement) e)
                .filter(notSkipped)
                .filter(optedIn)
                .filter(e -> !e.getModifiers().contains(Modifier.STATIC))
                .filter(e -> e.getModifiers().contains(Modifier.PUBLIC))
                .filter(e -> e.getParameters().isEmpty())
                .filter(e -> e.getReceiverType() != null)
                .filter(e -> isLoggable(e, e.getReturnType(), false))
                .toList();

        try {
          String loggedClassName = clazz.getQualifiedName().toString();
          writeLoggerFile(loggedClassName, epilogue, fieldsToLog, methodsToLog);

          if (processingEnv.getTypeUtils().isAssignable(clazz.getSuperclass(), robotBaseClass)) {
            mainRobotClass = clazz;
          }

          loggerClassNames.add(loggedClassName + "Logger");
        } catch (IOException e) {
          processingEnv.getMessager().printMessage(
              Diagnostic.Kind.ERROR,
              "Could not write logger file for " + clazz.getQualifiedName(),
              clazz
          );
        }
      }
    }

    if (!annotations.isEmpty()) {
      writeEpiloguerFile(loggerClassNames, mainRobotClass);
    }

    return true;
  }

  private boolean validateFields(Set<? extends Element> annotatedElements) {
    var fields =
        annotatedElements.stream()
            .filter(e -> e instanceof VariableElement)
            .map(e -> (VariableElement) e)
            .toList();

    boolean valid = true;

    for (VariableElement field : fields) {
      var config = field.getAnnotation(Epilogue.class);
      if (config != null) {
        // Field is explicitly tagged
        if (config.importance() != Epilogue.Importance.NONE) {
          // And is not opted out of
          if (!isLoggable(field, field.asType(), false)) {
            // And is not of a loggable type
            processingEnv.getMessager().printMessage(
                Diagnostic.Kind.ERROR,
                "[EPILOGUE] You have opted in to Epilogue logging on this field, but it is not a loggable data type!",
                field
            );
            valid = false;
          }
        }
      }
    }
    return valid;
  }

  private boolean validateMethods(Set<? extends Element> annotatedElements) {
    var methods =
        annotatedElements.stream()
            .filter(e -> e instanceof ExecutableElement)
            .map(e -> (ExecutableElement) e)
            .toList();

    boolean valid = true;

    for (ExecutableElement method : methods) {
      var config = method.getAnnotation(Epilogue.class);
      if (config != null) {
        // Field is explicitly tagged
        if (config.importance() != Epilogue.Importance.NONE) {
          // And is not opted out of
          if (!isLoggable(method, method.getReturnType(), false)) {
            // And is not of a loggable type
            processingEnv.getMessager().printMessage(
                Diagnostic.Kind.ERROR,
                "[EPILOGUE] You have opted in to Epilogue logging on this method, but it does not return a loggable data type!",
                method
            );
            valid = false;
          }

          if (!method.getModifiers().contains(Modifier.PUBLIC)) {
            // Only public methods can be logged

            processingEnv.getMessager().printMessage(
                Diagnostic.Kind.ERROR,
                "[EPILOGUE] Logged methods must be public",
                method
            );

            valid = false;
          }

          if (method.getModifiers().contains(Modifier.STATIC)) {
            processingEnv.getMessager().printMessage(
                Diagnostic.Kind.ERROR,
                "[EPILOGUE] Logged methods cannot be static",
                method
            );

            valid = false;
          }

          if (method.getReturnType().getKind() == TypeKind.NONE) {
            processingEnv.getMessager().printMessage(
                Diagnostic.Kind.ERROR,
                "[EPILOGUE] Logged methods cannot be void",
                method
            );

            valid = false;
          }

          if (!method.getParameters().isEmpty()) {
            processingEnv.getMessager().printMessage(
                Diagnostic.Kind.ERROR,
                "[EPILOGUE] Logged methods cannot accept arguments",
                method
            );

            valid = false;
          }
        }
      }
    }
    return valid;
  }

  /**
   * Creates the Epiloguer file, which is the main entry point for users to set up and interact
   * with the generated loggers.
   *
   * @param loggerClassNames the names of the generated logger classes. Each of these will be
   *                         instantiated in a public static field on the Epiloguer class.
   * @param mainRobotClass the main robot class. May be null. Used to generate a {@code bind()}
   *                       method to add a callback hook to a TimedRobot to log itself.
   */
  private void writeEpiloguerFile(List<String> loggerClassNames, TypeElement mainRobotClass) {
    final String robotClassName = mainRobotClass == null ? null : mainRobotClass.getQualifiedName().toString();
    final var timedRobot = processingEnv.getElementUtils().getTypeElement("edu.wpi.first.wpilibj.TimedRobot").asType();
    final boolean isTimedRobot = mainRobotClass != null && processingEnv.getTypeUtils().isAssignable(mainRobotClass.asType(), timedRobot);

    try {
      var centralStore = processingEnv.getFiler().createSourceFile("dev.slfc.epilogue.Epiloguer");

      try (var out = new PrintWriter(centralStore.openOutputStream())) {
        out.println("package dev.slfc.epilogue;");
        out.println();

        loggerClassNames.stream().sorted().forEach(name -> {
          if (!name.contains(".")) {
            // Logger is in the global namespace, don't need to import
            return;
          }

          out.print("import ");
          out.print(name);
          out.println(";");
        });
        out.println();

        out.println("public final class Epiloguer {");
        out.println("  private static final EpilogueConfiguration config = new EpilogueConfiguration();");
        out.println();

        loggerClassNames.forEach(name -> {
          String simple = simpleName(name);

          // public static final FooLogger fooLogger = new FooLogger();
          out.print("  public static final ");
          out.print(simple);
          out.print(" ");
          out.print(lowerCamelCase(simple));
          out.print(" = new ");
          out.print(simple);
          out.println("();");
        });
        out.println();

        out.println("""
              public static void configure(java.util.function.Consumer<EpilogueConfiguration> configurator) {
                configurator.accept(config);
              }

              public static EpilogueConfiguration getConfig() {
                return config;
              }
            """);

        out.println("""
              /**
               * Checks if data associated with a given importance level should be logged.
               */
              public static boolean shouldLog(Epilogue.Importance importance) {
                return importance.compareTo(config.minimumImportance) >= 0;
              }
            """.stripTrailing());

        // Only generate a binding if the robot class is a TimedRobot
        if (isTimedRobot) {
          out.println();
          out.print("""
                /**
                 * Binds Epilogue updates to a timed robot's update period. Log calls will be made at the
                 * same update rate as the robot's loop function, but will be offset by a full phase
                 * (for example, a 20ms update rate but 10ms offset from the main loop invocation) to
                 * help avoid high CPU loads. However, this does mean that any logged data that reads
                 * directly from sensors will be slightly different from data used in the main robot
                 * loop.
                 */
              """);
          out.println("  public static void bind(" + robotClassName + " robot) {");
          out.println("    robot.addPeriodic(() -> {");
          out.println("      long start = System.nanoTime();");
          out.println("      " + lowerCamelCase(simpleName(robotClassName)) + "Logger.tryUpdate(config.dataLogger.getSubLogger(config.root), robot, config.errorHandler);");
          out.println("      edu.wpi.first.networktables.NetworkTableInstance.getDefault().getEntry(\"Epilogue/Stats/Last Run\").setDouble((System.nanoTime() - start) / 1e6);");
          out.println("    }, robot.getPeriod(), robot.getPeriod() / 2);");
          out.println("  }");
        }

        out.println("}");
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void writeLoggerFile(
      String className,
      Epilogue classConfig,
      List<VariableElement> loggableFields,
      List<ExecutableElement> loggableMethods) throws IOException {
    String packageName = null;
    int lastDot = className.lastIndexOf('.');
    if (lastDot > 0) {
      packageName = className.substring(0, lastDot);
    }

    String simpleClassName = simpleName(className);

    String loggerClassName = className + "Logger";
    String loggerSimpleClassName = loggerClassName.substring(lastDot + 1);

    var loggerFile = processingEnv.getFiler().createSourceFile(loggerClassName);

    var privateFields = loggableFields.stream().filter(e -> e.getModifiers().contains(Modifier.PRIVATE)).toList();
    boolean requiresVarHandles = !privateFields.isEmpty();

    try (var out = new PrintWriter(loggerFile.openWriter())) {
      if (packageName != null) {
        // package com.example;
        out.print("package ");
        out.print(packageName);
        out.println(";");
        out.println();
      }

      out.println("import dev.slfc.epilogue.Epilogue;");
      out.println("import dev.slfc.epilogue.Epiloguer;");
      out.println("import dev.slfc.epilogue.logging.ClassSpecificLogger;");
      out.println("import dev.slfc.epilogue.logging.DataLogger;");
      if (requiresVarHandles) {
        out.println("import java.lang.invoke.MethodHandles;");
        out.println("import java.lang.invoke.VarHandle;");
      }
      out.println();

      // public class FooLogger implements ClassSpecificLogger<Foo> {
      out.print("public class ");
      out.print(loggerSimpleClassName);
      out.print(" extends ClassSpecificLogger<");
      out.print(simpleClassName);
      out.println("> {");

      if (requiresVarHandles) {
        for (var privateField : privateFields) {
          // This field needs a VarHandle to access.
          // Cache it in the class to avoid lookups
          out.println("  private static final VarHandle $" + privateField.getSimpleName() + ";");
        }
        out.println();
      }

      if (requiresVarHandles) {
        var clazz = simpleClassName + ".class";

        out.println("  static {");
        out.println("    try {");
        out.println("      var lookup = MethodHandles.privateLookupIn(" + clazz + ", MethodHandles.lookup());");

        for (var privateField : privateFields) {
          var fieldName = privateField.getSimpleName();
          out.println("      $" + fieldName + " = lookup.findVarHandle(" + clazz + ", \"" + fieldName + "\", " + processingEnv.getTypeUtils().erasure(privateField.asType()) + ".class);");
        }

        out.println("    } catch (ReflectiveOperationException e) {");
        out.println("      throw new RuntimeException(\"[EPILOGUE] Could not load private fields for logging!\", e);");
        out.println("    }");
        out.println("  }");
        out.println();
      }

      out.print("  public ");
      out.print(loggerSimpleClassName);
      out.println("() {");
      out.print("    super(");
      out.print(simpleClassName);
      out.println(".class);");
      out.println("  }");
      out.println();


      // @Override
      // public void update(DataLogger dataLogger, Foo object) {
      out.println("  @Override");
      out.println("  public void update(DataLogger dataLogger, " + simpleClassName + " object) {");

      // [log fields]
      // [log methods]

      // Build a map of importance levels to the fields logged at those levels
      // e.g. { DEBUG: [fieldA, fieldB], INFO: [fieldC], CRITICAL: [fieldD, fieldE, fieldF] }
      var loggedElementsByImportance =
          Stream
              .concat(loggableFields.stream(), loggableMethods.stream())
              .collect(groupingBy(
                  element -> {
                    var config = element.getAnnotation(Epilogue.class);
                    if (config == null) {
                      // No configuration on this element, fall back to the class-level configuration
                      return classConfig.importance();
                    } else {
                      return config.importance();
                    }
                  },
                  () -> new EnumMap<>(Epilogue.Importance.class), // EnumMap for consistent ordering
                  toList())
              );

      loggedElementsByImportance.forEach((importance, elements) -> {
        out.println("    if (Epiloguer.shouldLog(Epilogue.Importance." + importance.name() + ")) {");
        for (var loggableElement : elements) {
          switch (loggableElement) {
            case VariableElement field -> logField(out, field);
            case ExecutableElement method -> logMethod(out, method);
            default -> {
            } // Ignore
          }
        }
        out.println("    }");
      });

      out.println("  }");
      out.println("}");
    }
  }

  /**
   * Checks if a type is loggable.
   *
   * @param type the type to check
   * @param isArrayComponent flags the type as being a component of an outer array type. Used to
   *                         prevent logging multidimensional arrays or arrays of non-loggable types
   */
  private boolean isLoggable(Element element, TypeMirror type, boolean isArrayComponent) {
    if (type instanceof NoType) {
      // e.g. void, cannot log
      return false;
    }

    // Check for presence of a `public static final Struct<?> struct` field on the variable's
    // declared type, eg Rotation2d.struct
    var typeElement = processingEnv.getElementUtils().getTypeElement(processingEnv.getTypeUtils().erasure(type).toString());
    boolean hasStructDeclaration = hasStructDeclaration(typeElement);

    var string = processingEnv.getElementUtils().getTypeElement("java.lang.String");

    if (isArrayComponent) {
      // Only arrays of certain primitives, strings, and structs can be logged
      // Multidimensional arrays are never loggable
      return isLoggablePrimitive(type, true)
          || processingEnv.getTypeUtils().isAssignable(type, string.asType())
          || hasStructDeclaration;
    }

    if (isLoggablePrimitive(type, false) || processingEnv.getTypeUtils().isAssignable(type, string.asType()) || hasStructDeclaration) {
      // All primitives and strings can be logged
      return true;
    }

    var enumType = processingEnv.getElementUtils().getTypeElement("java.lang.Enum");
    if (processingEnv.getTypeUtils().isAssignable(type, processingEnv.getTypeUtils().erasure(enumType.asType()))) {
      // Enum values can be logged
      return true;
    }

    if (type.getAnnotation(Epilogue.class) != null) {
      // Class or one of its superclasses is tagged with @Epilogue
      return true;
    }

    if (type.getKind() == TypeKind.DECLARED) {
      // Like the above, but works for array component types
      var decl = ((DeclaredType) type);
      if (decl.asElement().getAnnotation(Epilogue.class) != null) {
        return true;
      }
    }

    // Check inherited interfaces for @Epilogue annotation
    var tm = type;
    while (!(tm instanceof NoType) && tm != null) {
      var te = processingEnv.getElementUtils().getTypeElement(processingEnv.getTypeUtils().erasure(type).toString());
      if (te == null) {
        break;
      }
      for (TypeMirror iface : te.getInterfaces()) {
        if (iface.getAnnotation(Epilogue.class) != null) {
          return true;
        }
      }
      if (te.getSuperclass() == tm) {
        break;
      }
      tm = te.getSuperclass();
    }

    var rawMeasure = processingEnv.getTypeUtils().erasure(processingEnv.getElementUtils().getTypeElement("edu.wpi.first.units.Measure").asType());
    if (processingEnv.getTypeUtils().isAssignable(type, rawMeasure)) {
      return true;
    }

    if (isSendable(type)) {
      // Early return here to skip the warning message. Don't need to spam the compiler output
      // with warnings about commands not being loggable
      return isLoggableSendableType(type);
    }

    if (type instanceof ArrayType arrayType && arrayType.getComponentType().getKind() != TypeKind.ARRAY) {
      // Can log single-dimensional arrays of loggable types, eg double[] but not double[][]
      return isLoggable(element, arrayType.getComponentType(), true);
    }

    var rawCollection = processingEnv.getTypeUtils().erasure(processingEnv.getElementUtils().getTypeElement("java.util.Collection").asType());
    if (processingEnv.getTypeUtils().isAssignable(type, rawCollection) && type instanceof DeclaredType decl) {
      if (decl.getTypeArguments().size() != 1) {
        // Probably raw type, not loggable
        return false;
      }
      var bound = decl.getTypeArguments().getFirst();
      return isLoggable(element, bound, true);
    }

    processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "[EPILOGUE] Excluded from logs because " + type + " is not a loggable data type", element);

    return false;
  }

  private boolean isLoggablePrimitive(TypeMirror type, boolean isArrayComponent) {
    if (type.getKind().isPrimitive()) {
      if (isArrayComponent) {
        return switch (type.toString()) {
          case "byte", "int", "long", "float", "double", "boolean" -> true;
          default -> false;
        };
      } else {
        return switch (type.toString()) {
          // All
          case "byte", "char", "short", "int", "long", "float", "double", "boolean" -> true;
          default -> false;
        };
      }
    }

    return false;
  }

  private boolean hasStructDeclaration(TypeElement typeElement) {
    if (typeElement == null) {
      return false;
    }

    var serializable = processingEnv.getElementUtils().getTypeElement("edu.wpi.first.util.struct.StructSerializable");

    return processingEnv.getTypeUtils().isAssignable(typeElement.asType(), serializable.asType());
  }

  private boolean isSendable(TypeMirror dataType) {
    var sendableType = processingEnv.getElementUtils().getTypeElement("edu.wpi.first.util.sendable.Sendable").asType();
    return processingEnv.getTypeUtils().isAssignable(dataType, sendableType);
  }

  private boolean isLoggableSendableType(TypeMirror dataType) {
    var sendableType = processingEnv.getElementUtils().getTypeElement("edu.wpi.first.util.sendable.Sendable").asType();
    var commandType = processingEnv.getElementUtils().getTypeElement("edu.wpi.first.wpilibj2.command.Command").asType();
    var subsystemType = processingEnv.getElementUtils().getTypeElement("edu.wpi.first.wpilibj2.command.SubsystemBase").asType();

    return processingEnv.getTypeUtils().isAssignable(dataType, sendableType) &&
        !processingEnv.getTypeUtils().isAssignable(dataType, commandType) &&
        !processingEnv.getTypeUtils().isAssignable(dataType, subsystemType);
  }

  private void logMethod(PrintWriter out, ExecutableElement loggableMethod) {
    var methodName = loggableMethod.getSimpleName().toString();
    var config = loggableMethod.getAnnotation(Epilogue.class);

    String loggedName;
    if (config != null && !config.name().isBlank()) {
      loggedName = config.name();
    } else {
      loggedName = methodName;
    }

    String methodAccess = "object." + methodName + "()";

    loggerCall(out, loggableMethod.getReturnType(), methodName, loggedName, methodAccess);
  }

  private void logField(PrintWriter out, VariableElement loggableField) {
    var fieldName = loggableField.getSimpleName().toString();

    var config = loggableField.getAnnotation(Epilogue.class);
    String loggedName;
    if (config != null && !config.name().isBlank()) {
      loggedName = config.name();
    } else {
      loggedName = fieldName;
    }

    String fieldAccess;
    if (loggableField.getModifiers().contains(Modifier.PRIVATE)) {
      fieldAccess = "(" + loggableField.asType().toString() + ") $" + fieldName + ".get(object)";
    } else {
      fieldAccess = "object." + fieldName;
    }

    loggerCall(out, loggableField.asType(), fieldName, loggedName, fieldAccess);
  }

  private void loggerCall(PrintWriter out, TypeMirror dataType, String codeName, String loggedName, String access) {
    // Sendables are logged using their custom sendable format, EXCEPT for commands and subsystems
    // Commands stored aren't useful to log, and subsystems are much better off logging
    // their contents than their likely empty sendable dataset
    var sendableType = processingEnv.getElementUtils().getTypeElement("edu.wpi.first.util.sendable.Sendable").asType();
    var commandType = processingEnv.getElementUtils().getTypeElement("edu.wpi.first.wpilibj2.command.Command").asType();
    var subsystemType = processingEnv.getElementUtils().getTypeElement("edu.wpi.first.wpilibj2.command.SubsystemBase").asType();
    var collectionType = processingEnv.getElementUtils().getTypeElement("java.util.Collection").asType();
    var measureType = processingEnv.getElementUtils().getTypeElement("edu.wpi.first.units.Measure").asType();

    var reflectedType = getTypeElement(processingEnv.getTypeUtils().erasure(dataType));

    if (processingEnv.getTypeUtils().isAssignable(dataType, sendableType) &&
        !processingEnv.getTypeUtils().isAssignable(dataType, commandType) &&
        !processingEnv.getTypeUtils().isAssignable(dataType, subsystemType)
    ) {
      // Log as sendable
      out.println("      logSendable(dataLogger.getSubLogger(\"" + loggedName + "\"), " + access + ");");
    } else if (reflectedType != null && reflectedType.getAnnotation(Epilogue.class) != null) {
      // Log nested fields that support Epilogue
      out.println("      Epiloguer." + lowerCamelCase(simpleName(reflectedType.getQualifiedName().toString())) + "Logger.tryUpdate(dataLogger.getSubLogger(\"" + loggedName + "\"), " + access + ", Epiloguer.getConfig().errorHandler);");
    } else if (hasStructDeclaration(reflectedType)) {
      // Log with struct serialization
      out.println("      dataLogger.log(\"" + loggedName + "\", " + access + ", " + reflectedType.getQualifiedName() + ".struct);");
    } else if (dataType.getKind() == TypeKind.ARRAY) {
      // Invoke with information about the array type
      // This takes advantage of the fact that the array log calls have EXACTLY the same shape as
      // the non-array log calls, eg log("One Thing", 1) and log("Two Things", { 1, 2 }) only differ
      // in signature on the value parameter
      var componentType = ((ArrayType) dataType).getComponentType();
      loggerCall(out, componentType, codeName, loggedName, access);
    } else if (processingEnv.getTypeUtils().isAssignable(dataType, processingEnv.getTypeUtils().erasure(collectionType)) && dataType instanceof DeclaredType decl) {
      // Can only get here if already determined to be loggable - ie, has a valid generic type bound
      var componentType = decl.getTypeArguments().getFirst();

      var toArry = "(" + access + ").toArray(" + componentType + "[]::new)";
      if (hasStructDeclaration(getTypeElement(componentType))) {
        // Logged as an array of structs, need to provide the serde object
        out.println("      dataLogger.log(\"" + loggedName + "\", " + toArry + ", " + processingEnv.getTypeUtils().erasure(componentType) + ".struct);");
      } else {
        // Not structs, can use one of the `log` methods and let the overloads figure it out
        out.println("      dataLogger.log(\"" + loggedName + "\", " + toArry + ");");
      }
    } else if (processingEnv.getTypeUtils().isAssignable(dataType, processingEnv.getTypeUtils().erasure(measureType))) {
      // Measurement like Measure<Voltage>
      out.println("      dataLogger.log(\"" + loggedName + "\", " + access + ");");
    } else if (processingEnv.getTypeUtils().isAssignable(dataType, processingEnv.getTypeUtils().erasure(processingEnv.getElementUtils().getTypeElement("java.lang.Enum").asType()))) {
      // Enum
      out.println("      dataLogger.log(\"" + loggedName + "\", " + access + ");");
    } else if (processingEnv.getTypeUtils().isAssignable(dataType, processingEnv.getElementUtils().getTypeElement("java.lang.String").asType())) {
      // String
      out.println("      dataLogger.log(\"" + loggedName + "\", " + access + ");");
    } else {
      switch (dataType.toString()) {
        case "byte", "char", "short", "int", "long", "float", "double", "boolean",
            "byte[]", "int[]", "long[]", "float[]", "double[]", "boolean[]",
            "java.lang.String", "java.lang.String[]",
            "edu.wpi.first.wpilibj.XboxController",
            "edu.wpi.first.wpilibj.Joystick" ->
            out.println("      dataLogger.log(\"" + loggedName + "\", " + access + ");");
        default -> {
          var reflectedDataType = getTypeElement(dataType);
          out.println("      // TODO: Support " + dataType + " (" + (reflectedDataType == null ? "<unknown type>" : reflectedDataType.getQualifiedName()) + ") for " + codeName);
        }
      }
    }
  }

  private TypeElement getTypeElement(TypeMirror mirror) {
    return processingEnv.getElementUtils().getTypeElement(mirror.toString());
  }

  private String simpleName(String fqn) {
    return fqn.substring(fqn.lastIndexOf('.') + 1);
  }

  private String upperCamelCase(CharSequence str) {
    return Character.toUpperCase(str.charAt(0)) + str.subSequence(1, str.length()).toString();
  }

  private String lowerCamelCase(CharSequence str) {
    StringBuilder builder = new StringBuilder(str.length());

    int i = 0;
    for (; i < str.length() - 1 && (i == 0 || (Character.isUpperCase(str.charAt(i)) && Character.isUpperCase(str.charAt(i + 1)))); i++) {
      builder.append(Character.toLowerCase(str.charAt(i)));
    }

    builder.append(str.subSequence(i, str.length()));
    return builder.toString();
  }
}
