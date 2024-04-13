package dev.slfc.epilogue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Place this annotation on a class to automatically log every field and every public accessor
 * method (methods with no arguments and return a loggable data type). Use {@link #strategy()} to
 * flag a class as logging everything it can, except for those elements tagged with
 * {@code @Epilogue(importance = NONE)}; or for logging only specific items also tagged with
 * {@code @Epilogue}.
 *
 * <p>Logged fields may have any access modifier. Logged methods must be public; non-public
 * methods will be ignored.
 *
 * <p>Epilogue can log all primitive types, arrays of primitive types (except char and short),
 * Strings, arrays of Strings, sendable objects, objects with a struct serializer, and arrays
 * of objects with struct serializers.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
public @interface Epilogue {
  /**
   * The name for the annotated element to be logged as. Does nothing on class-level annotations.
   * Fields and methods will default to be logged using their in-code names; use this attribute to
   * set it to something custom.
   */
  String name() default "";

  enum Strategy {
    /**
     * Log everything except for those elements explicitly opted out of with the skip = true
     * attribute. This is the default behavior.
     */
    OPT_OUT,

    /**
     * Log only fields and methods tagged with an {@link Epilogue} annotation.
     */
    OPT_IN
  }

  /**
   * The strategy to use for logging. Only has an effect on annotations on class or interface
   * declarations.
   */
  Strategy strategy() default Strategy.OPT_OUT;

  enum Importance {
    /**
     * The annotated element has no importance whatsoever and should never be logged.
     */
    NONE,

    /**
     * Debug information. Useful for low-level information like raw sensor values.
     */
    DEBUG,

    /**
     * Informational data. Useful for higher-level information like pose estimates or subsystem
     * state.
     */
    INFO,

    /**
     * Critical data that should always be present in logs.
     */
    CRITICAL
  }

  /**
   * The importance of the annotated data. If placed on a class or interface, this will be the
   * default importance of all data within that class; this can be overridden on a per-element
   * basis by annotating fields and methods with their own {@code @Epilogue(importance = ...)}
   * annotation.
   */
  Importance importance() default Importance.DEBUG;
}
