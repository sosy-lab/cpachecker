// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.output;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;

public class SequentializationWriter {

  // TODO is there a way to retrieve the current license somewhere? ...
  private static final String license =
      "// This file is part of CPAchecker,\n"
          + "// a tool for configurable software verification:\n"
          + "// https://cpachecker.sosy-lab.org\n"
          + "//\n"
          + "// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>\n"
          + "//\n"
          + "// SPDX-License-Identifier: Apache-2.0";

  private static final String targetDirectory = "test/programs/mpor_seq/";

  private final LogManager logManager;

  private final String inputFileComment;

  private final File outputFile;

  public SequentializationWriter(LogManager pLogManager, Path pInputFilePath) {
    logManager = pLogManager;
    String inputFileName = pInputFilePath.getFileName().toString();
    inputFileComment = createInputFileComment(pInputFilePath.toString());
    String outputFilePath = targetDirectory + "mpor_seq__" + inputFileName;
    outputFile = new File(outputFilePath);
  }

  public void write(String pOutputProgram) {
    String sequentialization = license + "\n\n" + inputFileComment + pOutputProgram;
    try {
      File parentDir = outputFile.getParentFile();
      // ensure the target directory exists
      if (!parentDir.exists()) {
        logManager.log(
            Level.SEVERE,
            () ->
                "MPOR FAIL. No sequentialization created, make sure the target directory exists in"
                    + " CPAchecker: "
                    + targetDirectory);

        // ensure the file does not exist already (no overwriting)
      } else if (!outputFile.createNewFile()) {
        logManager.log(
            Level.SEVERE,
            () ->
                "MPOR FAIL. No sequentialization created, file exists already: "
                    + outputFile.getAbsolutePath());
      } else {
        logManager.log(
            Level.INFO,
            () -> "MPOR SUCCESS. Sequentialization created: " + outputFile.getAbsolutePath());
        // write content to the file
        try (Writer writer = Files.newBufferedWriter(outputFile.toPath(), StandardCharsets.UTF_8)) {
          writer.write(sequentialization);
        }
      }
    } catch (IOException e) {
      logManager.log(
          Level.SEVERE,
          () -> "An IO error occurred while writing the sequentialization: " + e.getMessage());
    }
  }

  private String createInputFileComment(String pInputFilePath) {
    return "// This sequentialization (transformation of a parallel program into an equivalent \n"
        + "// sequential program) was created by the MPORAlgorithm implemented in CPAchecker. \n"
        + "// Input file path: "
        + pInputFilePath
        + "\n\n";
  }
}
