// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.formatting;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;

/** A formatter for C code. Requires {@code clang-format} to be installed and in system PATH. */
public class ClangFormatter {

  private static final String CLANG_FORMAT = "clang-format";

  /** Formats and returns the C code given in {@code pCode}. */
  public static String format(String pCode, ClangFormatStyle pStyle, LogManager pLogger) {
    try {
      return format(pCode, pStyle);
    } catch (IOException | InterruptedException e) {
      if (e.getMessage().contains("Cannot run program")
          || e.getMessage().contains("No such file")) {
        pLogger.log(
            Level.SEVERE, CLANG_FORMAT, "not found. ensure that", CLANG_FORMAT, "is installed.");
      } else {
        pLogger.log(
            Level.SEVERE, CLANG_FORMAT, "failed due to an error. using unformatted code instead.");
      }
      pLogger.logfUserException(Level.SEVERE, e, e.getMessage());
    }
    return pCode;
  }

  private static String format(String pCode, ClangFormatStyle pStyle)
      throws IOException, InterruptedException {
    ProcessBuilder pb = new ProcessBuilder(CLANG_FORMAT, pStyle.getCommand());
    Process process = pb.start();
    // send code to clang-format stdin
    try (BufferedWriter writer =
        new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
      writer.write(pCode);
      writer.flush();
    }
    // read formatted code from stdout
    StringBuilder formattedCode = new StringBuilder();
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(process.getInputStream()))) {
      String line;
      while ((line = reader.readLine()) != null) {
        formattedCode.append(line).append(System.lineSeparator());
      }
    }
    // wait for process to complete
    int exitCode = process.waitFor();
    if (exitCode != 0) {
      // read stderr if something went wrong
      StringBuilder errorOutput = new StringBuilder();
      try (BufferedReader errorReader =
          new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
        String line;
        while ((line = errorReader.readLine()) != null) {
          errorOutput.append(line).append(System.lineSeparator());
        }
      }
      throw new IOException(
          CLANG_FORMAT + " failed with exit code " + exitCode + ":\n" + errorOutput);
    }
    return formattedCode.toString();
  }
}
