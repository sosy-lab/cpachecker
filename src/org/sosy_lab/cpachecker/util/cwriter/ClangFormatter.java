// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter;

import java.io.IOException;
import java.util.logging.Level;
import org.sosy_lab.common.ProcessExecutor;
import org.sosy_lab.common.log.LogManager;

/** A formatter for C code. Requires {@code clang-format} to be installed and in system PATH. */
public class ClangFormatter {

  private static final String CLANG_FORMAT = "clang-format";

  /**
   * Tries to format and return the C code given in {@code pCode} using {@code clang}. If it fails,
   * returns {@code pCode} as is.
   */
  public static String tryFormat(String pCode, ClangFormatStyle pStyle, LogManager pLogger)
      throws InterruptedException {

    try {
      return format(pCode, pStyle, pLogger);
    } catch (IOException e) {
      pLogger.logfUserException(
          Level.WARNING,
          e,
          CLANG_FORMAT + " failed due to an error. Returning unformatted code instead.");
    }
    return pCode;
  }

  private static String format(String pCode, ClangFormatStyle pStyle, LogManager pLogger)
      throws IOException, InterruptedException {

    ProcessExecutor<IOException> executor =
        new ProcessExecutor<>(pLogger, IOException.class, CLANG_FORMAT, pStyle.getCommand());

    // send code to clang-format via stdin
    executor.print(pCode);
    executor.sendEOF();
    // wait for clang-format to finish
    int exitCode = executor.join();

    if (exitCode != 0) {
      String errorOutput = String.join(System.lineSeparator(), executor.getErrorOutput());
      throw new IOException(
          String.format("%s failed with exit code %d:%n%s", CLANG_FORMAT, exitCode, errorOutput));
    }

    // collect and return formatted output
    return String.join(System.lineSeparator(), executor.getOutput()) + System.lineSeparator();
  }
}
