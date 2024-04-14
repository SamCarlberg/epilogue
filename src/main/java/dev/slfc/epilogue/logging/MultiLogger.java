package dev.slfc.epilogue.logging;

import edu.wpi.first.util.struct.Struct;
import edu.wpi.first.util.struct.StructSerializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A data logger implementation that delegates to other loggers. Helpful for simultaneous logging
 * to multiple data stores at once.
 */
public class MultiLogger implements DataLogger {
  private final List<DataLogger> loggers;
  private final Map<String, SubLogger> subLoggers = new HashMap<>();

  public MultiLogger(DataLogger... loggers) {
    this.loggers = List.of(loggers);
  }

  public MultiLogger(Collection<? extends DataLogger> loggers) {
    this.loggers = List.copyOf(loggers);
  }

  @Override
  public DataLogger getSubLogger(String path) {
    return subLoggers.computeIfAbsent(path, k -> new SubLogger(k, this));
  }

  @Override
  public void log(String identifier, int value) {
    for (DataLogger logger : loggers) {
      logger.log(identifier, value);
    }
  }

  @Override
  public void log(String identifier, long value) {
    for (DataLogger logger : loggers) {
      logger.log(identifier, value);
    }
  }

  @Override
  public void log(String identifier, float value) {
    for (DataLogger logger : loggers) {
      logger.log(identifier, value);
    }
  }

  @Override
  public void log(String identifier, double value) {
    for (DataLogger logger : loggers) {
      logger.log(identifier, value);
    }
  }

  @Override
  public void log(String identifier, boolean value) {
    for (DataLogger logger : loggers) {
      logger.log(identifier, value);
    }
  }

  @Override
  public void log(String identifier, byte[] value) {
    for (DataLogger logger : loggers) {
      logger.log(identifier, value);
    }
  }

  @Override
  public void log(String identifier, int[] value) {
    for (DataLogger logger : loggers) {
      logger.log(identifier, value);
    }
  }

  @Override
  public void log(String identifier, long[] value) {
    for (DataLogger logger : loggers) {
      logger.log(identifier, value);
    }
  }

  @Override
  public void log(String identifier, float[] value) {
    for (DataLogger logger : loggers) {
      logger.log(identifier, value);
    }
  }

  @Override
  public void log(String identifier, double[] value) {
    for (DataLogger logger : loggers) {
      logger.log(identifier, value);
    }
  }

  @Override
  public void log(String identifier, boolean[] value) {
    for (DataLogger logger : loggers) {
      logger.log(identifier, value);
    }
  }

  @Override
  public void log(String identifier, String value) {
    for (DataLogger logger : loggers) {
      logger.log(identifier, value);
    }
  }

  @Override
  public void log(String identifier, String[] value) {
    for (DataLogger logger : loggers) {
      logger.log(identifier, value);
    }
  }

  @Override
  public <S> void log(String identifier, S value, Struct<S> struct) {
    for (DataLogger logger : loggers) {
      logger.log(identifier, value, struct);
    }
  }

  @Override
  public <S> void log(String identifier, S[] value, Struct<S> struct) {
    for (DataLogger logger : loggers) {
      logger.log(identifier, value, struct);
    }
  }
}
