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
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;

public class SequentializationWriter {

  private enum WriterError {
    IO("MPOR FAIL. An IO error occurred while writing the outputProgram: "),
    NO_OVERWRITING("MPOR FAIL. No sequentialization created, file exists already: "),
    TARGET_DIR("MPOR FAIL. No sequentialization created, could not create target directory: %s");

    final String message;

    WriterError(String pMessage) {
      message = pMessage;
    }
  }

  private static final String targetDirectory = "output/";

  public enum FileExtension {
    I(".i"),
    YML(".yml");

    public final String suffix;

    FileExtension(String pSuffix) {
      suffix = pSuffix;
    }
  }

  private final LogManager logManager;

  private final Path inputFilePath;

  private final String seqProgramPath;

  private final String seqMetadataPath;

  public SequentializationWriter(LogManager pLogManager, String pSeqName, Path pInputFilePath) {
    logManager = pLogManager;
    inputFilePath = pInputFilePath;
    seqProgramPath = targetDirectory + pSeqName + FileExtension.I.suffix;
    seqMetadataPath = targetDirectory + pSeqName + FileExtension.YML.suffix;
  }

  public void write(final String pFinalSeq, MPOROptions pOptions) {
    try {
      File seqProgramFile = new File(seqProgramPath);
      File parentDir = seqProgramFile.getParentFile();
      // ensure the target directory exists
      if (!parentDir.exists()) {
        if (parentDir.mkdirs()) {
          logManager.log(Level.INFO, "Directory created: " + targetDirectory);
        } else {
          logManager.log(Level.SEVERE, WriterError.TARGET_DIR.message, targetDirectory);
          throw new RuntimeException();
        }
        // ensure the file does not exist already (no overwriting)
      } else if (!seqProgramFile.createNewFile() && !pOptions.fileOverwriting) {
        logManager.log(
            Level.SEVERE, WriterError.NO_OVERWRITING.message, seqProgramFile.getAbsolutePath());
        throw new RuntimeException();
      } else {
        // write content to the file
        try (Writer writer =
            Files.newBufferedWriter(seqProgramFile.toPath(), StandardCharsets.UTF_8)) {
          writer.write(pFinalSeq);
        }
        File seqMetadataFile = new File(seqMetadataPath);
        try (Writer writer =
            Files.newBufferedWriter(seqMetadataFile.toPath(), StandardCharsets.UTF_8)) {
          writer.write(createMetadata());
        }
        logManager.log(
            Level.INFO,
            () -> "MPOR SUCCESS. Sequentialization created: " + seqProgramFile.getAbsolutePath());
      }
    } catch (IOException e) {
      logManager.log(Level.SEVERE, WriterError.IO.message, e.getMessage());
      throw new RuntimeException();
    }
  }

  private String createMetadata() {
    return "input_file_path : '"
        + inputFilePath
        + "'\n"
        + "input_file : '"
        + inputFilePath.getFileName()
        + "'\n";
  }
}
