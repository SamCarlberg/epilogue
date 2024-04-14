package dev.slfc.epilogue.processor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

/**
 * Collections of strings and structs are loggable. Collections of boxed primitive types are not.
 */
public class CollectionHandler extends ElementHandler {
  private final ArrayHandler arrayHandler;
  private final TypeMirror collectionType;
  private final StructHandler structHandler;

  protected CollectionHandler(ProcessingEnvironment processingEnv) {
    super(processingEnv);
    arrayHandler = new ArrayHandler(processingEnv);
    collectionType = processingEnv.getElementUtils().getTypeElement("java.util.Collection").asType();
    structHandler = new StructHandler(processingEnv);
  }

  @Override
  public boolean isLoggable(Element element) {
    var dataType = dataType(element);

    return processingEnv.getTypeUtils().isAssignable(dataType, processingEnv.getTypeUtils().erasure(collectionType))
        && dataType instanceof DeclaredType decl
        && decl.getTypeArguments().size() == 1
        && arrayHandler.isLoggableComponentType(decl.getTypeArguments().getFirst());
  }

  @Override
  public String logInvocation(Element element) {
    var dataType = dataType(element);
    var componentType = ((DeclaredType) dataType).getTypeArguments().getFirst();

    if (structHandler.isLoggableType(componentType)) {
      return "dataLogger.log(\"" + loggedName(element) + "\", " + elementAccess(element) + ", " + structHandler.structAccess(componentType) + ")";
    } else {
      return "dataLogger.log(\"" + loggedName(element) + "\", " + elementAccess(element) + ")";
    }
  }
}
