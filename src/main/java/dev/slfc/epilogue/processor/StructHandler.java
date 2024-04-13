package dev.slfc.epilogue.processor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

public class StructHandler extends ElementHandler {
  private final TypeMirror serializable;
  private final Types typeUtils;

  protected StructHandler(ProcessingEnvironment processingEnv) {
    super(processingEnv);
    serializable = processingEnv.getElementUtils().getTypeElement("edu.wpi.first.util.struct.StructSerializable").asType();
    typeUtils = processingEnv.getTypeUtils();
  }

  @Override
  public boolean isLoggable(Element element) {
    return typeUtils.isAssignable(dataType(element), serializable);
  }

  public boolean isLoggableType(TypeMirror type) {
    return typeUtils.isAssignable(type, serializable);
  }

  public String structAccess(TypeMirror serializableType) {
    var className = typeUtils.erasure(serializableType).toString();
    return className + ".struct";
  }

  @Override
  public String logInvocation(Element element) {
    return "dataLogger.log(\"" + loggedName(element) + "\", " + elementAccess(element) + ", " + structAccess(dataType(element)) + ")";
  }
}
