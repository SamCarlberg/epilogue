package dev.slfc.epilogue.logging;

import edu.wpi.first.util.struct.Struct;
import edu.wpi.first.util.struct.StructBuffer;
import java.util.ArrayList;
import java.util.List;

public class TestLogger implements DataLogger {
  public record LogEntry<T>(String identifier, T value) {
  }

  private final List<LogEntry<?>> entries = new ArrayList<>();

  public List<LogEntry<?>> getEntries() {
    return entries;
  }

  @Override
  public void log(String identifier, int value) {
    entries.add(new LogEntry<>(identifier, value));
  }

  @Override
  public void log(String identifier, long value) {
    entries.add(new LogEntry<>(identifier, value));
  }

  @Override
  public void log(String identifier, float value) {
    entries.add(new LogEntry<>(identifier, value));
  }

  @Override
  public void log(String identifier, double value) {
    entries.add(new LogEntry<>(identifier, value));
  }

  @Override
  public void log(String identifier, boolean value) {
    entries.add(new LogEntry<>(identifier, value));
  }

  @Override
  public void log(String identifier, byte[] value) {
    entries.add(new LogEntry<>(identifier, value));
  }

  @Override
  public void log(String identifier, int[] value) {
    entries.add(new LogEntry<>(identifier, value));
  }

  @Override
  public void log(String identifier, long[] value) {
    entries.add(new LogEntry<>(identifier, value));

  }

  @Override
  public void log(String identifier, float[] value) {
    entries.add(new LogEntry<>(identifier, value));
  }

  @Override
  public void log(String identifier, double[] value) {
    entries.add(new LogEntry<>(identifier, value));
  }

  @Override
  public void log(String identifier, boolean[] value) {
    entries.add(new LogEntry<>(identifier, value));
  }

  @Override
  public void log(String identifier, String value) {
    entries.add(new LogEntry<>(identifier, value));
  }

  @Override
  public void log(String identifier, String[] value) {
    entries.add(new LogEntry<>(identifier, value));
  }

  @Override
  public <S> void log(String identifier, S value, Struct<S> struct) {
    var serialized = StructBuffer.create(struct).write(value).array();

    entries.add(new LogEntry<>(identifier, serialized));
  }

  @Override
  public <S> void log(String identifier, S[] value, Struct<S> struct) {
    var serialized = StructBuffer.create(struct).writeArray(value).array();

    entries.add(new LogEntry<>(identifier, serialized));
  }
}
