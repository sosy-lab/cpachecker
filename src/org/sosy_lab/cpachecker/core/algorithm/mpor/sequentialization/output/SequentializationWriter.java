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
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;

public class SequentializationWriter {

  public enum FileExtension {
    I(".i"),
    YML(".yml");

    public final String suffix;

    FileExtension(String pSuffix) {
      suffix = pSuffix;
    }
  }

  private enum Error {
    IO("MPOR FAIL. An IO error occurred while writing the outputProgram:"),
    NO_OVERWRITING("MPOR FAIL. File exists already:"),
    OPTIONS_ILLEGAL_ACCESS("MPOR FAIL. Could not retrieve MPOROptions fields."),
    TARGET_DIR("MPOR FAIL. Could not create target directory:");

    final String message;

    Error(String pMessage) {
      message = pMessage;
    }
  }

  private static final String targetDirectory = "output/";

  private final LogManager logManager;

  private final List<Path> inputFilePaths;

  private final MPOROptions options;

  private final String seqProgramPath;

  private final String seqMetadataPath;

  public SequentializationWriter(
      LogManager pLogManager, String pSeqName, List<Path> pInputFilePaths, MPOROptions pOptions) {

    logManager = pLogManager;
    inputFilePaths = pInputFilePaths;
    options = pOptions;
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
          logManager.log(Level.SEVERE, Error.TARGET_DIR.message, targetDirectory);
          throw new RuntimeException();
        }
        // ensure the file does not exist already (no overwriting)
      } else if (!seqProgramFile.createNewFile() && !pOptions.fileOverwriting) {
        logManager.log(
            Level.SEVERE, Error.NO_OVERWRITING.message, seqProgramFile.getAbsolutePath());
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
        } catch (IllegalAccessException pE) {
          logManager.log(Level.SEVERE, Error.OPTIONS_ILLEGAL_ACCESS);
          throw new RuntimeException();
        }
        logManager.log(
            Level.INFO,
            () -> "MPOR SUCCESS. Sequentialization created: " + seqProgramFile.getAbsolutePath());
      }
    } catch (IOException e) {
      logManager.log(Level.SEVERE, Error.IO.message, e.getMessage());
      throw new RuntimeException();
    }
  }

  private String createMetadata() throws IllegalAccessException {
    StringBuilder rMetadata = new StringBuilder();
    rMetadata.append("input_files:\n");
    for (Path inputFilePath : inputFilePaths) {
      rMetadata.append(createNameAndPathEntry(inputFilePath));
    }
    rMetadata.append("\n");
    rMetadata.append("options:\n");
    for (Field field : options.getClass().getDeclaredFields()) {
      rMetadata.append("  ").append(field.getName()).append(": ");
      rMetadata.append(field.get(options)).append("\n");
    }
    return rMetadata.toString();
  }

  private String createNameAndPathEntry(Path pPath) {
    return "  - name: " + pPath.getFileName() + "\n" + "    path: " + pPath + "\n";
  }
}
