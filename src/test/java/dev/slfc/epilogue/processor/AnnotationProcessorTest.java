package dev.slfc.epilogue.processor;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class AnnotationProcessorTest {
  @Test
  void simple() {
    String source = """
      package dev.slfc.epilogue;
      
      @Epilogue
      class HelloWorld {
        double x;
      }
    """;

    String expectedGeneratedSource = """
      package dev.slfc.epilogue;

      import dev.slfc.epilogue.Epilogue;
      import dev.slfc.epilogue.Epiloguer;
      import dev.slfc.epilogue.logging.DataLogger;
      import dev.slfc.epilogue.logging.ClassSpecificLogger;
      import java.lang.invoke.VarHandle;

      public class HelloWorldLogger extends ClassSpecificLogger<HelloWorld> {

        public HelloWorldLogger() {
          super(HelloWorld.class);
        }

        @Override
        public void update(DataLogger dataLogger, String identifier, HelloWorld object) {
          try {
            if (Epiloguer.shouldLog(Epilogue.Importance.DEBUG)) {
              dataLogger.log(identifier + "/x", object.x);
            }
          } catch (Exception e) {
            System.err.println("[EPILOGUE] Encountered an error while logging: " + e.getMessage());
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void multiple() {
    String source = """
      package dev.slfc.epilogue;

      @Epilogue
      class HelloWorld {
        double x;
        double y;
      }
    """;

    String expectedGeneratedSource = """
      package dev.slfc.epilogue;

      import dev.slfc.epilogue.Epilogue;
      import dev.slfc.epilogue.Epiloguer;
      import dev.slfc.epilogue.logging.DataLogger;
      import dev.slfc.epilogue.logging.ClassSpecificLogger;
      import java.lang.invoke.VarHandle;

      public class HelloWorldLogger extends ClassSpecificLogger<HelloWorld> {

        public HelloWorldLogger() {
          super(HelloWorld.class);
        }

        @Override
        public void update(DataLogger dataLogger, String identifier, HelloWorld object) {
          try {
            if (Epiloguer.shouldLog(Epilogue.Importance.DEBUG)) {
              dataLogger.log(identifier + "/x", object.x);
              dataLogger.log(identifier + "/y", object.y);
            }
          } catch (Exception e) {
            System.err.println("[EPILOGUE] Encountered an error while logging: " + e.getMessage());
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void privateFields() {
    String source = """
      package dev.slfc.epilogue;
      
      @Epilogue
      class HelloWorld {
        private double x;
      }
    """;

    String expectedGeneratedSource = """
      package dev.slfc.epilogue;

      import dev.slfc.epilogue.Epilogue;
      import dev.slfc.epilogue.Epiloguer;
      import dev.slfc.epilogue.logging.DataLogger;
      import dev.slfc.epilogue.logging.ClassSpecificLogger;
      import java.lang.invoke.VarHandle;
          
      public class HelloWorldLogger extends ClassSpecificLogger<HelloWorld> {
        private final VarHandle $x;
          
        public HelloWorldLogger() {
          super(HelloWorld.class);
          $x = fieldHandle("x", double.class);
        }
          
        @Override
        public void update(DataLogger dataLogger, String identifier, HelloWorld object) {
          try {
            if (Epiloguer.shouldLog(Epilogue.Importance.DEBUG)) {
              dataLogger.log(identifier + "/x", (double) $x.get(object));
            }
          } catch (Exception e) {
            System.err.println("[EPILOGUE] Encountered an error while logging: " + e.getMessage());
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void privateWithGenerics() {
    String source = """
      package dev.slfc.epilogue;

      @Epilogue
      class HelloWorld {
        private edu.wpi.first.wpilibj.smartdashboard.SendableChooser<String> chooser;
      }
      """;

    String expectedGeneratedSource = """
      package dev.slfc.epilogue;

      import dev.slfc.epilogue.Epilogue;
      import dev.slfc.epilogue.Epiloguer;
      import dev.slfc.epilogue.logging.DataLogger;
      import dev.slfc.epilogue.logging.ClassSpecificLogger;
      import java.lang.invoke.VarHandle;

      public class HelloWorldLogger extends ClassSpecificLogger<HelloWorld> {
        private final VarHandle $chooser;

        public HelloWorldLogger() {
          super(HelloWorld.class);
          $chooser = fieldHandle("chooser", edu.wpi.first.wpilibj.smartdashboard.SendableChooser.class);
        }

        @Override
        public void update(DataLogger dataLogger, String identifier, HelloWorld object) {
          try {
            if (Epiloguer.shouldLog(Epilogue.Importance.DEBUG)) {
              logSendable(dataLogger, identifier + "/chooser", (edu.wpi.first.wpilibj.smartdashboard.SendableChooser<java.lang.String>) $chooser.get(object));
            }
          } catch (Exception e) {
            System.err.println("[EPILOGUE] Encountered an error while logging: " + e.getMessage());
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void importanceLevels() {
    String source = """
      package dev.slfc.epilogue;

      @Epilogue(importance = Epilogue.Importance.INFO)
      class HelloWorld {
        @Epilogue(importance = Epilogue.Importance.DEBUG)    double low;
        @Epilogue(importance = Epilogue.Importance.INFO)     int    medium;
        @Epilogue(importance = Epilogue.Importance.CRITICAL) long   high;
      }
      """;


    String expectedGeneratedSource = """
      package dev.slfc.epilogue;

      import dev.slfc.epilogue.Epilogue;
      import dev.slfc.epilogue.Epiloguer;
      import dev.slfc.epilogue.logging.DataLogger;
      import dev.slfc.epilogue.logging.ClassSpecificLogger;
      import java.lang.invoke.VarHandle;
      
      public class HelloWorldLogger extends ClassSpecificLogger<HelloWorld> {

        public HelloWorldLogger() {
          super(HelloWorld.class);
        }
        
        @Override
        public void update(DataLogger dataLogger, String identifier, HelloWorld object) {
          try {
            if (Epiloguer.shouldLog(Epilogue.Importance.DEBUG)) {
              dataLogger.log(identifier + "/low", object.low);
            }
            if (Epiloguer.shouldLog(Epilogue.Importance.INFO)) {
              dataLogger.log(identifier + "/medium", object.medium);
            }
            if (Epiloguer.shouldLog(Epilogue.Importance.CRITICAL)) {
              dataLogger.log(identifier + "/high", object.high);
            }
          } catch (Exception e) {
            System.err.println("[EPILOGUE] Encountered an error while logging: " + e.getMessage());
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void logEnum() {
    String source = """
      package dev.slfc.epilogue;

      @Epilogue
      class HelloWorld {
        enum E {
          a, b, c;
        }
        private E enumValue;   // Should be logged
        private E[] enumArray; // Should not be logged
      }
      """;

    String expectedGeneratedSource = """
      package dev.slfc.epilogue;

      import dev.slfc.epilogue.Epilogue;
      import dev.slfc.epilogue.Epiloguer;
      import dev.slfc.epilogue.logging.DataLogger;
      import dev.slfc.epilogue.logging.ClassSpecificLogger;
      import java.lang.invoke.VarHandle;

      public class HelloWorldLogger extends ClassSpecificLogger<HelloWorld> {
        private final VarHandle $enumValue;

        public HelloWorldLogger() {
          super(HelloWorld.class);
          $enumValue = fieldHandle("enumValue", dev.slfc.epilogue.HelloWorld.E.class);
        }

        @Override
        public void update(DataLogger dataLogger, String identifier, HelloWorld object) {
          try {
            if (Epiloguer.shouldLog(Epilogue.Importance.DEBUG)) {
              dataLogger.log(identifier + "/enumValue", (dev.slfc.epilogue.HelloWorld.E) $enumValue.get(object));
            }
          } catch (Exception e) {
            System.err.println("[EPILOGUE] Encountered an error while logging: " + e.getMessage());
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void bytes() {
    String source = """
      package dev.slfc.epilogue;

      @Epilogue
      class HelloWorld {
        byte x;        // Should be logged
        byte[] arr1;   // Should be logged
        byte[][] arr2; // Should not be logged
      }
      """;

    String expectedGeneratedSource = """
      package dev.slfc.epilogue;

      import dev.slfc.epilogue.Epilogue;
      import dev.slfc.epilogue.Epiloguer;
      import dev.slfc.epilogue.logging.DataLogger;
      import dev.slfc.epilogue.logging.ClassSpecificLogger;
      import java.lang.invoke.VarHandle;

      public class HelloWorldLogger extends ClassSpecificLogger<HelloWorld> {

        public HelloWorldLogger() {
          super(HelloWorld.class);
        }

        @Override
        public void update(DataLogger dataLogger, String identifier, HelloWorld object) {
          try {
            if (Epiloguer.shouldLog(Epilogue.Importance.DEBUG)) {
              dataLogger.log(identifier + "/x", object.x);
              dataLogger.log(identifier + "/arr1", object.arr1);
            }
          } catch (Exception e) {
            System.err.println("[EPILOGUE] Encountered an error while logging: " + e.getMessage());
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void chars() {
    String source = """
      package dev.slfc.epilogue;

      @Epilogue
      class HelloWorld {
        char x;        // Should be logged
        char[] arr1;   // Should not be logged
        char[][] arr2; // Should not be logged
      }
      """;

    String expectedGeneratedSource = """
      package dev.slfc.epilogue;

      import dev.slfc.epilogue.Epilogue;
      import dev.slfc.epilogue.Epiloguer;
      import dev.slfc.epilogue.logging.DataLogger;
      import dev.slfc.epilogue.logging.ClassSpecificLogger;
      import java.lang.invoke.VarHandle;

      public class HelloWorldLogger extends ClassSpecificLogger<HelloWorld> {

        public HelloWorldLogger() {
          super(HelloWorld.class);
        }

        @Override
        public void update(DataLogger dataLogger, String identifier, HelloWorld object) {
          try {
            if (Epiloguer.shouldLog(Epilogue.Importance.DEBUG)) {
              dataLogger.log(identifier + "/x", object.x);
            }
          } catch (Exception e) {
            System.err.println("[EPILOGUE] Encountered an error while logging: " + e.getMessage());
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void shorts() {
    String source = """
      package dev.slfc.epilogue;

      @Epilogue
      class HelloWorld {
        char x;        // Should be logged
        char[] arr1;   // Should not be logged
        char[][] arr2; // Should not be logged
      }
      """;

    String expectedGeneratedSource = """
      package dev.slfc.epilogue;

      import dev.slfc.epilogue.Epilogue;
      import dev.slfc.epilogue.Epiloguer;
      import dev.slfc.epilogue.logging.DataLogger;
      import dev.slfc.epilogue.logging.ClassSpecificLogger;
      import java.lang.invoke.VarHandle;

      public class HelloWorldLogger extends ClassSpecificLogger<HelloWorld> {

        public HelloWorldLogger() {
          super(HelloWorld.class);
        }

        @Override
        public void update(DataLogger dataLogger, String identifier, HelloWorld object) {
          try {
            if (Epiloguer.shouldLog(Epilogue.Importance.DEBUG)) {
              dataLogger.log(identifier + "/x", object.x);
            }
          } catch (Exception e) {
            System.err.println("[EPILOGUE] Encountered an error while logging: " + e.getMessage());
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void ints() {
    String source = """
      package dev.slfc.epilogue;

      @Epilogue
      class HelloWorld {
        int x;           // Should be logged
        int[] intArr1;   // Should be logged
        int[][] intArr2; // Should not be logged
      }
      """;

    String expectedGeneratedSource = """
      package dev.slfc.epilogue;

      import dev.slfc.epilogue.Epilogue;
      import dev.slfc.epilogue.Epiloguer;
      import dev.slfc.epilogue.logging.DataLogger;
      import dev.slfc.epilogue.logging.ClassSpecificLogger;
      import java.lang.invoke.VarHandle;

      public class HelloWorldLogger extends ClassSpecificLogger<HelloWorld> {

        public HelloWorldLogger() {
          super(HelloWorld.class);
        }

        @Override
        public void update(DataLogger dataLogger, String identifier, HelloWorld object) {
          try {
            if (Epiloguer.shouldLog(Epilogue.Importance.DEBUG)) {
              dataLogger.log(identifier + "/x", object.x);
              dataLogger.log(identifier + "/intArr1", object.intArr1);
            }
          } catch (Exception e) {
            System.err.println("[EPILOGUE] Encountered an error while logging: " + e.getMessage());
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void longs() {
    String source = """
      package dev.slfc.epilogue;

      @Epilogue
      class HelloWorld {
        long x;        // Should be logged
        long[] arr1;   // Should be logged
        long[][] arr2; // Should not be logged
      }
      """;

    String expectedGeneratedSource = """
      package dev.slfc.epilogue;

      import dev.slfc.epilogue.Epilogue;
      import dev.slfc.epilogue.Epiloguer;
      import dev.slfc.epilogue.logging.DataLogger;
      import dev.slfc.epilogue.logging.ClassSpecificLogger;
      import java.lang.invoke.VarHandle;

      public class HelloWorldLogger extends ClassSpecificLogger<HelloWorld> {

        public HelloWorldLogger() {
          super(HelloWorld.class);
        }

        @Override
        public void update(DataLogger dataLogger, String identifier, HelloWorld object) {
          try {
            if (Epiloguer.shouldLog(Epilogue.Importance.DEBUG)) {
              dataLogger.log(identifier + "/x", object.x);
              dataLogger.log(identifier + "/arr1", object.arr1);
            }
          } catch (Exception e) {
            System.err.println("[EPILOGUE] Encountered an error while logging: " + e.getMessage());
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void floats() {
    String source = """
      package dev.slfc.epilogue;

      @Epilogue
      class HelloWorld {
        float x;        // Should be logged
        float[] arr1;   // Should be logged
        float[][] arr2; // Should not be logged
      }
      """;

    String expectedGeneratedSource = """
      package dev.slfc.epilogue;

      import dev.slfc.epilogue.Epilogue;
      import dev.slfc.epilogue.Epiloguer;
      import dev.slfc.epilogue.logging.DataLogger;
      import dev.slfc.epilogue.logging.ClassSpecificLogger;
      import java.lang.invoke.VarHandle;

      public class HelloWorldLogger extends ClassSpecificLogger<HelloWorld> {

        public HelloWorldLogger() {
          super(HelloWorld.class);
        }

        @Override
        public void update(DataLogger dataLogger, String identifier, HelloWorld object) {
          try {
            if (Epiloguer.shouldLog(Epilogue.Importance.DEBUG)) {
              dataLogger.log(identifier + "/x", object.x);
              dataLogger.log(identifier + "/arr1", object.arr1);
            }
          } catch (Exception e) {
            System.err.println("[EPILOGUE] Encountered an error while logging: " + e.getMessage());
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void doubles() {
    String source = """
      package dev.slfc.epilogue;

      @Epilogue
      class HelloWorld {
        double x;        // Should be logged
        double[] arr1;   // Should be logged
        double[][] arr2; // Should not be logged
      }
      """;

    String expectedGeneratedSource = """
      package dev.slfc.epilogue;

      import dev.slfc.epilogue.Epilogue;
      import dev.slfc.epilogue.Epiloguer;
      import dev.slfc.epilogue.logging.DataLogger;
      import dev.slfc.epilogue.logging.ClassSpecificLogger;
      import java.lang.invoke.VarHandle;

      public class HelloWorldLogger extends ClassSpecificLogger<HelloWorld> {

        public HelloWorldLogger() {
          super(HelloWorld.class);
        }

        @Override
        public void update(DataLogger dataLogger, String identifier, HelloWorld object) {
          try {
            if (Epiloguer.shouldLog(Epilogue.Importance.DEBUG)) {
              dataLogger.log(identifier + "/x", object.x);
              dataLogger.log(identifier + "/arr1", object.arr1);
            }
          } catch (Exception e) {
            System.err.println("[EPILOGUE] Encountered an error while logging: " + e.getMessage());
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void booleans() {
    String source = """
      package dev.slfc.epilogue;

      @Epilogue
      class HelloWorld {
        boolean x;        // Should be logged
        boolean[] arr1;   // Should be logged
        boolean[][] arr2; // Should not be logged
      }
      """;

    String expectedGeneratedSource = """
      package dev.slfc.epilogue;

      import dev.slfc.epilogue.Epilogue;
      import dev.slfc.epilogue.Epiloguer;
      import dev.slfc.epilogue.logging.DataLogger;
      import dev.slfc.epilogue.logging.ClassSpecificLogger;
      import java.lang.invoke.VarHandle;

      public class HelloWorldLogger extends ClassSpecificLogger<HelloWorld> {

        public HelloWorldLogger() {
          super(HelloWorld.class);
        }

        @Override
        public void update(DataLogger dataLogger, String identifier, HelloWorld object) {
          try {
            if (Epiloguer.shouldLog(Epilogue.Importance.DEBUG)) {
              dataLogger.log(identifier + "/x", object.x);
              dataLogger.log(identifier + "/arr1", object.arr1);
            }
          } catch (Exception e) {
            System.err.println("[EPILOGUE] Encountered an error while logging: " + e.getMessage());
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void strings() {
    String source = """
      package dev.slfc.epilogue;

      @Epilogue
      class HelloWorld {
        String str;         // Should be logged
        String[] strArr1;   // Should be logged
        String[][] strArr2; // Should not be logged
      }
      """;

    String expectedGeneratedSource = """
      package dev.slfc.epilogue;

      import dev.slfc.epilogue.Epilogue;
      import dev.slfc.epilogue.Epiloguer;
      import dev.slfc.epilogue.logging.DataLogger;
      import dev.slfc.epilogue.logging.ClassSpecificLogger;
      import java.lang.invoke.VarHandle;

      public class HelloWorldLogger extends ClassSpecificLogger<HelloWorld> {

        public HelloWorldLogger() {
          super(HelloWorld.class);
        }

        @Override
        public void update(DataLogger dataLogger, String identifier, HelloWorld object) {
          try {
            if (Epiloguer.shouldLog(Epilogue.Importance.DEBUG)) {
              dataLogger.log(identifier + "/str", object.str);
              dataLogger.log(identifier + "/strArr1", object.strArr1);
            }
          } catch (Exception e) {
            System.err.println("[EPILOGUE] Encountered an error while logging: " + e.getMessage());
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void structs() {
    String source = """
      package dev.slfc.epilogue;

      import edu.wpi.first.util.struct.Struct;
      import edu.wpi.first.util.struct.StructSerializable;

      @Epilogue
      class HelloWorld {
        static class Structable implements StructSerializable {
          int x, y;

          public static final Struct<Structable> struct = null; // value doesn't matter
        }

        Structable x;        // Should be logged
        Structable[] arr1;   // Should be logged
        Structable[][] arr2; // Should not be logged
      }
      """;

    String expectedGeneratedSource = """
      package dev.slfc.epilogue;

      import dev.slfc.epilogue.Epilogue;
      import dev.slfc.epilogue.Epiloguer;
      import dev.slfc.epilogue.logging.DataLogger;
      import dev.slfc.epilogue.logging.ClassSpecificLogger;
      import java.lang.invoke.VarHandle;

      public class HelloWorldLogger extends ClassSpecificLogger<HelloWorld> {

        public HelloWorldLogger() {
          super(HelloWorld.class);
        }

        @Override
        public void update(DataLogger dataLogger, String identifier, HelloWorld object) {
          try {
            if (Epiloguer.shouldLog(Epilogue.Importance.DEBUG)) {
              dataLogger.log(identifier + "/x", object.x, dev.slfc.epilogue.HelloWorld.Structable.struct);
              dataLogger.log(identifier + "/arr1", object.arr1, dev.slfc.epilogue.HelloWorld.Structable.struct);
            }
          } catch (Exception e) {
            System.err.println("[EPILOGUE] Encountered an error while logging: " + e.getMessage());
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  private void assertLoggerGenerates(String loggedClassContent, String loggerClassContent) {
    Compilation compilation =
        javac()
            .withProcessors(new AnnotationProcessor())
            .compile(JavaFileObjects.forSourceString("dev.slfc.epilogue.HelloWorld", loggedClassContent));

    assertThat(compilation).succeededWithoutWarnings();
    var generatedFiles = compilation.generatedSourceFiles();
    assertEquals(2, generatedFiles.size());
    // first is Epiloguer
    // second is the class-specific logger
    var generatedFile = generatedFiles.getLast();
    try {
      var content = generatedFile.getCharContent(false);
      assertEquals(loggerClassContent, content);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}