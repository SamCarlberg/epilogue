package dev.slfc.epilogue.processor;

import static java.util.stream.Collectors.*;

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
import javax.annotation.processing.FilerException;
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
                .filter(e -> isLoggable(e.asType()))
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
                .filter(e -> isLoggable(e.getReturnType()))
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

    writeEpiloguerFile(loggerClassNames, mainRobotClass);

    return true;
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

        if (isTimedRobot) {
          out.println("import edu.wpi.first.wpilibj.TimedRobot;");
        }

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
            """);

        out.println("""
              /**
               * Handles an error in Epilogue. By default, this will print the error message to the
               * console, but this can be overridden to throw an exception instead by setting the
               * {@code crashOnError} configuration option to {@code true}.
               *
               * @param errorMessage the error message to handle
               */
              public static void handleError(String errorMessage) {
                if (config.crashOnError) {
                  throw new IllegalStateException(errorMessage);
                } else {
                  System.err.println(errorMessage);
                }
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
          out.println("      " + lowerCamelCase(simpleName(robotClassName)) + "Logger.update(config.dataLogger, \"Robot\", robot);");
          out.println("    }, robot.getPeriod(), robot.getPeriod() / 2);");
          out.println("  }");
        }

        out.println("}");
      }
    } catch (FilerException e) {
      // Probably from trying to create the file multiple times in the same compilation step
      // We can ignore this, since the file contents shouldn't change
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
      out.println("import dev.slfc.epilogue.logging.DataLogger;");
      out.println("import dev.slfc.epilogue.logging.ClassSpecificLogger;");
      out.println("import java.lang.invoke.VarHandle;");
      out.println();

      // public class FooLogger implements ClassSpecificLogger<Foo> {
      out.print("public class ");
      out.print(loggerSimpleClassName);
      out.print(" extends ClassSpecificLogger<");
      out.print(simpleClassName);
      out.println("> {");

      for (var loggableField : loggableFields) {
        if (loggableField.getModifiers().contains(Modifier.PRIVATE)) {
          // This field needs a VarHandle to access.
          // Cache it in the class to avoid lookups
          out.println("  private final VarHandle $" + loggableField.getSimpleName() + ";");
        }
      }
      out.println();

      out.print("  public ");
      out.print(loggerSimpleClassName);
      out.println("() {");
      out.print("    super(");
      out.print(simpleClassName);
      out.println(".class);");

      for (var loggableField : loggableFields) {
        if (loggableField.getModifiers().contains(Modifier.PRIVATE)) {
          // This field needs a VarHandle to access.
          // Cache it in the class to avoid lookups
          var fieldName = loggableField.getSimpleName();
          out.println("    $" + fieldName + " = fieldHandle(\"" + fieldName + "\", " + processingEnv.getTypeUtils().erasure(loggableField.asType()) + ".class);");
        }
      }
      out.println("  }");
      out.println();


      // @Override
      // public void update(DataLogger dataLogger, String identifier, Foo object) {
      out.println("  @Override");
      out.print("  public void update(DataLogger dataLogger, String identifier, ");
      out.print(simpleClassName);
      out.println(" object) {");

      // try {
      //    [log fields]
      //    [log methods]
      // } catch (Exception e) {
      //   System.err.println("[EPILOGUE] Encountered an error while logging: " + e.getMessage());
      // }
      out.println("    try {");

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
        out.println("      if (Epiloguer.shouldLog(Epilogue.Importance." + importance.name() + ")) {");
        for (var loggableElement : elements) {
          switch (loggableElement) {
            case VariableElement field -> logField(out, field);
            case ExecutableElement method -> logMethod(out, method);
            default -> {} // Ignore
          }
        }
        out.println("      }");
      });

      out.println("    } catch (Exception e) {");
      out.println("      System.err.println(\"[EPILOGUE] Encountered an error while logging: \" + e.getMessage());");
      out.println("    }");
      out.println("  }");
      out.println("}");
    }
  }

  private boolean isLoggable(TypeMirror type) {
    if (type instanceof NoType) {
      // e.g. void, cannot log
      return false;
    }

    if (type.getKind().isPrimitive()) {
      // All primitives can be logged
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
          processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "@Epilogue tagged interface detected: " + iface, te);
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

    if (isLoggableSendableType(type)) {
      // Can log sendables, but not commands or subsystems
      return true;
    }

    // Check for presence of a `public static final Struct<?> struct` field on the variable's
    // declared type, eg Rotation2d.struct
    var typeElement = processingEnv.getElementUtils().getTypeElement(processingEnv.getTypeUtils().erasure(type).toString());
    if (hasStructDeclaration(typeElement)) {
      return true;
    }

    if (type instanceof ArrayType arrayType && arrayType.getComponentType().getKind() != TypeKind.ARRAY) {
      // Can log single-dimensional arrays of loggable types, eg double[] but not double[][]
      return isLoggable(arrayType.getComponentType());
    }

    processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Cannot log values of type " + type + ". Any declared loggable elements of this type will be excluded from logs.");

    return false;
  }

  private boolean hasStructDeclaration(TypeElement typeElement) {
    if (typeElement == null) {
      return false;
    }

    TypeElement struct = processingEnv.getElementUtils().getTypeElement("edu.wpi.first.util.struct.Struct");
    var structType = struct.asType();

    for (Element enclosedElement : typeElement.getEnclosedElements()) {
      if (enclosedElement instanceof VariableElement varElement) {
        if (varElement.getSimpleName().contentEquals("struct")) {
          Set<Modifier> modifiers = varElement.getModifiers();
          if (modifiers.contains(Modifier.PUBLIC) &&
              modifiers.contains(Modifier.STATIC) &&
              modifiers.contains(Modifier.FINAL)) {

            TypeMirror variableType = varElement.asType();
            if (processingEnv.getTypeUtils().isAssignable(variableType, processingEnv.getTypeUtils().erasure(structType))) {
              // The type has a declared `public static final Struct<Foo> struct` field,
              // which lets us serialize to raw bytes
              return true;
            }
          }
        }
      }
    }

    return false;
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
    var listType = processingEnv.getElementUtils().getTypeElement("java.util.List").asType();
    var measureType = processingEnv.getElementUtils().getTypeElement("edu.wpi.first.units.Measure").asType();

    var reflectedType = getTypeElement(processingEnv.getTypeUtils().erasure(dataType));

    if (processingEnv.getTypeUtils().isAssignable(dataType, sendableType) &&
        !processingEnv.getTypeUtils().isAssignable(dataType, commandType) &&
        !processingEnv.getTypeUtils().isAssignable(dataType, subsystemType)
    ) {
      // Log as sendable
      out.println("        logSendable(dataLogger, identifier + \"/" + loggedName + "\", " + access + ");");
    } else if (reflectedType != null && reflectedType.getAnnotation(Epilogue.class) != null) {
      // Log nested fields that support Epilogue
      out.println("        Epiloguer." + lowerCamelCase(simpleName(reflectedType.getQualifiedName().toString())) + "Logger.update(dataLogger, identifier + \"/" + loggedName + "\", " + access + ");");
    } else if (hasStructDeclaration(reflectedType)) {
      // Log with struct serialization
      out.println("        dataLogger.log(identifier + \"/" + loggedName + "\", " + access + ", " + reflectedType.getQualifiedName() + ".struct);");
    } else if (dataType.getKind() == TypeKind.ARRAY) {
      // Invoke with information about the array type
      // This takes advantage of the fact that the array log calls have EXACTLY the same shape as
      // the non-array log calls, eg log("One Thing", 1) and log("Two Things", { 1, 2 }) only differ
      // in signature on the value parameter
      var componentType = ((ArrayType) dataType).getComponentType();
      loggerCall(out, componentType, codeName, loggedName, access);
    } else if (processingEnv.getTypeUtils().isAssignable(dataType, processingEnv.getTypeUtils().erasure(listType))) {
      out.println("        // TODO: Log " + loggedName + " as an array (if possible)");
    } else if (processingEnv.getTypeUtils().isAssignable(dataType, processingEnv.getTypeUtils().erasure(measureType))) {
      out.println("        dataLogger.log(identifier + \"/" + loggedName + "\", " + access + ");");
    } else {
      switch (dataType.toString()) {
        case "byte", "char", "short", "int", "long", "float", "double", "boolean",
            "byte[]", "int[]", "long[]", "float[]", "double[]", "boolean[]",
            "edu.wpi.first.wpilibj.XboxController",
            "edu.wpi.first.wpilibj.Joystick" ->
            out.println("        dataLogger.log(identifier + \"/" + loggedName + "\", " + access + ");");
        default -> {
          var reflectedDataType = getTypeElement(dataType);
          out.println("        // TODO: Support " + dataType + " (" + (reflectedDataType == null ? "<unknown type>" : reflectedDataType.getQualifiedName()) + ") for " + codeName);
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
