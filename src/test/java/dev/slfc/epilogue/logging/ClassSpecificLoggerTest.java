package dev.slfc.epilogue.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.slfc.epilogue.Epilogue;
import java.lang.invoke.VarHandle;
import java.util.List;
import org.junit.jupiter.api.Test;

class ClassSpecificLoggerTest {
  @Epilogue
  record Point2d(double x, double y, int dim) {
  }

  static class Point2dLogger extends ClassSpecificLogger<Point2d> {
    private final VarHandle $x = fieldHandle("x", double.class);
    private final VarHandle $y = fieldHandle("y", double.class);
    private final VarHandle $dim = fieldHandle("dim", int.class);

    public Point2dLogger() {
      super(Point2d.class);
    }

    @Override
    protected void update(DataLogger dataLogger, String identifier, Point2d object) {
      try {
        dataLogger.log(identifier + "/x", (double) $x.get(object));
        dataLogger.log(identifier + "/y", (double) $y.get(object));
        dataLogger.log(identifier + "/dim", (int) $dim.get(object));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Test
  void testReadPrivate() {
    var point = new Point2d(1, 4, 2);
    var logger = new Point2dLogger();
    var dataLog = new TestLogger();
    logger.update(dataLog, "Point", point);

    assertEquals(
        List.of(
            new TestLogger.LogEntry<>("Point/x", 1.0),
            new TestLogger.LogEntry<>("Point/y", 4.0),
            new TestLogger.LogEntry<>("Point/dim", 2)
        ), dataLog.getEntries());
  }
}