// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.svlibwitnessexport;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.logging.Level;
import org.sosy_lab.common.Appenders;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibCommand;

public class WitnessExportUtils {

  public static void writeCommandsAsWitness(
      Path pOutputPath, Iterable<? extends SvLibCommand> pCommands, LogManager pLogger) {
    FluentIterable<String> witnessContentLines =
        FluentIterable.of("(")
            .append(
                FluentIterable.from(pCommands).transform(SvLibCommand::toASTString).append(")"));

    // The catch block was not triggered, so we can proceed to write the witness
    try {
      IO.writeFile(
          pOutputPath,
          Charset.defaultCharset(),
          Appenders.forIterable(Joiner.on(System.lineSeparator()), witnessContentLines));
    } catch (IOException e) {
      pLogger.logUserException(
          Level.WARNING,
          e,
          "Could not write the SV-LIB violation witness to file: "
              + pOutputPath
              + ". Therefore no SV-LIB witness will be exported.");
    }
  }
}
