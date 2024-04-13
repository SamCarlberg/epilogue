# Epilogue

Epilogue is an a low overhead annotation-based data logging library for FRC.

## How it works

Epilogue does not use reflection at runtime, which keeps CPU overhead to a minimum. Instead, an annotation processor will run when your project is compiled and scan for the classes you've marked with the `@Epilogue` annotation. Custom logger files will be generated for each annotated class that will be used at runtime to directly read from the fields and methods on the objects being logged - no reflection required. The code generation also creates an `Epiloguer` class in the `dev.slfc.epilogue` package that contains instances of all of the generated custom loggers and - if you're logging your main robot class - a convenience method to automatically set up logging.

## Usage

Annotate the classes you're interested in logging using the `@Epilogue` annotation. Classes that implement `Sendable` (with the exceptions of commands and subsystems) will be logged using their sendable implementation and do not need to be annotated. Likewise, classes that declare a `public static final Struct struct` object for struct-base serialization will be serialized to raw bytes using that serialization path and also do not need to be annotated.

By default, an `@Epilogue` annotation on a class will result in logging of every field of a loggable type and every public no-argument method returning a loggable type.

Be aware that some sensors may have blocking reads that force your program to wait until data is received. If Epilogue is set up to call too many of those methods, you may see performance degrade, manifesting as loop time overrun messages in the driverstation. You can alleviate the issue by either marking the offending sensors or methods that read from those sensors as skipped with `@Epilogue(importance = NONE)` on the field or methods, or by periodically reading from those sensors in a subsystem or robot periodic method and only refer to those cached values in your calculations and in the log configuration.

### Annotating

The only annotation you need to use is `@Epilogue`. It can be placed on classes to mark them as loggable, and on fields and methods within an annotated class to configure how they get logged.

```java
@Epilogue(strategy = Strategy.OPT_IN)
class Drivebase extends SubsystemBase {
  @Epilogue(importance = NONE)
  private Pose2d lastKnownPosition; // This field will never be logged

  @Epilogue(name = "Pose")
  Pose2d getPose(); // Logged under "<...>/Pose"

  Rotation2d getHeading(); // Top-level strategy is opt-in, so this is excluded

  @Epilogue
  Measure<Velocity<Distance>> speed(); // Logged under "<...>/speed"
}
```

### Loggable Types

The data types supported by Epilogue are:

-  `int` 
-  `long` 
-  `float` 
-  `double` 
-  `byte[]` 
-  `int[]` 
-  `long[]` 
-  `float[]` 
-  `double[]` 
-  `boolean` 
-  `boolean[]` 
-  `String` 
-  `String[]`
- `StructSerializable`
- `StructSerializable[]`
- `Collection<String>`
- `Collection<StructSerializable>`
- `Enum` values (logged using the enum constant name)
- `Measure` values (logged as doubles in terms of their base units)
-  `Sendable`
  - Except for `Command`, which would never log anything meaningful,
  - And `Subsystem`, which is better off logging using Epilogue instead of the default sendable implementation

Additionally, any class that has a `public static final Struct struct` field declared is also loggable using raw struct data serialization (think `Rotation2d` or `SwerveModuleState`).

Any class that is directly marked with `@Epilogue` is also loggable by other classes, allowing for a nested data structure.
**NOTE**: The declared type of the field or method must *exactly* match the declared type for a logger, or they will not be logged.

```java
@Epilogue
class Child {}

class GoldenChild extends Child {}

@Epilogue
class Parent {
  // OK - the Child class is annotated, so this can be logged
  Child child;

  // Not OK - the GoldenChild class is not annotated, so this can't be logged,
  // even though it inherits from a loggable type
  GoldenChild favoriteChild;

  // OK - the return type matches, so this can be logged
  Child getChild() { return child; }

  // Not OK - even though this returns an object of a loggable type,
  // the compiler doesn't know that
  Object getChildUntyped() { return child; }
}
```

### Epiloguer

`Epiloguer` is a special class generated at compile time to make it easier to interface with the library at runtime and start logging. It offers two main methods: one, a `configure` method that lets you customize the behavior of logging at runtime; and two, a `bind` method that lets you start logging with a single method call if your robot class inherits from `TimedRobot`.

Here's a minimal example of setting up using the default configuration. By default, any internal errors encountered (such as method invocations throwing unchecked exceptions) will be printed to the console but will let the program keep running. You can turn this behavior off and have errors crash your program if you want to find bugs in unit testing or simulation. It's *strongly* recommended to have this turned off when running on a field!
The default logging implementation also does nothing; data sent to the log will be ignored. This can help cut down on high CPU usage if logging calls adversely impact your robot's performance. However, this also means Epilogue won't actually log any data.

```java
@Epilogue
public class Robot extends TimedRobot {
  @Override
  public void robotInit() {
    Epiloguer.bind(this);
  }
}
```

Here's a more realistic example:

```java
@Epilogue
public class Robot extends TimedRobot {
  @Override
  public void robotInit() {
    Epiloguer.configure(config -> {
      // Log to a .wpilog file on the roboRIO's filesystem
      config.dataLogger = new FileLogger(DataLogManager.getLog());

      // ... or, send data to NetworkTables for telemetry
      // and possible client-side data recording
      config.dataLogger = new NTDataLogger(NetworkTableInstance.getDefault());

      // ... or, do both! MultiLogger allows the same data be passed
      // to arbitrarily many other data loggers
      config.dataLogger = new MultiLogger(
        new FileLogger(DataLogManager.getLog()),
        new NTDataLogger(NetworkTableInstance.getDefault())
      );
    });

    Epiloguer.bind(this);
  }
}
```

## Examples

### Introductory

This example will log robot data to the datalog file on the roboRIO. The logged data includes the values of `previousPose`, `getPose()` and `velocity` (which will appear in terms of meters per second). 

```java
@Epilogue
class Robot extends TimedRobot {
  Pose2d previousPose;

  Pose2d getPose();

  Measure<Velocity<Distance>> velocity();

  @Override
  public void robotInit() {
    Epiloguer.configure(config -> {
      config.dataLogger = new FileLogger(DataLogManager.getLog());
    });

    Epiloguer.bind(this);
  }
}
```

### Importance Levels

Data can be flagged with difference importance levels, which can be used to configure how much data should get logged. For example, you might want to gather as much information as possible in your workspace, but only log the really important things during a match to keep CPU usage and network bandwidth down.

By default, all data fields are treated as having the `DEBUG` information level unless otherwise specified. It can be set either on the class-level `@Epilogue` annotation to set the default for all fields within that class to something else, or on the individual fields themselves.

| Information Level | Description                                                                                                             |
|-------------------|-------------------------------------------------------------------------------------------------------------------------|
| `NONE`            | Any data field flagged with `importance = NONE` will never be logged                                                    |
| `DEBUG`           | Low-level information like raw sensor data that is useful for tuning controls and troubleshooting                       |
| `INFO`            | Medium-level information that is useful for tracking higher-level concepts like subsystem states and a robot's position |
| `CRITICAL`        | Critical information like hardware or mechanism faults that should always be included in logs                           |

```java
// Treat everything in this class as critical information unless otherwise specified
@Epilogue(importance = Epilogue.Importance.CRITICAL)
class Robot extends TimedRobot {
  // This is low importance, override the class-level default
  @Epilogue(importance = Epilogue.Importance.DEBUG)
  Pose2d previousPose;

  // Medium importance, override the class-level default
  @Epilogue(importance = Epilogue.Importance.INFO)
  Pose2d getPose();

  // Not explicitly configured. Therefore, per the class-level default, this is considered critical
  Measure<Velocity<Distance>> velocity();

  // This field is utterly unimportant and should never be logged
  @Epilogue(importance = Epilogue.Importance.NONE)
  private double ignored;

  @Override
  public void robotInit() {
    Epiloguer.configure(config -> {
      // Setting minimum importance to INFO excludes previousPose,
      // since DEBUG is less important than INFO
      config.minimumImportance = Epilogue.Importance.INFO;
      config.dataLogger = new FileLogger(DataLogManager.getLog());
    });

    Epiloguer.bind(this);
  }
}
```

### Error Handling

The default error handler used by Epilogue will print out errors to the standard output. This helps prevent logging setups from causing robot code to crash at inopportune times (such as during an official match!).

Error handling behavior can be configured with the `errorHandler` property. Epilogue comes with three types of error handlers by default: the one that prints errors to the console; one that rethrows the errors and causes code to crash; and one that automatically disables loggers after too many exceptions are encountered during use.

The `errorHandler` property is a functional interface, and can be set using a lambda function. The function accepts the logger that encountered the error, and the exception object that was encountered.

```java
@Epilogue
class Robot extends TimedRobot {
  @Override
  public void robotInit() {
    Epiloguer.configure(config -> {
      if (isSimulation()) {
        // In simulation or unit tests, rethrow any errors encountered during logging
        // This lets us quickly find and fix bugs before they make it to a real robot
        config.errorHandler = ErrorHandler.crashOnError();
      } else {
        // Running on a real robot, allow loggers to continue running after a single error.
        // But if a logger later encounters a second error, disable it.
        config.errorHandler = ErrorHandler.disabling(1);
      }

      // Or use something custom:
      config.errorHandler = (logger, exception) -> {
        // ... custom error handling logic
      };
    });

    Epiloguer.bind(this);
  }
}
```