package dev.slfc.epilogue.processor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;

public class SendableHandler extends ElementHandler {
  private final TypeMirror sendableType;
  private final TypeMirror commandType;
  private final TypeMirror subsystemType;

  public SendableHandler(ProcessingEnvironment processingEnv) {
    super(processingEnv);

    sendableType = processingEnv.getElementUtils().getTypeElement("edu.wpi.first.util.sendable.Sendable").asType();
    commandType = processingEnv.getElementUtils().getTypeElement("edu.wpi.first.wpilibj2.command.Command").asType();
    subsystemType = processingEnv.getElementUtils().getTypeElement("edu.wpi.first.wpilibj2.command.SubsystemBase").asType();
  }

  @Override
  public boolean isLoggable(Element element) {
    var dataType = dataType(element);

    // Accept any sendable type. However, the log invocation will return null
    // for sendable types that should not be logged (commands, subsystems)
    return processingEnv.getTypeUtils().isAssignable(dataType, sendableType);
  }

  @Override
  public String logInvocation(Element element) {
    var dataType = dataType(element);

    if (processingEnv.getTypeUtils().isAssignable(dataType, commandType) ||
        processingEnv.getTypeUtils().isAssignable(dataType, subsystemType)) {
      return null;
    }

    return "logSendable(dataLogger.getSubLogger(\""
        + loggedName(element)
        + "\"), "
        + elementAccess(element)
        + ")";
  }
}
