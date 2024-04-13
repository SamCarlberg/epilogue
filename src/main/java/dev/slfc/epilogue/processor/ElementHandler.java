package dev.slfc.epilogue.processor;

import dev.slfc.epilogue.Epilogue;
import dev.slfc.epilogue.logging.DataLogger;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

/**
 * Handles logging of fields or methods. An element that passes the {@link #isLoggable(Element)}
 * check guarantees that {@link #logInvocation(Element)} will generate a code snippet that will
 * log that element. Some subclasses may return {@code null} for the invocation to signal that
 * the element should not be logged, but still be considered loggable for the purposes of
 * error messaging during the compilation phase.
 */
public abstract class ElementHandler {
  protected final ProcessingEnvironment processingEnv;

  protected ElementHandler(ProcessingEnvironment processingEnv) {
    this.processingEnv = processingEnv;
  }

  /**
   * Gets the type of data that would be logged by a field or method.
   * @param element the field or method element to check
   * @return the logged datatype
   */
  protected TypeMirror dataType(Element element) {
    return switch (element) {
      case VariableElement field -> field.asType();
      case ExecutableElement method -> method.getReturnType();
      default -> throw new IllegalStateException("Unexpected" + element.getClass().getName());
    };
  }

  /**
   * Gets the name of a field or method as it would appear in logs.
   *
   * @param element the field or method element to check
   * @return the name specified in the {@link Epilogue @Epilogue} annotation on the element,
   * if present; otherwise, the field or method's name with no modifications
   */
  public String loggedName(Element element) {
    var elementName = element.getSimpleName().toString();
    var config = element.getAnnotation(Epilogue.class);

    if (config != null && !config.name().isBlank()) {
      return config.name();
    } else {
      return elementName;
    }
  }

  /**
   * Generates the code snippet to use to access a field or method on a logged object. Private
   * fields are accessed via {@link java.lang.invoke.VarHandle VarHandles} and private methods
   * are accessed via {@link java.lang.invoke.MethodHandle MethodHandles} (note that this requires
   * the logger file to generate those fields). Because the generated logger files are in the same
   * package as the logged type, package-private, protected, and public fields and methods are
   * always accessible using normal field reads and method calls. Values returned by
   * {@code VarHandle} and {@code MethodHandle} invocations will be cast to the appropriate data
   * type.
   *
   * @param element the element to generate the access for
   * @return the generated access snippet
   */
  public String elementAccess(Element element) {
    return switch (element) {
      case VariableElement field -> {
        if (field.getModifiers().contains(Modifier.PRIVATE)) {
          // (com.example.Foo) $fooField.get(object)
          yield "(" + field.asType() + ") $" + field.getSimpleName() + ".get(object)";
        } else {
          // object.fooField
          yield "object." + field.getSimpleName();
        }
      }
      case ExecutableElement method -> {
        if (method.getModifiers().contains(Modifier.PRIVATE)) {
          // (com.example.Foo) _getFoo.invoke(object)
          yield "(" + method.getReturnType() + ") _" + method.getSimpleName() + ".invoke(object)";
        } else {
          // object.getFoo()
          yield "object." + method.getSimpleName() + "()";
        }
      }
      default -> throw new IllegalStateException("Unexpected" + element.getClass().getName());
    };
  }

  /**
   * Checks if a field or method can be logged by this handler.
   * @param element the field or method element to check
   * @return true if the element can be logged, false if not
   */
  public abstract boolean isLoggable(Element element);

  /**
   * Generates a code snippet to place in a generated logger file to log the value of a field or
   * method. Log invocations are placed in a generated implementation of
   * {@link dev.slfc.epilogue.logging.ClassSpecificLogger#update(DataLogger, Object)}, with access
   * to the data logger and logged object passed to the method call.
   *
   * @param element the field or method element to generate the logger call for
   * @return the generated log invocation
   */
  public abstract String logInvocation(Element element);
}
