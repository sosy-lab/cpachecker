// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.output;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;

/** A class to write the sequentialized program to a file. */
public class MPORWriter {

  enum FileExtension {
    I(".i"),
    YML(".yml");

    private final String suffix;

    FileExtension(String pSuffix) {
      suffix = pSuffix;
    }

    private String getSuffix() {
      return suffix;
    }
  }

  private enum OutputMessage {
    IO_ERROR("An IO error occurred while writing the output program."),
    OVERWRITE_ERROR("File exists already:"),
    SEQUENTIALIZATION_CREATED("Sequentialization created in:");

    private final String message;

    OutputMessage(String pMessage) {
      message = pMessage;
    }

    private String getMessage() {
      return message;
    }
  }

  public static void write(
      MPOROptions pOptions, String pOutputProgram, List<Path> pInputFilePaths, LogManager pLogger) {

    try {
      // use first input file name as output program name
      String programName =
          SeqNameUtil.getFileNameWithoutExtension(pInputFilePaths.getFirst().getFileName());

      // write output program
      Path programPath = buildOutputPath(pOptions, programName, FileExtension.I, pLogger);
      try (Writer writer = IO.openOutputFile(programPath, Charset.defaultCharset())) {
        writer.write(pOutputProgram);
        pLogger.log(
            Level.INFO,
            OutputMessage.SEQUENTIALIZATION_CREATED.getMessage(),
            programPath.toString());
      }

      // if enabled: write metadata file
      MetadataWriter.tryWrite(pOptions, programName, pInputFilePaths, pLogger);

    } catch (IOException e) {
      pLogger.logUserException(Level.SEVERE, e, OutputMessage.IO_ERROR.getMessage());
    }
  }

  static Path buildOutputPath(
      MPOROptions pOptions, String pProgramName, FileExtension pFileExtension, LogManager pLogger) {

    String templateWithExtension = pOptions.outputPath().getTemplate() + pFileExtension.getSuffix();
    Path rOutputPath = PathTemplate.ofFormatString(templateWithExtension).getPath(pProgramName);
    handleOverwriting(pOptions, rOutputPath, pLogger);
    return rOutputPath;
  }

  /**
   * Throws an {@link AssertionError} if {@code pOutputProgramPath} exists already and overwriting
   * is not allowed in {@code pOptions}.
   */
  private static void handleOverwriting(
      MPOROptions pOptions, Path pOutputProgramPath, LogManager pLogger) {

    if (!pOptions.overwriteFiles()) {
      if (Files.exists(pOutputProgramPath)) {
        pLogger.log(Level.SEVERE, OutputMessage.OVERWRITE_ERROR.getMessage(), pOutputProgramPath);
        // assertion error is required to stop execution and prevent any overwriting
        throw new AssertionError();
      }
    }
  }
}
