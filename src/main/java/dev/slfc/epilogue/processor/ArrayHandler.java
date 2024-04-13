package dev.slfc.epilogue.processor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;

/**
 * Arrays of bytes, ints, flats, doubles, booleans, Strings, and struct-serializable objects can
 * be logged. No other array types - including multidimensional arrays - are loggable.
 */
public class ArrayHandler extends ElementHandler {
  private final StructHandler structHandler;
  private final TypeMirror javaLangString;

  protected ArrayHandler(ProcessingEnvironment processingEnv) {
    super(processingEnv);

    // use a struct handler for managing struct arrays
    structHandler = new StructHandler(processingEnv);

    javaLangString = processingEnv.getElementUtils().getTypeElement("java.lang.String").asType();
  }

  @Override
  public boolean isLoggable(Element element) {
    return dataType(element) instanceof ArrayType arr
        && isLoggableComponentType(arr.getComponentType());
  }

  public boolean isLoggableComponentType(TypeMirror type) {
    if (type instanceof PrimitiveType primitive) {
      return switch (primitive.getKind()) {
        case BYTE, INT, LONG, FLOAT, DOUBLE, BOOLEAN -> true;
        default -> false;
      };
    }

    if (structHandler.isLoggableType(type)) {
      return true;
    }

    if (processingEnv.getTypeUtils().isAssignable(type, javaLangString)) {
      return true;
    }

    return false;
  }

  @Override
  public String logInvocation(Element element) {
    var dataType = dataType(element);

    // known to be an array type (assuming isLoggable is checked first); this is a safe cast
    var componentType = ((ArrayType) dataType).getComponentType();

    if (structHandler.isLoggableType(componentType)) {
      // Struct arrays need to pass in the struct serializer
      return "dataLogger.log(\"" + loggedName(element) + "\", " + elementAccess(element) + ", " + structHandler.structAccess(componentType) + ")";
    } else {
      // Primitive or string array
      return "dataLogger.log(\"" + loggedName(element) + "\", " + elementAccess(element) + ")";
    }
  }
}
