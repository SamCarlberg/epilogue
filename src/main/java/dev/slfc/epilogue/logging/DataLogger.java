package dev.slfc.epilogue.logging;

import edu.wpi.first.units.Measure;
import edu.wpi.first.util.struct.Struct;

public interface DataLogger {
  static DataLogger multi(DataLogger... loggers) {
    return new MultiLogger(loggers);
  }

  void log(String identifier, int value);

  void log(String identifier, long value);

  void log(String identifier, float value);

  void log(String identifier, double value);

  void log(String identifier, boolean value);

  void log(String identifier, byte[] value);

  void log(String identifier, int[] value);

  void log(String identifier, long[] value);

  void log(String identifier, float[] value);

  void log(String identifier, double[] value);

  void log(String identifier, boolean[] value);

  void log(String identifier, String value);

  void log(String identifier, String[] value);

  <S> void log(String identifier, S value, Struct<S> struct);

  <S> void log(String identifier, S[] value, Struct<S> struct);

  /**
   * Logs a measurement's value in terms of its base unit.
   * @param identifier the identifier of the data field
   * @param value the new value of the data field
   */
  default void log(String identifier, Measure<?> value) {
    log(identifier, value.baseUnitMagnitude());
  }

  // TODO: Add default methods to support common no-struct no-sendable types like joysticks?
}
