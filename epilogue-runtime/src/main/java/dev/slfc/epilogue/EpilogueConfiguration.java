package dev.slfc.epilogue;

import dev.slfc.epilogue.logging.DataLogger;
import dev.slfc.epilogue.logging.FileLogger;
import dev.slfc.epilogue.logging.NullLogger;
import dev.slfc.epilogue.logging.errors.ErrorHandler;
import dev.slfc.epilogue.logging.errors.ErrorPrinter;

public class EpilogueConfiguration {
  /**
   * The data logger implementation for Epilogue to use. By default, this is a no-op logger that
   * does nothing. Set this to a different implementation like {@link FileLogger}
   */
  public DataLogger dataLogger = new NullLogger();

  /**
   * The minimum importance level of data to be logged. Defaults to debug, which logs data of all
   * importance levels. Any data tagged with a importance level lower than this will not be logged.
   */
  public Epilogue.Importance minimumImportance = Epilogue.Importance.DEBUG;

  /**
   * The error handler for loggers to use if they encounter an error while logging. Defaults to
   * printing an error to the standard output.
   */
  public ErrorHandler errorHandler = new ErrorPrinter();

  /**
   * The root identifier to use for all logged data. Defaults to "Robot", but can be changed to
   * any string.
   */
  public String root = "Robot";
}
