package dev.slfc.epilogue.logging;

import edu.wpi.first.util.struct.Struct;
import edu.wpi.first.util.struct.StructSerializable;
import java.util.HashMap;
import java.util.Map;

public class SubLogger implements DataLogger {
  private final String prefix;
  private final DataLogger impl;
  private final Map<String, SubLogger> subLoggers = new HashMap<>();

  public SubLogger(String prefix, DataLogger impl) {
    this.prefix = prefix + "/";
    this.impl = impl;
  }

  @Override
  public DataLogger getSubLogger(String path) {
    return subLoggers.computeIfAbsent(path, k -> new SubLogger(k, this));
  }

  @Override
  public void log(String identifier, int value) {
    impl.log(prefix + identifier, value);
  }

  @Override
  public void log(String identifier, long value) {
    impl.log(prefix + identifier, value);
  }

  @Override
  public void log(String identifier, float value) {
    impl.log(prefix + identifier, value);
  }

  @Override
  public void log(String identifier, double value) {
    impl.log(prefix + identifier, value);
  }

  @Override
  public void log(String identifier, boolean value) {
    impl.log(prefix + identifier, value);
  }

  @Override
  public void log(String identifier, byte[] value) {
    impl.log(prefix + identifier, value);
  }

  @Override
  public void log(String identifier, int[] value) {
    impl.log(prefix + identifier, value);
  }

  @Override
  public void log(String identifier, long[] value) {
    impl.log(prefix + identifier, value);
  }

  @Override
  public void log(String identifier, float[] value) {
    impl.log(prefix + identifier, value);
  }

  @Override
  public void log(String identifier, double[] value) {
    impl.log(prefix + identifier, value);
  }

  @Override
  public void log(String identifier, boolean[] value) {
    impl.log(prefix + identifier, value);
  }

  @Override
  public void log(String identifier, String value) {
    impl.log(prefix + identifier, value);
  }

  @Override
  public void log(String identifier, String[] value) {
    impl.log(prefix + identifier, value);
  }

  @Override
  public <S extends StructSerializable> void log(String identifier, S value, Struct<S> struct) {
    impl.log(prefix + identifier, value, struct);
  }

  @Override
  public <S extends StructSerializable> void log(String identifier, S[] value, Struct<S> struct) {
    impl.log(prefix + identifier, value, struct);
  }
}
