/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.sosy_lab.cpachecker.cmdline;

import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import java.io.PrintStream;
import org.checkerframework.dataflow.qual.TerminatesExecution;

/** Utilities for output while we do not yet have a logger */
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
