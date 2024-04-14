package dev.slfc.epilogue.processor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;

public class MeasureHandler extends ElementHandler {
  private final TypeMirror measure;

  protected MeasureHandler(ProcessingEnvironment processingEnv) {
    super(processingEnv);

    measure = processingEnv.getTypeUtils().erasure(processingEnv.getElementUtils().getTypeElement("edu.wpi.first.units.Measure").asType());
  }

  @Override
  public boolean isLoggable(Element element) {
    return processingEnv.getTypeUtils().isAssignable(dataType(element), measure);
  }

  @Override
  public String logInvocation(Element element) {
    // DataLogger has builtin support for logging measures
    return "dataLogger.log(\"" + loggedName(element) + "\", " + elementAccess(element) + ")";
  }
}
