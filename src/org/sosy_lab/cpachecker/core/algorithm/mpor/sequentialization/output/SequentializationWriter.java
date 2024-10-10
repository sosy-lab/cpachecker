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
          + "// SPDX-License-Identifier: Apache-2.0\n\n";

  private static final String targetDirectory = "test/programs/mpor_seq/";

  private final LogManager logManager;

  private final String filePath;

  private final File file;

  public SequentializationWriter(LogManager pLogManager, String pInputFileName) {
    logManager = pLogManager;
    filePath = targetDirectory + "mpor_seq_" + pInputFileName + ".i";
    file = new File(filePath);
  }

  public void write(String pOutputProgram) {
    String sequentialization = license + pOutputProgram;
    try {
      File parentDir = file.getParentFile();
      // ensure the target directory exists
      if (!parentDir.exists()) {
        logManager.log(
            Level.SEVERE,
            () ->
                "FAIL. No sequentialization created, make sure the target directory exists in"
                    + " CPAchecker: "
                    + targetDirectory);

        // ensure the file does not exist already (no overwriting)
      } else if (!file.createNewFile()) {
        logManager.log(
            Level.SEVERE,
            () ->
                "FAIL. No sequentialization created, file exists already: "
                    + file.getAbsolutePath());
      } else {
        logManager.log(
            Level.INFO, () -> "SUCCESS. Sequentialization created: " + file.getAbsolutePath());
        // write content to the file
        Writer writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8);
        writer.write(sequentialization);
        writer.close();
      }
    } catch (IOException e) {
      logManager.log(
          Level.SEVERE,
          () -> "An IO error occurred while writing the sequentialization: " + e.getMessage());
    }
  }
}
