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
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.validation.SeqValidator;
import org.sosy_lab.cpachecker.exceptions.ParserException;

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

  private final ShutdownNotifier shutdownNotifier;

  private final LogManager logger;

  private final List<Path> inputFilePaths;

  private final MPOROptions options;

  private final String sequentializationPath;

  private final String metadataPath;

  public SeqWriter(
      ShutdownNotifier pShutdownNotifier,
      LogManager pLogger,
      String pSeqName,
      List<Path> pInputFilePaths,
      MPOROptions pOptions) {

    shutdownNotifier = pShutdownNotifier;
    logger = pLogger;
    inputFilePaths = pInputFilePaths;
    options = pOptions;
    // TODO use .c suffix if both input function and type declarations are disabled
    sequentializationPath = pOptions.outputPath + pSeqName + FileExtension.I.suffix;
    metadataPath = pOptions.outputPath + pSeqName + FileExtension.YML.suffix;
  }

  public void write(final String pFinalSeq) {
    try {
      File seqProgramFile = new File(sequentializationPath);
      File parentDir = seqProgramFile.getParentFile();

      handleDirectoryCreation(parentDir);
      // option: no overwriting
      handleOverwriting(seqProgramFile);

      // write sequentialized program to file
      Path filePath = seqProgramFile.toPath();
      try (Writer writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
        writer.write(pFinalSeq);
        // option: create metadata file
        handleMetadata(metadataPath);
        // option: validate that CPAchecker can parse output
        if (options.validateParse && !options.inputTypeDeclarations) {
          handleParsing();
        } else {
          logger.log(Level.INFO, SUCCESS, sequentializationPath);
        }
      }

    } catch (IOException e) {
      logger.log(Level.SEVERE, OutputError.IO.message, e.getMessage());
      throw new RuntimeException();
    }
  }

  private void handleDirectoryCreation(File pParentDir) {
    if (!pParentDir.exists()) {
      if (pParentDir.mkdirs()) {
        logger.log(Level.INFO, "Directory created: " + options.outputPath);
      } else {
        logger.log(Level.SEVERE, OutputError.TARGET_DIR.message, options.outputPath);
        throw new RuntimeException();
      }
    }
  }

  private void handleOverwriting(File pSeqProgramFile) throws IOException {
    // ensure the file does not exist already (if overwriteFiles is false)
    if (!pSeqProgramFile.createNewFile() && !options.overwriteFiles) {
      logger.log(
          Level.SEVERE, OutputError.NO_OVERWRITING.message, pSeqProgramFile.getAbsolutePath());
      throw new RuntimeException();
    }
  }

  private void handleParsing() throws IOException {
    // we only parse the output if type declarations are disabled
    if (options.validateParse && !options.inputTypeDeclarations) {

      Path seqPath = Path.of(sequentializationPath);
      try {
        SeqValidator.validateProgramParsing(seqPath, options, shutdownNotifier, logger);
        logger.log(Level.INFO, SUCCESS, sequentializationPath);

      } catch (InvalidConfigurationException | ParserException | InterruptedException e) {
        // delete output again if parsing fails
        Files.delete(seqPath);
        Files.delete(Path.of(metadataPath));
        logger.log(Level.SEVERE, "Error while parsing sequentialization:", e.getMessage());
        throw new RuntimeException(e.getMessage());

      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private void handleMetadata(String pMetadataPath) {
    if (options.outputMetadata) {
      File seqMetadataFile = new File(pMetadataPath);
      try (Writer writer =
          Files.newBufferedWriter(seqMetadataFile.toPath(), StandardCharsets.UTF_8)) {
        writer.write(createMetadata());
      } catch (IllegalAccessException e) {
        logger.log(Level.SEVERE, OutputError.OPTIONS_ILLEGAL_ACCESS.message, e);
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
