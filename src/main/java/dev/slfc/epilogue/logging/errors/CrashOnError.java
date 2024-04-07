package dev.slfc.epilogue.logging.errors;

import dev.slfc.epilogue.logging.ClassSpecificLogger;

public class CrashOnError implements ErrorHandler {
  @Override
  public void handle(Throwable exception, ClassSpecificLogger<?> logger) {
    throw new RuntimeException(
        "[EPILOGUE] An error occurred while logging an instance of "
            + logger.getLoggedType().getName(),
        exception
    );
  }
}
