package dev.slfc.epilogue.processor;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
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
          if (Epiloguer.shouldLog(Epilogue.Importance.DEBUG)) {
            dataLogger.log(identifier + "/x", object.x);
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
          if (Epiloguer.shouldLog(Epilogue.Importance.DEBUG)) {
            dataLogger.log(identifier + "/x", object.x);
            dataLogger.log(identifier + "/y", object.y);
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
          if (Epiloguer.shouldLog(Epilogue.Importance.DEBUG)) {
            dataLogger.log(identifier + "/x", (double) $x.get(object));
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
          if (Epiloguer.shouldLog(Epilogue.Importance.DEBUG)) {
            logSendable(dataLogger, identifier + "/chooser", (edu.wpi.first.wpilibj.smartdashboard.SendableChooser<java.lang.String>) $chooser.get(object));
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
          if (Epiloguer.shouldLog(Epilogue.Importance.DEBUG)) {
            dataLogger.log(identifier + "/low", object.low);
          }
          if (Epiloguer.shouldLog(Epilogue.Importance.INFO)) {
            dataLogger.log(identifier + "/medium", object.medium);
          }
          if (Epiloguer.shouldLog(Epilogue.Importance.CRITICAL)) {
            dataLogger.log(identifier + "/high", object.high);
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
          if (Epiloguer.shouldLog(Epilogue.Importance.DEBUG)) {
            dataLogger.log(identifier + "/enumValue", (dev.slfc.epilogue.HelloWorld.E) $enumValue.get(object));
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
          if (Epiloguer.shouldLog(Epilogue.Importance.DEBUG)) {
            dataLogger.log(identifier + "/x", object.x);
            dataLogger.log(identifier + "/arr1", object.arr1);
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
          if (Epiloguer.shouldLog(Epilogue.Importance.DEBUG)) {
            dataLogger.log(identifier + "/x", object.x);
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
          if (Epiloguer.shouldLog(Epilogue.Importance.DEBUG)) {
            dataLogger.log(identifier + "/x", object.x);
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
          if (Epiloguer.shouldLog(Epilogue.Importance.DEBUG)) {
            dataLogger.log(identifier + "/x", object.x);
            dataLogger.log(identifier + "/intArr1", object.intArr1);
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
          if (Epiloguer.shouldLog(Epilogue.Importance.DEBUG)) {
            dataLogger.log(identifier + "/x", object.x);
            dataLogger.log(identifier + "/arr1", object.arr1);
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
          if (Epiloguer.shouldLog(Epilogue.Importance.DEBUG)) {
            dataLogger.log(identifier + "/x", object.x);
            dataLogger.log(identifier + "/arr1", object.arr1);
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
      
      import java.util.List;

      @Epilogue
      class HelloWorld {
        double x;        // Should be logged
        double[] arr1;   // Should be logged
        double[][] arr2; // Should not be logged
        List<Double> list; // Should not be logged
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
          if (Epiloguer.shouldLog(Epilogue.Importance.DEBUG)) {
            dataLogger.log(identifier + "/x", object.x);
            dataLogger.log(identifier + "/arr1", object.arr1);
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
      import java.util.List;

      @Epilogue
      class HelloWorld {
        boolean x;        // Should be logged
        boolean[] arr1;   // Should be logged
        boolean[][] arr2; // Should not be logged
        List<Boolean> list; // Should not be logged
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
          if (Epiloguer.shouldLog(Epilogue.Importance.DEBUG)) {
            dataLogger.log(identifier + "/x", object.x);
            dataLogger.log(identifier + "/arr1", object.arr1);
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
      
      import java.util.List;

      @Epilogue
      class HelloWorld {
        String str;         // Should be logged
        String[] strArr1;   // Should be logged
        String[][] strArr2; // Should not be logged
        List<String> list;  // Should be logged
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
          if (Epiloguer.shouldLog(Epilogue.Importance.DEBUG)) {
            dataLogger.log(identifier + "/str", object.str);
            dataLogger.log(identifier + "/strArr1", object.strArr1);
            dataLogger.log(identifier + "/list", (object.list).toArray(java.lang.String[]::new));
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
      import java.util.List;

      @Epilogue
      class HelloWorld {
        static class Structable implements StructSerializable {
          int x, y;

          public static final Struct<Structable> struct = null; // value doesn't matter
        }

        Structable x;        // Should be logged
        Structable[] arr1;   // Should be logged
        Structable[][] arr2; // Should not be logged
        List<Structable> list; // Should be logged
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
          if (Epiloguer.shouldLog(Epilogue.Importance.DEBUG)) {
            dataLogger.log(identifier + "/x", object.x, dev.slfc.epilogue.HelloWorld.Structable.struct);
            dataLogger.log(identifier + "/arr1", object.arr1, dev.slfc.epilogue.HelloWorld.Structable.struct);
            dataLogger.log(identifier + "/list", (object.list).toArray(dev.slfc.epilogue.HelloWorld.Structable[]::new), dev.slfc.epilogue.HelloWorld.Structable.struct);
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void lists() {
    String source = """
      package dev.slfc.epilogue;

      import edu.wpi.first.util.struct.Struct;
      import edu.wpi.first.util.struct.StructSerializable;
      import java.util.*;

      @Epilogue
      class HelloWorld {
        /* Logged */     List<String> list;
        /* Not Logged */ List<List<String>> nestedList;
        /* Not logged */ List rawList;
        /* Logged */     Set<String> set;
        /* Logged */     Queue<String> queue;
        /* Logged */     Stack<String> stack;
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
          if (Epiloguer.shouldLog(Epilogue.Importance.DEBUG)) {
            dataLogger.log(identifier + "/list", (object.list).toArray(java.lang.String[]::new));
            dataLogger.log(identifier + "/set", (object.set).toArray(java.lang.String[]::new));
            dataLogger.log(identifier + "/queue", (object.queue).toArray(java.lang.String[]::new));
            dataLogger.log(identifier + "/stack", (object.stack).toArray(java.lang.String[]::new));
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void boxedPrimitiveLists() {
    // Boxed primitives are not directly supported, nor are arrays of boxed primitives
    // int[] is fine, but Integer[] is not

    String source = """
      package dev.slfc.epilogue;

      import edu.wpi.first.util.struct.Struct;
      import edu.wpi.first.util.struct.StructSerializable;
      import java.util.List;

      @Epilogue
      class HelloWorld {
        /* Not logged */ List<Integer> ints;
        /* Not logged */ List<Double> doubles;
        /* Not logged */ List<Long> longs;
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
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void badLogSetup() {
    String source = """
      package dev.slfc.epilogue;

      import edu.wpi.first.util.struct.Struct;
      import edu.wpi.first.util.struct.StructSerializable;
      import java.util.*;

      @Epilogue
      class HelloWorld {
        @Epilogue Map<String, String> notLoggableType;
        @Epilogue List rawType;
        @Epilogue(importance = Epilogue.Importance.NONE) List skippedUnloggable;

        @Epilogue
        private String privateMethod() { return ""; }

        @Epilogue
        String packagePrivateMethod() { return ""; }

        @Epilogue
        protected String protectedMethod() { return ""; }

        @Epilogue
        public static String publicStaticMethod() { return ""; }
        
        @Epilogue
        private static String privateStaticMethod() { return ""; }

        @Epilogue
        public void publicVoidMethod() {}

        @Epilogue
        public Map<String, String> publicNonLoggableMethod() { return notLoggableType; }
      }
      """;

    Compilation compilation =
        javac()
            .withProcessors(new AnnotationProcessor())
            .compile(JavaFileObjects.forSourceString("dev.slfc.epilogue.HelloWorld", source));

    assertThat(compilation).failed();
    assertThat(compilation).hadErrorCount(10);

    List<Diagnostic<? extends JavaFileObject>> errors = compilation.errors();
    assertAll(
        () -> assertCompilationError("[EPILOGUE] You have opted in to Epilogue logging on this field, but it is not a loggable data type!", 9, 33, errors.get(0)),
        () -> assertCompilationError("[EPILOGUE] You have opted in to Epilogue logging on this field, but it is not a loggable data type!", 10, 18, errors.get(1)),
        () -> assertCompilationError("[EPILOGUE] Logged methods must be public", 14, 18, errors.get(2)),
        () -> assertCompilationError("[EPILOGUE] Logged methods must be public", 17, 10, errors.get(3)),
        () -> assertCompilationError("[EPILOGUE] Logged methods must be public", 20, 20, errors.get(4)),
        () -> assertCompilationError("[EPILOGUE] Logged methods cannot be static", 23, 24, errors.get(5)),
        () -> assertCompilationError("[EPILOGUE] Logged methods must be public", 26, 25, errors.get(6)),
        () -> assertCompilationError("[EPILOGUE] Logged methods cannot be static", 26, 25, errors.get(7)),
        () -> assertCompilationError("[EPILOGUE] You have opted in to Epilogue logging on this method, but it does not return a loggable data type!", 29, 15, errors.get(8)),
        () -> assertCompilationError("[EPILOGUE] You have opted in to Epilogue logging on this method, but it does not return a loggable data type!", 32, 30, errors.get(9))
    );
  }

  @Test
  void onGenericType() {
    String source = """
      package dev.slfc.epilogue;

      @Epilogue
      class HelloWorld<T extends String> {
        T value;

        public <S extends T> S upcast() { return (S) value; }
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
          if (Epiloguer.shouldLog(Epilogue.Importance.DEBUG)) {
            dataLogger.log(identifier + "/value", object.value);
            dataLogger.log(identifier + "/upcast", object.upcast());
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  private void assertCompilationError(String message, long lineNumber, long col, Diagnostic<? extends JavaFileObject> diagnostic) {
    assertAll(
        () -> assertEquals(Diagnostic.Kind.ERROR, diagnostic.getKind(), "not an error"),
        () -> assertEquals(message, diagnostic.getMessage(Locale.getDefault()), "error message mismatch"),
        () -> assertEquals(lineNumber, diagnostic.getLineNumber(), "line number mismatch"),
        () -> assertEquals(col, diagnostic.getColumnNumber(), "column number mismatch")
    );
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