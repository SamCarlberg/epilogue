package dev.slfc.epilogue.logging;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class LazyLoggerTest {
  @Test
  void lazyOfLazyReturnsSelf() {
    var lazy = new LazyLogger(new NullLogger());
    assertSame(lazy, lazy.lazy());
  }

  @Test
  void lazyInt() {
    var logger = new TestLogger();
    var lazy = new LazyLogger(logger);

    {
      // First time logging to "int" should go through
      lazy.log("int", 0);
      assertEquals(List.of(new TestLogger.LogEntry<>("int", 0)), logger.getEntries());
    }

    {
      // Logging the current value shouldn't go through
      lazy.log("int", 0);
      assertEquals(List.of(new TestLogger.LogEntry<>("int", 0)), logger.getEntries());
    }

    {
      // Logging a new value should go through
      lazy.log("int", 1);
      assertEquals(
          List.of(
              new TestLogger.LogEntry<>("int", 0),
              new TestLogger.LogEntry<>("int", 1)
          ),
          logger.getEntries()
      );
    }

    {
      // Logging a previous value should go through
      lazy.log("int", 0);
      assertEquals(
          List.of(
              new TestLogger.LogEntry<>("int", 0),
              new TestLogger.LogEntry<>("int", 1),
              new TestLogger.LogEntry<>("int", 0)
          ),
          logger.getEntries()
      );
    }
  }
}