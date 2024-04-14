package dev.slfc.epilogue.logging.errors;

import dev.slfc.epilogue.logging.ClassSpecificLogger;
import java.util.HashMap;
import java.util.Map;

/**
 * An error handler that disables loggers after too many exceptions are raised. Useful when playing
 * in matches, where data logging is less important than reliable control. Setting the threshold
 * to ≤0 will cause any logger that encounters an exception whilst logging to immediately be
 * disabled. Setting to higher values means your program is more tolerant of errors, but takes
 * longer to disable, and therefore may have more sets of partial or incomplete data and may have
 * more CPU overhead due to the cost of throwing exceptions.
 */
public class LoggerDisabler implements ErrorHandler {
  private final int threshold;
  private final Map<ClassSpecificLogger<?>, Integer> errorCounts = new HashMap<>();

  public LoggerDisabler(int threshold) {
    this.threshold = threshold;
  }

  /**
   * Creates a disabler that kicks in after a logger raises more than {@code threshold} exceptions.
   * @param threshold the threshold value for the maximum number of exceptions loggers are permitted
   *                  to encounter before they are disabled
   */
  public static LoggerDisabler forLimit(int threshold) {
    return new LoggerDisabler(threshold);
  }

  @Override
  public void handle(Throwable exception, ClassSpecificLogger<?> logger) {
    var errorCount = errorCounts.getOrDefault(logger, 0) + 1;
    errorCounts.put(logger, errorCount);

    if (errorCount > threshold) {
      logger.disable();
      System.err.println(
          "[EPILOGUE] Too many errors detected in "
              + logger.getClass().getName()
              + " (maximum allowed: " + threshold + "). The most recent error follows:");
      System.err.println(exception.getMessage());
      exception.printStackTrace(System.err);
    }
  }

  /**
   * Resets all error counts and reenables all loggers.
   */
  public void reset() {
    for (var logger : errorCounts.keySet()) {
      // Safe. This is a no-op on loggers that are already enabled
      logger.reenable();
    }
    errorCounts.clear();
  }
}
