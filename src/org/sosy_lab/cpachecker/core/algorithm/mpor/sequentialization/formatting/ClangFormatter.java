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
import java.nio.charset.Charset;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;

/** A formatter for C code. Requires {@code clang-format} to be installed and in system PATH. */
public class ClangFormatter {

  private static final String CLANG_FORMAT = "clang-format";

  private static final Charset charset = Charset.defaultCharset();

  /** Formats and returns the C code given in {@code pCode} if enabled. */
  public static String tryFormat(MPOROptions pOptions, String pCode, LogManager pLogger) {
    if (pOptions.clangFormatStyle.isEnabled()) {
      try {
        return format(pCode, pOptions.clangFormatStyle);
      } catch (IOException | InterruptedException e) {
        pLogger.logfUserException(
            Level.SEVERE,
            e,
            CLANG_FORMAT + " failed due to an error. using unformatted code instead.");
      }
    }
    return pCode;
  }

  private static String format(String pCode, ClangFormatStyle pStyle)
      throws IOException, InterruptedException {
    ProcessBuilder pb = new ProcessBuilder(CLANG_FORMAT, pStyle.getCommand());
    Process process = pb.start();
    // send code to clang-format stdin
    try (BufferedWriter writer =
        new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), charset))) {
      writer.write(pCode);
      writer.flush();
    }
    // read formatted code from stdout
    StringBuilder formattedCode = new StringBuilder();
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(process.getInputStream(), charset))) {
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
          new BufferedReader(new InputStreamReader(process.getErrorStream(), charset))) {
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
