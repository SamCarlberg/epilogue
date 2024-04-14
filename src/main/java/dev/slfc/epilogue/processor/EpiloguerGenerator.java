package dev.slfc.epilogue.processor;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

/**
 * Generates the {@code Epiloguer} file used as the main entry point to logging with Epilogue in a
 * robot program. {@code Epiloguer} has instances of every generated logger class, a
 * {@link dev.slfc.epilogue.EpilogueConfiguration config} object, and (if the main robot class
 * inherits from {@link edu.wpi.first.wpilibj.TimedRobot TimedRobot}) a {@code bind()} method to
 * automatically add a periodic logging call to the robot.
 */
public class EpiloguerGenerator {
  private final ProcessingEnvironment processingEnv;
  private final Map<TypeMirror, DeclaredType> customLoggers;

  public EpiloguerGenerator(ProcessingEnvironment processingEnv, Map<TypeMirror, DeclaredType> customLoggers) {
    this.processingEnv = processingEnv;
    this.customLoggers = customLoggers;
  }

  /**
   * Creates the Epiloguer file, which is the main entry point for users to set up and interact
   * with the generated loggers.
   *
   * @param loggerClassNames the names of the generated logger classes. Each of these will be
   *                         instantiated in a public static field on the Epiloguer class.
   * @param mainRobotClass the main robot class. May be null. Used to generate a {@code bind()}
   *                       method to add a callback hook to a TimedRobot to log itself.
   */
  public void writeEpiloguerFile(List<String> loggerClassNames, Collection<TypeElement> mainRobotClasses) {
    try {
      var centralStore = processingEnv.getFiler().createSourceFile("dev.slfc.epilogue.Epiloguer");

      try (var out = new PrintWriter(centralStore.openOutputStream())) {
        out.println("package dev.slfc.epilogue;");
        out.println();

        loggerClassNames.stream().sorted().forEach(name -> {
          if (!name.contains(".")) {
            // Logger is in the global namespace, don't need to import
            return;
          }

          out.print("import ");
          out.print(name);
          out.println(";");
        });
        out.println();

        out.println("public final class Epiloguer {");
        out.println("  private static final EpilogueConfiguration config = new EpilogueConfiguration();");
        out.println();

        loggerClassNames.forEach(name -> {
          String simple = StringUtils.simpleName(name);

          // public static final FooLogger fooLogger = new FooLogger();
          out.print("  public static final ");
          out.print(simple);
          out.print(" ");
          out.print(StringUtils.lowerCamelCase(simple));
          out.print(" = new ");
          out.print(simple);
          out.println("();");
        });
        out.println();

        customLoggers.forEach((targetType, loggerType) -> {
          var loggerTypeName = loggerType.asElement().getSimpleName();
          out.println("  public static final " + loggerType + " " + StringUtils.lowerCamelCase(loggerTypeName) + " = new " + loggerType + "();");
        });

        out.println("""
              public static void configure(java.util.function.Consumer<EpilogueConfiguration> configurator) {
                configurator.accept(config);
              }

              public static EpilogueConfiguration getConfig() {
                return config;
              }
            """);

        out.println("""
              /**
               * Checks if data associated with a given importance level should be logged.
               */
              public static boolean shouldLog(Epilogue.Importance importance) {
                return importance.compareTo(config.minimumImportance) >= 0;
              }
            """.stripTrailing());

        // Only generate a binding if the robot class is a TimedRobot
        if (!mainRobotClasses.isEmpty()) {
          for (TypeElement mainRobotClass : mainRobotClasses) {
            String robotClassName = mainRobotClass.getQualifiedName().toString();

            out.println();
            out.print("""
                  /**
                   * Binds Epilogue updates to a timed robot's update period. Log calls will be made at the
                   * same update rate as the robot's loop function, but will be offset by a full phase
                   * (for example, a 20ms update rate but 10ms offset from the main loop invocation) to
                   * help avoid high CPU loads. However, this does mean that any logged data that reads
                   * directly from sensors will be slightly different from data used in the main robot
                   * loop.
                   */
                """);
            out.println("  public static void bind(" + robotClassName + " robot) {");
            out.println("    robot.addPeriodic(() -> {");
            out.println("      long start = System.nanoTime();");
            out.println("      " + StringUtils.lowerCamelCase(StringUtils.simpleName(robotClassName)) + "Logger.tryUpdate(config.dataLogger.getSubLogger(config.root), robot, config.errorHandler);");
            out.println("      edu.wpi.first.networktables.NetworkTableInstance.getDefault().getEntry(\"Epilogue/Stats/Last Run\").setDouble((System.nanoTime() - start) / 1e6);");
            out.println("    }, robot.getPeriod(), robot.getPeriod() / 2);");
            out.println("  }");
          }
        }

        out.println("}");
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
