package dev.slfc.epilogue.processor;

import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

public class ConfiguredLoggerHandler extends ElementHandler {
  private final Map<TypeMirror, DeclaredType> customLoggers;

  protected ConfiguredLoggerHandler(ProcessingEnvironment processingEnv, Map<TypeMirror, DeclaredType> customLoggers) {
    super(processingEnv);

    this.customLoggers = customLoggers;
  }

  @Override
  public boolean isLoggable(Element element) {
    return customLoggers.containsKey(dataType(element));
  }

  @Override
  public String logInvocation(Element element) {
    var dataType = dataType(element);
    var loggerType = customLoggers.get(dataType);

    return "Epiloguer." + StringUtils.lowerCamelCase(loggerType.asElement().getSimpleName())
        + ".tryUpdate(dataLogger.getSubLogger(\"" + loggedName(element) + "\"), "
        + elementAccess(element)
        + ", Epiloguer.getConfig().errorHandler)";
  }
}
