package org.sosy_lab.cpachecker.cpa.qMultiInterval;

// This logger was used mostly for the implementation, but got obsolete as it was replaced by the
// LogManager to meet CPAchecker standards. I used this one, because with it it was much more
// comfortable to work with.

/** Logger */
public class Log {
  private static boolean enable = true;
  private static boolean info = true;

  @SuppressWarnings("all")
  /**
   * Prints the given string representation of the given Object. Additionally prints the class and
   * line in which this method was called.
   *
   * @param message Object to print
   */
  public static void Log2(Object message) {
    if (enable) {
      String fullClassName = Thread.currentThread().getStackTrace()[2].getClassName();
      String className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
    }
  }

  /**
   * Prints the given string representation of the given Object. Additionally prints the class and
   * line in which this method was called.
   *
   * @param message Object to print
   */
  public static void LogInfo2(Object message) {
    if (info && enable) {
      String fullClassName = Thread.currentThread().getStackTrace()[2].getClassName();
      String className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
    }
  }

  /** Disables all outputs from this Logger */
  public static void Disable() {
    enable = false;
  }

  /** Disables only the info method */
  public static void DisableInfo() {
    info = false;
  }

  /**
   * Just prints a new line of "-------------------------------------", mostly for a better overview
   */
  public static void NewLine() {
    if (enable) {
    }
  }

}
