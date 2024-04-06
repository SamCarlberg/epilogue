package dev.slfc.epilogue.logging;

import edu.wpi.first.util.struct.Struct;
import edu.wpi.first.util.struct.StructSerializable;

/**
 * Null data logger implementation that logs nothing.
 */
public class NullLogger implements DataLogger {
  @Override
  public void log(String identifier, int value) {

  }

  @Override
  public void log(String identifier, long value) {

  }

  @Override
  public void log(String identifier, float value) {

  }

  @Override
  public void log(String identifier, double value) {

  }

  @Override
  public void log(String identifier, boolean value) {

  }

  @Override
  public void log(String identifier, byte[] value) {

  }

  @Override
  public void log(String identifier, int[] value) {

  }

  @Override
  public void log(String identifier, long[] value) {

  }

  @Override
  public void log(String identifier, float[] value) {

  }

  @Override
  public void log(String identifier, double[] value) {

  }

  @Override
  public void log(String identifier, boolean[] value) {

  }

  @Override
  public void log(String identifier, String value) {

  }

  @Override
  public void log(String identifier, String[] value) {

  }

  @Override
  public <S extends StructSerializable> void log(String identifier, S value, Struct<S> struct) {

  }

  @Override
  public <S extends StructSerializable> void log(String identifier, S[] value, Struct<S> struct) {

  }
}
