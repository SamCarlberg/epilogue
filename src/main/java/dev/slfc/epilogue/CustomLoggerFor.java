package dev.slfc.epilogue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Placed on a subclass of {@link dev.slfc.epilogue.logging.ClassSpecificLogger}. Epilogue will
 * detect it at compile time and allow logging of data types copmatible with the logger.
 * {@snippet lang = java:
 * @CustomLoggerFor(VendorMotorType.class)
 * class ExampleMotorLogger extends ClassSpecificLogger<VendorMotorType> {
 * }
 *}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CustomLoggerFor {
  /**
   * The class of objects able to be logged with the annotated logger.
   */
  Class<?> value();
}
