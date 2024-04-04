package dev.slfc.epilogue.logging;

import static edu.wpi.first.util.ErrorMessages.requireNonNullParam;

import edu.wpi.first.util.datalog.BooleanArrayLogEntry;
import edu.wpi.first.util.datalog.BooleanLogEntry;
import edu.wpi.first.util.datalog.DataLog;
import edu.wpi.first.util.datalog.DataLogEntry;
import edu.wpi.first.util.datalog.DoubleArrayLogEntry;
import edu.wpi.first.util.datalog.DoubleLogEntry;
import edu.wpi.first.util.datalog.FloatArrayLogEntry;
import edu.wpi.first.util.datalog.FloatLogEntry;
import edu.wpi.first.util.datalog.IntegerArrayLogEntry;
import edu.wpi.first.util.datalog.IntegerLogEntry;
import edu.wpi.first.util.datalog.RawLogEntry;
import edu.wpi.first.util.datalog.StringArrayLogEntry;
import edu.wpi.first.util.datalog.StringLogEntry;
import edu.wpi.first.util.struct.Struct;
import edu.wpi.first.util.struct.StructBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * A data logger implementation that saves information to a WPILib {@link DataLog} file on disk.
 */
public class FileLogger implements DataLogger {
  private final DataLog dataLog;
  private final Map<String, DataLogEntry> entries = new HashMap<>();

  // Cache struct buffers to avoid runtime allocations when possible
  @SuppressWarnings("rawtypes")
  private final Map<Struct, StructBuffer> buffers = new HashMap<>();

  /**
   * Creates a new file logger.
   *
   * @param dataLog the data log to save data to
   */
  public FileLogger(DataLog dataLog) {
    this.dataLog = requireNonNullParam(dataLog, "dataLog", "FileLogger");
  }

  @SuppressWarnings("unchecked")
  private <E extends DataLogEntry> E getEntry(
      String identifier,
      BiFunction<DataLog, String, ? extends E> ctor) {
    requireNonNullParam(identifier, "identifier", "log");

    if (entries.containsKey(identifier)) {
      return (E) entries.get(identifier);
    } else {
      var entry = ctor.apply(dataLog, identifier);
      entries.put(identifier, entry);
      return entry;
    }
  }

  @Override
  public void log(String identifier, int value) {
    getEntry(identifier, IntegerLogEntry::new).append(value);
  }

  @Override
  public void log(String identifier, long value) {
    getEntry(identifier, IntegerLogEntry::new).append(value);
  }

  @Override
  public void log(String identifier, float value) {
    getEntry(identifier, FloatLogEntry::new).append(value);
  }

  @Override
  public void log(String identifier, double value) {
    getEntry(identifier, DoubleLogEntry::new).append(value);
  }

  @Override
  public void log(String identifier, boolean value) {
    getEntry(identifier, BooleanLogEntry::new).append(value);
  }

  @Override
  public void log(String identifier, byte[] value) {
    getEntry(identifier, RawLogEntry::new).append(value);
  }

  @Override
  public void log(String identifier, int[] value) {
    long[] widened = new long[value.length];
    for (int i = 0; i < value.length; i++) {
      widened[i] = value[i];
    }
    getEntry(identifier, IntegerArrayLogEntry::new).append(widened);
  }

  @Override
  public void log(String identifier, long[] value) {
    getEntry(identifier, IntegerArrayLogEntry::new).append(value);
  }

  @Override
  public void log(String identifier, float[] value) {
    getEntry(identifier, FloatArrayLogEntry::new).append(value);
  }

  @Override
  public void log(String identifier, double[] value) {
    getEntry(identifier, DoubleArrayLogEntry::new).append(value);
  }

  @Override
  public void log(String identifier, boolean[] value) {
    getEntry(identifier, BooleanArrayLogEntry::new).append(value);
  }

  @Override
  public void log(String identifier, String value) {
    getEntry(identifier, StringLogEntry::new).append(value);
  }

  @Override
  public void log(String identifier, String[] value) {
    getEntry(identifier, StringArrayLogEntry::new).append(value);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <S> void log(String identifier, S value, Struct<S> struct) {
    dataLog.addSchema(struct);
    StructBuffer<S> buffer = buffers.computeIfAbsent(struct, StructBuffer::create);

    var serializedData = buffer.write(value);

    getEntry(identifier, RawLogEntry::new).append(serializedData);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <S> void log(String identifier, S[] value, Struct<S> struct) {
    dataLog.addSchema(struct);
    StructBuffer<S> buffer = buffers.computeIfAbsent(struct, StructBuffer::create);

    var serializedData = buffer.writeArray(value);

    getEntry(identifier, RawLogEntry::new).append(serializedData);
  }
}
