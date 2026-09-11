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
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;

/** A formatter for C code. Requires {@code clang-format} to be installed and in system PATH. */
@Options(prefix = "clangFormatter")
public class ClangFormatter {

  @Option(
      secure = false,
      description =
          "define the clang-format command to use, e.g. \"clang-format-18\" or \"clang-format\"")
  private String clangFormatCommand = "clang-format-18";

  private final LogManager logger;

  public ClangFormatter(Configuration pConfiguration, LogManager pLogger)
      throws InvalidConfigurationException {

    pConfiguration.inject(this);
    logger = pLogger;
  }

  /**
   * Tries to format and return the C code given in {@code pCode} using {@code clang}. If it fails,
   * returns {@code pCode} as is.
   */
  public String tryFormat(String pCode, ClangFormatStyle pStyle) throws InterruptedException {
    try {
      return format(pCode, pStyle);
    } catch (IOException e) {
      logger.logfUserException(
          Level.WARNING,
          e,
          "%s failed due to an error. Returning unformatted code instead.",
          clangFormatCommand);
    }
    return pCode;
  }

  private String format(String pCode, ClangFormatStyle pStyle)
      throws IOException, InterruptedException {

    ProcessExecutor<IOException> executor =
        new ProcessExecutor<>(logger, IOException.class, clangFormatCommand, pStyle.getCommand());

    // send code to clang-format via stdin
    executor.print(pCode);
    executor.sendEOF();
    // wait for clang-format to finish
    int exitCode = executor.join();

    if (exitCode != 0) {
      String errorOutput = String.join(System.lineSeparator(), executor.getErrorOutput());
      throw new IOException(
          String.format(
              "%s failed with exit code %d:%n%s", clangFormatCommand, exitCode, errorOutput));
    }

    // collect and return formatted output
    return String.join(System.lineSeparator(), executor.getOutput()) + System.lineSeparator();
  }
}
