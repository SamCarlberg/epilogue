package dev.slfc.epilogue.logging;

import edu.wpi.first.util.struct.Struct;
import edu.wpi.first.util.struct.StructSerializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * A data logger implementation that only logs data when it changes. Useful for keeping bandwidth
 * and file sizes down. However, because it still needs to check that data has changed, it cannot
 * avoid expensive sensor reads.
 */
public class LazyLogger implements DataLogger {
  private final DataLogger logger;

  // Keep a record of the most recent value written to each entry
  // Note that this may duplicate a lot of data, and will box primitives.
  private final Map<String, Object> previousValues = new HashMap<>();
  private final Map<String, SubLogger> subLoggers = new HashMap<>();

  /**
   *
   * @param logger the logger to delegate to
   */
  public LazyLogger(DataLogger logger) {
    this.logger = logger;
  }

  @Override
  public DataLogger lazy() {
    // Already lazy, don't need to wrap it again
    return this;
  }

  @Override
  public DataLogger getSubLogger(String path) {
    return subLoggers.computeIfAbsent(path, k -> new SubLogger(k, this));
  }

  @Override
  public void log(String identifier, int value) {
    var previous = previousValues.get(identifier);

    if (previous instanceof Integer oldValue && oldValue == value) {
      // no change
      return;
    }

    previousValues.put(identifier, value);
    logger.log(identifier, value);
  }

  @Override
  public void log(String identifier, long value) {
    var previous = previousValues.get(identifier);

    if (previous instanceof Long oldValue && oldValue == value) {
      // no change
      return;
    }

    previousValues.put(identifier, value);
    logger.log(identifier, value);
  }

  @Override
  public void log(String identifier, float value) {
    var previous = previousValues.get(identifier);

    if (previous instanceof Float oldValue && oldValue == value) {
      // no change
      return;
    }

    previousValues.put(identifier, value);
    logger.log(identifier, value);
  }

  @Override
  public void log(String identifier, double value) {
    var previous = previousValues.get(identifier);

    if (previous instanceof Double oldValue && oldValue == value) {
      // no change
      return;
    }

    previousValues.put(identifier, value);
    logger.log(identifier, value);
  }

  @Override
  public void log(String identifier, boolean value) {
    var previous = previousValues.get(identifier);

    if (previous instanceof Boolean oldValue && oldValue == value) {
      // no change
      return;
    }

    previousValues.put(identifier, value);
    logger.log(identifier, value);
  }

  @Override
  public void log(String identifier, byte[] value) {
    var previous = previousValues.get(identifier);

    if (previous instanceof byte[] oldValue && Arrays.equals(oldValue, value)) {
      // no change
      return;
    }

    previousValues.put(identifier, value);
    logger.log(identifier, value);
  }

  @Override
  public void log(String identifier, int[] value) {
    var previous = previousValues.get(identifier);

    if (previous instanceof int[] oldValue && Arrays.equals(oldValue, value)) {
      // no change
      return;
    }

    previousValues.put(identifier, value);
    logger.log(identifier, value);
  }

  @Override
  public void log(String identifier, long[] value) {
    var previous = previousValues.get(identifier);

    if (previous instanceof long[] oldValue && Arrays.equals(oldValue, value)) {
      // no change
      return;
    }

    previousValues.put(identifier, value);
    logger.log(identifier, value);
  }

  @Override
  public void log(String identifier, float[] value) {
    var previous = previousValues.get(identifier);

    if (previous instanceof float[] oldValue && Arrays.equals(oldValue, value)) {
      // no change
      return;
    }

    previousValues.put(identifier, value);
    logger.log(identifier, value);
  }

  @Override
  public void log(String identifier, double[] value) {
    var previous = previousValues.get(identifier);

    if (previous instanceof double[] oldValue && Arrays.equals(oldValue, value)) {
      // no change
      return;
    }

    previousValues.put(identifier, value);
    logger.log(identifier, value);
  }

  @Override
  public void log(String identifier, boolean[] value) {
    var previous = previousValues.get(identifier);

    if (previous instanceof boolean[] oldValue && Arrays.equals(oldValue, value)) {
      // no change
      return;
    }

    previousValues.put(identifier, value);
    logger.log(identifier, value);
  }

  @Override
  public void log(String identifier, String value) {
    var previous = previousValues.get(identifier);

    if (previous instanceof String oldValue && oldValue.equals(value)) {
      // no change
      return;
    }

    previousValues.put(identifier, value);
    logger.log(identifier, value);
  }

  @Override
  public void log(String identifier, String[] value) {
    var previous = previousValues.get(identifier);

    if (previous instanceof String[] oldValue && Arrays.equals(oldValue, value)) {
      // no change
      return;
    }

    previousValues.put(identifier, value);
    logger.log(identifier, value);
  }

  @Override
  public <S> void log(String identifier, S value, Struct<S> struct) {
    var previous = previousValues.get(identifier);

    if (previous instanceof Object oldValue && oldValue.equals(value)) {
      // no change
      return;
    }

    previousValues.put(identifier, value);
    logger.log(identifier, value, struct);
  }

  @Override
  public <S> void log(String identifier, S[] value, Struct<S> struct) {
    var previous = previousValues.get(identifier);

    if (previous instanceof Object[] oldValue && Arrays.equals(oldValue, value)) {
      // no change
      return;
    }

    previousValues.put(identifier, value);
    logger.log(identifier, value, struct);
  }
}
