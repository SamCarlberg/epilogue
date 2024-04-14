package dev.slfc.epilogue.logging.errors;

import dev.slfc.epilogue.logging.ClassSpecificLogger;

@FunctionalInterface
public interface ErrorHandler {
  void handle(Throwable exception, ClassSpecificLogger<?> logger);

  static ErrorHandler crashOnError() {
    return new CrashOnError();
  }

  static ErrorHandler printErrorMessages() {
    return new ErrorPrinter();
  }

  static ErrorHandler disabling(int maximumPermissibleErrors) {
    return new LoggerDisabler(maximumPermissibleErrors);
  }
}
