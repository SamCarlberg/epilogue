package dev.slfc.epilogue.processor;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import dev.slfc.epilogue.Epilogue;
import dev.slfc.epilogue.NotLogged;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

/**
 * Generates logger class files for {@link dev.slfc.epilogue.Epilogue @Epilogue}-annotated classes.
 */
public class LoggerGenerator {
  private final ProcessingEnvironment processingEnv;
  private final List<ElementHandler> handlers;

  public LoggerGenerator(ProcessingEnvironment processingEnv, List<ElementHandler> handlers) {
    this.processingEnv = processingEnv;
    this.handlers = handlers;
  }

  private static boolean isNotSkipped(Element e) {
    return e.getAnnotation(NotLogged.class) == null;
  }

  public String writeLoggerFile(TypeElement clazz) throws IOException {
    var epilogue = clazz.getAnnotation(Epilogue.class);
    boolean requireExplicitOptIn = epilogue.strategy() == Epilogue.Strategy.OPT_IN;

    Predicate<Element> notSkipped = LoggerGenerator::isNotSkipped;
    Predicate<Element> optedIn = e -> !requireExplicitOptIn || e.getAnnotation(Epilogue.class) != null;

    var fieldsToLog =
        clazz.getEnclosedElements().stream()
            .filter(e -> e instanceof VariableElement)
            .map(e -> (VariableElement) e)
            .filter(notSkipped)
            .filter(optedIn)
            .filter(e -> !e.getModifiers().contains(Modifier.STATIC))
            .filter(this::isLoggable)
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
            .filter(this::isLoggable)
            .toList();

    String loggedClassName = clazz.getQualifiedName().toString();

    writeLoggerFile(loggedClassName, epilogue, fieldsToLog, methodsToLog);

    return loggedClassName;
  }

  public void writeLoggerFile(
      String className,
      Epilogue classConfig,
      List<VariableElement> loggableFields,
      List<ExecutableElement> loggableMethods) throws IOException {
    String packageName = null;
    int lastDot = className.lastIndexOf('.');
    if (lastDot > 0) {
      packageName = className.substring(0, lastDot);
    }

    String simpleClassName = StringUtils.simpleName(className);
    String loggerClassName = className + "Logger";
    String loggerSimpleClassName = loggerClassName.substring(lastDot + 1);

    var loggerFile = processingEnv.getFiler().createSourceFile(loggerClassName);

    var privateFields = loggableFields.stream().filter(e -> e.getModifiers().contains(Modifier.PRIVATE)).toList();
    boolean requiresVarHandles = !privateFields.isEmpty();

    try (var out = new PrintWriter(loggerFile.openWriter())) {
      if (packageName != null) {
        // package com.example;
        out.println("package " + packageName + ";");
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
      out.println("public class " + loggerSimpleClassName + " extends ClassSpecificLogger<" + simpleClassName + "> {");

      if (requiresVarHandles) {
        for (var privateField : privateFields) {
          // This field needs a VarHandle to access.
          // Cache it in the class to avoid lookups
          out.println("  private static final VarHandle $" + privateField.getSimpleName() + ";");
        }
        out.println();

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

      out.println("  public " + loggerSimpleClassName + "() {");
      out.println("    super(" + simpleClassName + ".class);");
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
          // findFirst for prioritization
          var handler = handlers.stream().filter(h -> h.isLoggable(loggableElement)).findFirst();

          handler.ifPresent(h -> {
            // May be null if the handler consumes the element but does not actually want it to be
            // logged. For example, the sendable handler consumes all sendable types but does not
            // log commands or subsystems, to prevent excessive warnings about unloggable commands.
            var logInvocation = h.logInvocation(loggableElement);
            if (logInvocation != null) {
              out.println(logInvocation.indent(6).stripTrailing() + ";");
            }
          });
        }

        out.println("    }");
      });

      out.println("  }");
      out.println("}");
    }
  }

  private boolean isLoggable(Element element) {
    return handlers.stream().anyMatch(h -> h.isLoggable(element));
  }
}
