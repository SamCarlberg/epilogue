package dev.slfc.epilogue.logging;

import edu.wpi.first.util.function.BooleanConsumer;
import edu.wpi.first.util.function.FloatConsumer;
import edu.wpi.first.util.function.FloatSupplier;
import edu.wpi.first.util.sendable.SendableBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * A sendable builder implementation that sends data to a {@link DataLogger}.
 */
public class LogBackedSendableBuilder implements SendableBuilder {
  private final DataLogger logger;
  private final String prefix;
  private final Collection<Runnable> updates = new ArrayList<>();

  public LogBackedSendableBuilder(DataLogger logger, String prefix) {
    this.logger = logger;
    this.prefix = prefix;
  }

  @Override
  public void setSmartDashboardType(String type) {
    logger.log(prefix + "/.type", type);
  }

  @Override
  public void setActuator(boolean value) {
    // ignore
  }

  @Override
  public void setSafeState(Runnable func) {
    // ignore
  }

  @Override
  public void addBooleanProperty(String key, BooleanSupplier getter, BooleanConsumer setter) {
    updates.add(() -> logger.log(prefix + "/" + key, getter.getAsBoolean()));
  }

  @Override
  public void publishConstBoolean(String key, boolean value) {
    logger.log(prefix + "/" + key, value);
  }

  @Override
  public void addIntegerProperty(String key, LongSupplier getter, LongConsumer setter) {
    updates.add(() -> logger.log(prefix + "/" + key, getter.getAsLong()));
  }

  @Override
  public void publishConstInteger(String key, long value) {
    logger.log(prefix + "/" + key, value);
  }

  @Override
  public void addFloatProperty(String key, FloatSupplier getter, FloatConsumer setter) {
    updates.add(() -> logger.log(prefix + "/" + key, getter.getAsFloat()));
  }

  @Override
  public void publishConstFloat(String key, float value) {
    logger.log(prefix + "/" + key, value);
  }

  @Override
  public void addDoubleProperty(String key, DoubleSupplier getter, DoubleConsumer setter) {
    updates.add(() -> logger.log(prefix + "/" + key, getter.getAsDouble()));
  }

  @Override
  public void publishConstDouble(String key, double value) {
    logger.log(prefix + "/" + key, value);
  }

  @Override
  public void addStringProperty(String key, Supplier<String> getter, Consumer<String> setter) {
    if (getter != null) updates.add(() -> logger.log(prefix + "/" + key, getter.get()));
  }

  @Override
  public void publishConstString(String key, String value) {
    logger.log(prefix + "/" + key, value);
  }

  @Override
  public void addBooleanArrayProperty(String key, Supplier<boolean[]> getter, Consumer<boolean[]> setter) {
    if (getter != null) updates.add(() -> logger.log(prefix + "/" + key, getter.get()));
  }

  @Override
  public void publishConstBooleanArray(String key, boolean[] value) {
    logger.log(prefix + "/" + key, value);
  }

  @Override
  public void addIntegerArrayProperty(String key, Supplier<long[]> getter, Consumer<long[]> setter) {
    if (getter != null) updates.add(() -> logger.log(prefix + "/" + key, getter.get()));
  }

  @Override
  public void publishConstIntegerArray(String key, long[] value) {
    logger.log(prefix + "/" + key, value);
  }

  @Override
  public void addFloatArrayProperty(String key, Supplier<float[]> getter, Consumer<float[]> setter) {
    if (getter != null) updates.add(() -> logger.log(prefix + "/" + key, getter.get()));
  }

  @Override
  public void publishConstFloatArray(String key, float[] value) {
    logger.log(prefix + "/" + key, value);
  }

  @Override
  public void addDoubleArrayProperty(String key, Supplier<double[]> getter, Consumer<double[]> setter) {
    if (getter != null) updates.add(() -> logger.log(prefix + "/" + key, getter.get()));
  }

  @Override
  public void publishConstDoubleArray(String key, double[] value) {
    logger.log(prefix + "/" + key, value);
  }

  @Override
  public void addStringArrayProperty(String key, Supplier<String[]> getter, Consumer<String[]> setter) {
    if (getter != null) updates.add(() -> logger.log(prefix + "/" + key, getter.get()));
  }

  @Override
  public void publishConstStringArray(String key, String[] value) {
    logger.log(prefix + "/" + key, value);
  }

  @Override
  public void addRawProperty(String key, String typeString, Supplier<byte[]> getter, Consumer<byte[]> setter) {
    if (getter != null) updates.add(() -> logger.log(prefix + "/" + key, getter.get()));
  }

  @Override
  public void publishConstRaw(String key, String typeString, byte[] value) {
    logger.log(prefix + "/" + key, value);
  }

  @Override
  public BackendKind getBackendKind() {
    return BackendKind.kUnknown;
  }

  @Override
  public boolean isPublished() {
    return true;
  }

  @Override
  public void update() {
    for (Runnable update : updates) {
      update.run();
    }
  }

  @Override
  public void clearProperties() {
    updates.clear();
  }

  @Override
  public void addCloseable(AutoCloseable closeable) {
    // Ignore
  }

  @Override
  public void close() throws Exception {
    clearProperties();
  }
}
