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
}
