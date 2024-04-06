package dev.slfc.epilogue.logging;

import edu.wpi.first.networktables.BooleanArrayPublisher;
import edu.wpi.first.networktables.BooleanPublisher;
import edu.wpi.first.networktables.DoubleArrayPublisher;
import edu.wpi.first.networktables.DoublePublisher;
import edu.wpi.first.networktables.FloatArrayPublisher;
import edu.wpi.first.networktables.FloatPublisher;
import edu.wpi.first.networktables.IntegerArrayPublisher;
import edu.wpi.first.networktables.IntegerPublisher;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.Publisher;
import edu.wpi.first.networktables.RawPublisher;
import edu.wpi.first.networktables.StringArrayPublisher;
import edu.wpi.first.networktables.StringPublisher;
import edu.wpi.first.networktables.StructArrayPublisher;
import edu.wpi.first.networktables.StructPublisher;
import edu.wpi.first.util.struct.Struct;
import edu.wpi.first.util.struct.StructBuffer;
import edu.wpi.first.util.struct.StructSerializable;
import java.util.HashMap;
import java.util.Map;

/**
 * A data logger implementation that sends data over network tables. Be careful when using this,
 * since sending too much data may cause bandwidth or CPU starvation.
 */
public class NTDataLogger implements DataLogger {
  private final NetworkTableInstance nt;

  private final Map<String, Publisher> publishers = new HashMap<>();

  public NTDataLogger(NetworkTableInstance nt) {
    this.nt = nt;
  }

  @Override
  public void log(String identifier, int value) {
    ((IntegerPublisher) publishers.computeIfAbsent(identifier, k -> nt.getIntegerTopic(k).publish()))
        .set(value);
  }

  @Override
  public void log(String identifier, long value) {
    ((IntegerPublisher) publishers.computeIfAbsent(identifier, k -> nt.getIntegerTopic(k).publish()))
        .set(value);
  }

  @Override
  public void log(String identifier, float value) {
    ((FloatPublisher) publishers.computeIfAbsent(identifier, k -> nt.getFloatTopic(k).publish()))
        .set(value);
  }

  @Override
  public void log(String identifier, double value) {
    ((DoublePublisher) publishers.computeIfAbsent(identifier, k -> nt.getDoubleTopic(k).publish()))
        .set(value);
  }

  @Override
  public void log(String identifier, boolean value) {
    ((BooleanPublisher) publishers.computeIfAbsent(identifier, k -> nt.getBooleanTopic(k).publish()))
        .set(value);
  }

  @Override
  public void log(String identifier, byte[] value) {
    ((RawPublisher) publishers.computeIfAbsent(identifier, k -> nt.getRawTopic(k).publish("raw")))
        .set(value);
  }

  @Override
  public void log(String identifier, int[] value) {
    // NT backend only supports int64[], so we have to manually widen to 64 bits before sending
    long[] widened = new long[value.length];

    for (int i = 0; i < value.length; i++) {
      widened[i] = (long) value[i];
    }

    ((IntegerArrayPublisher) publishers.computeIfAbsent(identifier, k -> nt.getIntegerArrayTopic(k).publish()))
        .set(widened);
  }

  @Override
  public void log(String identifier, long[] value) {
    ((IntegerArrayPublisher) publishers.computeIfAbsent(identifier, k -> nt.getIntegerArrayTopic(k).publish()))
        .set(value);
  }

  @Override
  public void log(String identifier, float[] value) {
    ((FloatArrayPublisher) publishers.computeIfAbsent(identifier, k -> nt.getFloatArrayTopic(k).publish()))
        .set(value);
  }

  @Override
  public void log(String identifier, double[] value) {
    ((DoubleArrayPublisher) publishers.computeIfAbsent(identifier, k -> nt.getDoubleArrayTopic(k).publish()))
        .set(value);
  }

  @Override
  public void log(String identifier, boolean[] value) {
    ((BooleanArrayPublisher) publishers.computeIfAbsent(identifier, k -> nt.getBooleanArrayTopic(k).publish()))
        .set(value);
  }

  @Override
  public void log(String identifier, String value) {
    ((StringPublisher) publishers.computeIfAbsent(identifier, k -> nt.getStringTopic(k).publish()))
        .set(value);
  }

  @Override
  public void log(String identifier, String[] value) {
    ((StringArrayPublisher) publishers.computeIfAbsent(identifier, k -> nt.getStringArrayTopic(k).publish()))
        .set(value);
  }

  @Override
  public <S extends StructSerializable> void log(String identifier, S value, Struct<S> struct) {
    nt.addSchema(struct);
    ((StructPublisher<S>) publishers.computeIfAbsent(identifier, k -> nt.getStructTopic(k, struct).publish()))
        .set(value);
  }

  @Override
  public <S extends StructSerializable> void log(String identifier, S[] value, Struct<S> struct) {
    nt.addSchema(struct);
    ((StructArrayPublisher<S>) publishers.computeIfAbsent(identifier, k -> nt.getStructArrayTopic(k, struct).publish()))
        .set(value);
  }
}
