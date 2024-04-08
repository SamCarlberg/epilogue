package dev.slfc.epilogue.logging;

import dev.slfc.epilogue.logging.errors.ErrorHandler;
import edu.wpi.first.util.sendable.Sendable;
import edu.wpi.first.util.sendable.SendableBuilder;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Base class for class-specific generated loggers. Loggers are generated at compile time by
 * the Epilogue annotation processor and are used at runtime for zero-overhead data logging.
 */
@SuppressWarnings("unused") // Used by generated subclasses
public abstract class ClassSpecificLogger<T> {
  private final Class<T> clazz;
  private final MethodHandles.Lookup lookup;

  // TODO: This will hold onto Sendables that are otherwise no longer referenced by a robot program.
  //       Determine if that's a concern
  // Linked hashmap to maintain insert order
  private final Map<Sendable, SendableBuilder> sendables = new LinkedHashMap<>();

  private boolean disabled = false;

  /**
   * @param clazz the Java class of objects that can be logged
   */
  protected ClassSpecificLogger(Class<T> clazz) {
    this.clazz = clazz;
    try {
      lookup = MethodHandles.privateLookupIn(clazz, MethodHandles.lookup());
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Updates an object's fields in a data log under the given identifier.
   *
   * @param dataLogger the logger to update
   * @param identifier the identifier for the object. Nested attributes will be appended using a "/"
   *                   delimiter
   * @param object the object to update in the log
   */
  protected abstract void update(DataLogger dataLogger, String identifier, T object);

  public final void tryUpdate(
      DataLogger dataLogger,
      String identifier,
      T object,
      ErrorHandler errorHandler) {
    if (disabled) {
      return;
    }

    try {
      update(dataLogger, identifier, object);
    } catch (Exception e) {
      errorHandler.handle(e, this);
    }
  }

  /**
   * Checks if this logger has been disabled.
   */
  public final boolean isDisabled() {
    return disabled;
  }

  /**
   * Disables this logger. Any log calls made while disabled will be ignored.
   */
  public final void disable() {
    disabled = true;
  }

  /**
   * Reenables this logger after being disabled. Has no effect if the logger is already enabled.
   */
  public final void reenable() {
    disabled = false;
  }

  /**
   * Gets the type of the data this logger accepts.
   */
  public final Class<T> getLoggedType() {
    return clazz;
  }

  protected void logSendable(DataLogger dataLogger, String identifier, Sendable sendable) {
    if (sendable == null) {
      return;
    }

    var builder = sendables.computeIfAbsent(sendable, (s) -> {
      var b = new LogBackedSendableBuilder(dataLogger, identifier);
      s.initSendable(b);
      return b;
    });
    builder.update();
  }
}
