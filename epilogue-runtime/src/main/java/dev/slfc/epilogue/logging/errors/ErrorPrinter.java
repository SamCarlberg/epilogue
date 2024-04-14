package dev.slfc.epilogue.logging.errors;

import dev.slfc.epilogue.logging.ClassSpecificLogger;

public class ErrorPrinter implements ErrorHandler {
  @Override
  public void handle(Throwable exception, ClassSpecificLogger<?> logger) {
    System.err.println(
        "[EPILOGUE] An error occurred while logging an instance of "
            + logger.getLoggedType().getName()
            + ": "
            + exception.getMessage()
    );
  }
}
