// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cmdline;

import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import java.io.PrintStream;
import org.checkerframework.dataflow.qual.TerminatesExecution;
import org.sosy_lab.common.annotations.SuppressForbidden;

/** Utilities for output while we do not yet have a logger */
@SuppressForbidden("System.out in this class is ok")
class Output {

  private static final PrintStream ERROR_OUTPUT = System.err;

  private static final boolean USE_COLORS =
      (System.console() != null) && !System.getProperty("os.name", "").startsWith("Windows");
  private static final String ERROR_COLOR = "\033[31;1m"; // bold red
  private static final String WARNING_COLOR = "\033[1m"; // bold
  private static final String REGULAR_COLOR = "\033[m";

  /**
   * Output an error message and terminate process with error code.
   *
   * @return never, this is just so that <code>throw fatalError(...);</code> can be written
   */
  @TerminatesExecution
  @FormatMethod
  static RuntimeException fatalError(String msg, Object... args) {
    coloredOutput(ERROR_COLOR, msg, args);
    System.exit(CPAMain.ERROR_EXIT_CODE);
    return new RuntimeException("never reached");
  }

  /**
   * Output an error message and a help text and terminate process with error code.
   *
   * @return never, this is just so that <code>throw fatalError(...);</code> can be written
   */
  @TerminatesExecution
  @FormatMethod
  static RuntimeException fatalErrorWithHelptext(String msg, Object... args) {
    coloredOutput(ERROR_COLOR, msg, args);
    CmdLineArguments.printHelp(ERROR_OUTPUT);
    System.exit(CPAMain.ERROR_EXIT_CODE);
    return new RuntimeException("never reached");
  }

  /** Output a warning. */
  @FormatMethod
  static void warning(String msg, Object... args) {
    coloredOutput(WARNING_COLOR, msg, args);
    ERROR_OUTPUT.println();
  }

  @FormatMethod
  private static void coloredOutput(String color, @FormatString String msg, Object... args) {
    ERROR_OUTPUT.println();

    if (USE_COLORS) {
      ERROR_OUTPUT.print(color);
    }

    ERROR_OUTPUT.printf(msg, args);

    if (USE_COLORS) {
      ERROR_OUTPUT.print(REGULAR_COLOR); // regular color
    }

    ERROR_OUTPUT.println();
  }
}
