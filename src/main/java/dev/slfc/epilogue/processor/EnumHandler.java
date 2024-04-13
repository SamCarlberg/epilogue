package dev.slfc.epilogue.processor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

public class EnumHandler extends ElementHandler {

  private final TypeMirror javaLangEnum;

  protected EnumHandler(ProcessingEnvironment processingEnv) {
    super(processingEnv);

    // raw type java.lang.Enum
    javaLangEnum = processingEnv.getTypeUtils().erasure(processingEnv.getElementUtils().getTypeElement("java.lang.Enum").asType());
  }

  @Override
  public boolean isLoggable(Element element) {
    return processingEnv.getTypeUtils().isAssignable(dataType(element), javaLangEnum);
  }

  @Override
  public String logInvocation(Element element) {
    return "dataLogger.log(\"" + loggedName(element) + "\", " + elementAccess(element) + ")";
  }
}
