package dev.slfc.epilogue;

import dev.slfc.epilogue.logging.DataLogger;
import dev.slfc.epilogue.logging.FileLogger;
import dev.slfc.epilogue.logging.NullLogger;

public class EpilogueConfiguration {
  /**
   * The data logger implementation for Epilogue to use. By default, this is a no-op logger that
   * does nothing. Set this to a different implementation like {@link FileLogger}
   */
  public DataLogger dataLogger = new NullLogger();

  /**
   * Flag to tell Epilogue how to handle errors. If set to false (the default), then errors and
   * warnings will be printed to the console output. This setting is useful when running on a field
   * or in another environment where code crashes due to bad data going to the logs are undesirable.
   * Set this to true to have fatal errors throw exceptions.
   */
  public boolean crashOnError = false;

  /**
   * The minimum importance level of data to be logged. Defaults to debug, which logs data of all
   * importance levels. Any data tagged with a importance level lower than this will not be logged.
   *
   * <p>Note: setting this value to {@link Epilogue.Importance#NONE} will
   * behave exactly as if it were set to debug, because elements tagged with an importance of
   * {@code NONE} are excluded from the generated loggers.</p>
   */
  public Epilogue.Importance minimumImportance = Epilogue.Importance.DEBUG;
}
