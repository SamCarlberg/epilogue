package dev.slfc.epilogue.processor;

import dev.slfc.epilogue.Epilogue;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

/**
 * Handles logging for types annotated with the {@link Epilogue @Epilogue} annotation.
 */
public class LoggableHandler extends ElementHandler {
  protected LoggableHandler(ProcessingEnvironment processingEnv) {
    super(processingEnv);
  }

  @Override
  public boolean isLoggable(Element element) {
    var dataType = dataType(element);
    return dataType.getAnnotation(Epilogue.class) != null
        || (dataType instanceof DeclaredType decl && decl.asElement().getAnnotation(Epilogue.class) != null);
  }

  @Override
  public String logInvocation(Element element) {
    TypeMirror dataType = dataType(element);
    var reflectedType =
        processingEnv.getElementUtils().getTypeElement(processingEnv.getTypeUtils().erasure(dataType).toString());

    return "Epiloguer." + StringUtils.lowerCamelCase(reflectedType.getSimpleName()) + "Logger"
        + ".tryUpdate(dataLogger.getSubLogger(\"" + loggedName(element) + "\"), "
        + elementAccess(element)
        + ", Epiloguer.getConfig().errorHandler)";
  }
}
