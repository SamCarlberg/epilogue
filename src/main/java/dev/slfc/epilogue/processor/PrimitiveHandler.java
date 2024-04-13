package dev.slfc.epilogue.processor;

import static javax.lang.model.type.TypeKind.BOOLEAN;
import static javax.lang.model.type.TypeKind.BYTE;
import static javax.lang.model.type.TypeKind.CHAR;
import static javax.lang.model.type.TypeKind.DOUBLE;
import static javax.lang.model.type.TypeKind.FLOAT;
import static javax.lang.model.type.TypeKind.INT;
import static javax.lang.model.type.TypeKind.LONG;
import static javax.lang.model.type.TypeKind.SHORT;

import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

public class PrimitiveHandler extends ElementHandler {
  private final TypeMirror javaLangString;
  protected PrimitiveHandler(ProcessingEnvironment processingEnv) {
    super(processingEnv);

    javaLangString = processingEnv.getElementUtils().getTypeElement("java.lang.String").asType();
  }

  @Override
  public boolean isLoggable(Element element) {
    return processingEnv.getTypeUtils().isAssignable(dataType(element), javaLangString)
        || Set.of(BYTE, CHAR, SHORT, INT, LONG, FLOAT, DOUBLE, BOOLEAN).contains(dataType(element).getKind());
  }

  @Override
  public String logInvocation(Element element) {
    return "dataLogger.log(\"" + loggedName(element) + "\", " + elementAccess(element) + ")";
  }
}
