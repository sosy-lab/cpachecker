// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization;

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

/** A class to write the sequentialized program to a file. */
public class SeqWriter {

  public enum FileExtension {
    I(".i"),
    YML(".yml");

    public final String suffix;

    FileExtension(String pSuffix) {
      suffix = pSuffix;
    }
  }

  private enum OutputError {
    IO("MPOR FAIL. An IO error occurred while writing the outputProgram:"),
    NO_OVERWRITING("MPOR FAIL. File exists already:"),
    OPTIONS_ILLEGAL_ACCESS("MPOR FAIL. Could not retrieve MPOROptions fields:"),
    TARGET_DIR("MPOR FAIL. Could not create target directory:");

    final String message;

    OutputError(String pMessage) {
      message = pMessage;
    }
  }

  private static final String SUCCESS = "MPOR SUCCESS. Sequentialization created in:";

  public static final String DEFAULT_OUTPUT_PATH = "output/";

  private final LogManager logManager;

  private final List<Path> inputFilePaths;

  private final MPOROptions options;

  private final String seqProgramPath;

  private final String metadataPath;

  public SeqWriter(
      LogManager pLogManager, String pSeqName, List<Path> pInputFilePaths, MPOROptions pOptions) {

    logManager = pLogManager;
    inputFilePaths = pInputFilePaths;
    options = pOptions;
    seqProgramPath = pOptions.outputPath + pSeqName + FileExtension.I.suffix;
    metadataPath = pOptions.outputPath + pSeqName + FileExtension.YML.suffix;
  }

  public void write(final String pFinalSeq) {
    try {
      File seqProgramFile = new File(seqProgramPath);
      File parentDir = seqProgramFile.getParentFile();

      handleDirectoryCreation(parentDir);
      // option: no overwriting
      handleOverwriting(seqProgramFile);

      // write sequentialized program to file
      try (Writer writer =
          Files.newBufferedWriter(seqProgramFile.toPath(), StandardCharsets.UTF_8)) {
        writer.write(pFinalSeq);
        logManager.log(Level.INFO, SUCCESS, seqProgramPath);
      }

      // option: create metadata file
      handleMetadata(metadataPath);

    } catch (IOException e) {
      logManager.log(Level.SEVERE, OutputError.IO.message, e.getMessage());
      throw new RuntimeException();
    }
  }

  private void handleDirectoryCreation(File pParentDir) {
    if (!pParentDir.exists()) {
      if (pParentDir.mkdirs()) {
        logManager.log(Level.INFO, "Directory created: " + options.outputPath);
      } else {
        logManager.log(Level.SEVERE, OutputError.TARGET_DIR.message, options.outputPath);
        throw new RuntimeException();
      }
    }
  }

  private void handleOverwriting(File pSeqProgramFile) throws IOException {
    // ensure the file does not exist already (if overwriteFiles is false)
    if (!pSeqProgramFile.createNewFile() && !options.overwriteFiles) {
      logManager.log(
          Level.SEVERE, OutputError.NO_OVERWRITING.message, pSeqProgramFile.getAbsolutePath());
      throw new RuntimeException();
    }
  }

  private void handleMetadata(String pMetadataPath) {
    if (options.outputMetadata) {
      File seqMetadataFile = new File(pMetadataPath);
      try (Writer writer =
          Files.newBufferedWriter(seqMetadataFile.toPath(), StandardCharsets.UTF_8)) {
        writer.write(createMetadata());
      } catch (IllegalAccessException e) {
        logManager.log(Level.SEVERE, OutputError.OPTIONS_ILLEGAL_ACCESS.message, e);
        throw new RuntimeException();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private String createMetadata() throws IllegalAccessException {
    StringBuilder rMetadata = new StringBuilder();
    rMetadata.append("input_files:\n");
    for (Path inputFilePath : inputFilePaths) {
      rMetadata.append(createNameAndPathEntry(inputFilePath));
    }
    rMetadata.append("\n");
    rMetadata.append("algorithm_options:\n");
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
