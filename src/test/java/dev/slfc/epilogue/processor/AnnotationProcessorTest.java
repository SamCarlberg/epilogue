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
  void simple() throws IOException {
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
  void multiple() throws IOException {
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
  void privateFields() throws IOException {
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
  void privateWithGenerics() throws IOException {
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
  void importanceLevels() throws IOException {
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

  private void assertLoggerGenerates(String loggedClassContent, String loggerClassContent) throws IOException {
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
    var content = generatedFile.getCharContent(false);
    assertEquals(loggerClassContent, content);
  }
}