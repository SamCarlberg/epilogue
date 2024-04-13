package dev.slfc.epilogue.processor;

import com.google.auto.service.AutoService;
import dev.slfc.epilogue.Epilogue;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
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
import javax.lang.model.type.NoType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

@SupportedAnnotationTypes("dev.slfc.epilogue.Epilogue")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@AutoService(Processor.class)
public class AnnotationProcessor extends AbstractProcessor {
  private EpiloguerGenerator epiloguerGenerator;
  private LoggerGenerator loggerGenerator;
  private List<ElementHandler> handlers;

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);

    // Handlers are declared in order of priority. If an element could be logged in more than one
    // way (eg a class implements both Sendable and StructSerializable), the order of the handlers
    // in this list will determine how it gets logged.
    handlers = List.of(
        new LoggableHandler(processingEnv), // prioritize epilogue logging over Sendable

        new ArrayHandler(processingEnv),
        new CollectionHandler(processingEnv),
        new EnumHandler(processingEnv),
        new MeasureHandler(processingEnv),
        new PrimitiveHandler(processingEnv),
        new StructHandler(processingEnv), // prioritize struct over sendable
        new SendableHandler(processingEnv)
    );

    epiloguerGenerator = new EpiloguerGenerator(processingEnv);
    loggerGenerator = new LoggerGenerator(processingEnv, handlers);
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    List<String> loggerClassNames = new ArrayList<>();
    var mainRobotClasses = new ArrayList<TypeElement>();

    // Used to check for a main robot class
    var robotBaseClass = processingEnv.getElementUtils().getTypeElement("edu.wpi.first.wpilibj.RobotBase").asType();

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
        try {
          String loggedClassName = loggerGenerator.writeLoggerFile(clazz);

          if (processingEnv.getTypeUtils().isAssignable(clazz.getSuperclass(), robotBaseClass)) {
            mainRobotClasses.add(clazz);
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
      // Sort alphabetically
      mainRobotClasses.sort(Comparator.comparing(c -> c.getSimpleName().toString()));
      epiloguerGenerator.writeEpiloguerFile(loggerClassNames, mainRobotClasses);
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
          if (isNotLoggable(field, field.asType())) {
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
          if (isNotLoggable(method, method.getReturnType())) {
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
   * Checks if a type is not loggable.
   *
   * @param type the type to check
   */
  private boolean isNotLoggable(Element element, TypeMirror type) {
    if (type instanceof NoType) {
      // e.g. void, cannot log
      return true;
    }

    boolean loggable = handlers.stream().anyMatch(h -> h.isLoggable(element));

    if (loggable) {
      return false;
    }

    processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "[EPILOGUE] Excluded from logs because " + type + " is not a loggable data type", element);
    return true;
  }
}
