// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.output;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.validation.SeqValidator;
import org.sosy_lab.cpachecker.exceptions.ParserException;

/** A class to write the sequentialized program to a file. */
public class MPORWriter {

  public enum FileExtension {
    I(".i"),
    YML(".yml");

    public final String suffix;

    FileExtension(String pSuffix) {
      suffix = pSuffix;
    }
  }

  private enum OutputMessageType {
    FAIL("MPOR FAIL. "),
    INFO("MPOR INFO. "),
    SUCCESS("MPOR SUCCESS. ");

    final String prefix;

    OutputMessageType(String pPrefix) {
      prefix = pPrefix;
    }
  }

  private enum OutputMessage {
    DIRECTORY_CREATED(OutputMessageType.INFO, "Directory created:"),
    IO_ERROR(OutputMessageType.FAIL, "An IO error occurred while writing the output program:"),
    OPTION_ACCESS_ERROR(OutputMessageType.FAIL, "Could not access algorithm option fields:"),
    OVERWRITE_ERROR(OutputMessageType.FAIL, "File exists already:"),
    PARSE_ERROR(OutputMessageType.FAIL, "Error while parsing sequentialization:"),
    SEQUENTIALIZATION_CREATED(OutputMessageType.SUCCESS, "Sequentialization created in:"),
    TARGET_DIRECTORY_ERROR(OutputMessageType.FAIL, "Could not create target directory:");

    final OutputMessageType type;

    final String message;

    OutputMessage(OutputMessageType pType, String pMessage) {
      type = pType;
      message = pMessage;
    }

    String getMessage() {
      return type.prefix + message;
    }
  }

  public static final String DEFAULT_OUTPUT_PATH = "output/";

  public static void write(
      MPOROptions pOptions,
      String pOutputProgram,
      String pOutputProgramName,
      String pOutputProgramPath,
      List<Path> pInputFilePaths,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier) {

    try {
      String metadataPath = buildPath(pOptions, pOutputProgramName, FileExtension.YML);
      File outputProgramFile = new File(pOutputProgramPath);
      File parentDir = outputProgramFile.getParentFile();

      handleDirectoryCreation(pOptions, parentDir, pLogger);
      handleOverwriting(pOptions, outputProgramFile, pLogger);

      // write sequentialized program to file
      Path filePath = outputProgramFile.toPath();
      try (Writer writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
        writer.write(pOutputProgram);
        // option: validate that CPAchecker can parse output
        if (pOptions.validateParse() && !pOptions.inputTypeDeclarations()) {
          handleParsing(pOptions, pOutputProgramPath, metadataPath, pLogger, pShutdownNotifier);
        }
        // option: create metadata file
        if (pOptions.outputMetadata()) {
          MetadataWriter.write(pOptions, metadataPath, pInputFilePaths);
        }
        handleOutputMessage(
            Level.INFO, OutputMessage.SEQUENTIALIZATION_CREATED, pOutputProgramPath, pLogger);
      }

    } catch (IOException e) {
      handleOutputMessage(Level.SEVERE, OutputMessage.IO_ERROR, e.getMessage(), pLogger);

    } catch (IllegalAccessException e) {
      handleOutputMessage(Level.SEVERE, OutputMessage.OPTION_ACCESS_ERROR, e.getMessage(), pLogger);
    }
  }

  public static String buildPath(
      MPOROptions pOptions, String pOutputFileName, FileExtension pFileExtension) {

    return pOptions.outputPath() + pOutputFileName + pFileExtension.suffix;
  }

  private static void handleOutputMessage(
      Level pLevel, OutputMessage pOutputMessage, String pMessage, LogManager pLogger) {

    pLogger.log(pLevel, pOutputMessage.getMessage(), pMessage);
    if (pOutputMessage.type.equals(OutputMessageType.FAIL)) {
      throw new RuntimeException(pMessage);
    }
  }

  private static void handleDirectoryCreation(
      MPOROptions pOptions, File pParentDir, LogManager pLogger) {

    if (!pParentDir.exists()) {
      String outputPath = pOptions.outputPath();
      if (pParentDir.mkdirs()) {
        handleOutputMessage(Level.INFO, OutputMessage.DIRECTORY_CREATED, outputPath, pLogger);
      } else {
        handleOutputMessage(
            Level.SEVERE, OutputMessage.TARGET_DIRECTORY_ERROR, outputPath, pLogger);
      }
    }
  }

  private static void handleOverwriting(
      MPOROptions pOptions, File pOutputProgramFile, LogManager pLogger) throws IOException {

    // ensure the file does not exist already (if overwriting is disabled)
    if (!pOutputProgramFile.createNewFile() && !pOptions.overwriteFiles()) {
      handleOutputMessage(
          Level.SEVERE,
          OutputMessage.OVERWRITE_ERROR,
          pOutputProgramFile.getAbsolutePath(),
          pLogger);
    }
  }

  private static void handleParsing(
      MPOROptions pOptions,
      String pOutputProgramPath,
      String pMetadataPath,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier)
      throws IOException {

    Path seqPath = Path.of(pOutputProgramPath);
    try {
      SeqValidator.validateProgramParsing(seqPath, pOptions, pLogger, pShutdownNotifier);
      handleOutputMessage(
          Level.INFO, OutputMessage.SEQUENTIALIZATION_CREATED, pOutputProgramPath, pLogger);

    } catch (InvalidConfigurationException
        | ParserException
        | InterruptedException
        | IOException e) {
      // delete output again if parsing fails
      Files.delete(seqPath);
      Files.delete(Path.of(pMetadataPath));
      handleOutputMessage(Level.SEVERE, OutputMessage.PARSE_ERROR, e.getMessage(), pLogger);
    }
  }
}
