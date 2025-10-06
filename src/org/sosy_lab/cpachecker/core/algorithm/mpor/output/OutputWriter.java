// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.output;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.validation.SeqValidator;
import org.sosy_lab.cpachecker.exceptions.ParserException;

// TODO use records and jackson for exporting .yml metadata
/** A class to write the sequentialized program to a file. */
public class OutputWriter {

  private static final int YML_TAB_SIZE = 2;

  public enum FileExtension {
    I(".i"),
    YML(".yml");

    public final String suffix;

    FileExtension(String pSuffix) {
      suffix = pSuffix;
    }
  }

  private enum OutputMessagePrefix {
    FAIL("MPOR FAIL. "),
    INFO("MPOR INFO. "),
    SUCCESS("MPOR SUCCESS. ");

    final String string;

    OutputMessagePrefix(String pString) {
      string = pString;
    }
  }

  private enum OutputMessageType {
    DIRECTORY_CREATED(OutputMessagePrefix.INFO, "Directory created:"),
    IO_ERROR(OutputMessagePrefix.FAIL, "An IO error occurred while writing the output program:"),
    OPTION_ACCESS_ERROR(OutputMessagePrefix.FAIL, "Could not access algorithm option fields:"),
    OVERWRITE_ERROR(OutputMessagePrefix.FAIL, "File exists already:"),
    PARSE_ERROR(OutputMessagePrefix.FAIL, "Error while parsing sequentialization:"),
    SEQUENTIALIZATION_CREATED(OutputMessagePrefix.SUCCESS, "Sequentialization created in:"),
    TARGET_DIRECTORY_ERROR(OutputMessagePrefix.FAIL, "Could not create target directory:");

    final OutputMessagePrefix prefix;

    final String suffix;

    OutputMessageType(OutputMessagePrefix pPrefix, String pSuffix) {
      prefix = pPrefix;
      suffix = pSuffix;
    }

    String getMessage() {
      return prefix.string + suffix;
    }
  }

  public static final String DEFAULT_OUTPUT_PATH = "output/";

  private final ShutdownNotifier shutdownNotifier;

  private final LogManager logger;

  private final List<Path> inputFilePaths;

  private final MPOROptions options;

  private final String sequentializationPath;

  private final String metadataPath;

  public OutputWriter(
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
          handleOutputMessage(
              Level.INFO, OutputMessageType.SEQUENTIALIZATION_CREATED, sequentializationPath);
        }
      }

    } catch (IOException e) {
      handleOutputMessage(Level.SEVERE, OutputMessageType.IO_ERROR, e.getMessage());
      throw new RuntimeException();
    }
  }

  private void handleOutputMessage(Level pLevel, OutputMessageType pType, String pMessage) {
    logger.log(pLevel, pType.getMessage(), pMessage);
  }

  private void handleDirectoryCreation(File pParentDir) {
    if (!pParentDir.exists()) {
      String outputPath = options.outputPath;
      if (pParentDir.mkdirs()) {
        handleOutputMessage(Level.INFO, OutputMessageType.DIRECTORY_CREATED, outputPath);
      } else {
        handleOutputMessage(Level.SEVERE, OutputMessageType.TARGET_DIRECTORY_ERROR, outputPath);
        throw new RuntimeException();
      }
    }
  }

  private void handleOverwriting(File pOutputProgramFile) throws IOException {
    // ensure the file does not exist already (if overwriteFiles is false)
    if (!pOutputProgramFile.createNewFile() && !options.overwriteFiles) {
      handleOutputMessage(
          Level.SEVERE, OutputMessageType.OVERWRITE_ERROR, pOutputProgramFile.getAbsolutePath());
      throw new RuntimeException();
    }
  }

  private void handleParsing() throws IOException {
    // we only parse the output if type declarations are disabled
    if (options.validateParse && !options.inputTypeDeclarations) {

      Path seqPath = Path.of(sequentializationPath);
      try {
        SeqValidator.validateProgramParsing(seqPath, options, shutdownNotifier, logger);
        handleOutputMessage(
            Level.INFO, OutputMessageType.SEQUENTIALIZATION_CREATED, sequentializationPath);

      } catch (InvalidConfigurationException | ParserException | InterruptedException e) {
        // delete output again if parsing fails
        Files.delete(seqPath);
        Files.delete(Path.of(metadataPath));
        handleOutputMessage(Level.SEVERE, OutputMessageType.PARSE_ERROR, e.getMessage());
        throw new RuntimeException(e.getMessage());

      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private void handleMetadata(String pMetadataPath) {
    if (options.outputMetadata) {
      File seqMetadataFile = new File(pMetadataPath);
      Path path = seqMetadataFile.toPath();
      try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
        writer.write(createMetadata());
      } catch (IllegalAccessException e) {
        handleOutputMessage(Level.SEVERE, OutputMessageType.OPTION_ACCESS_ERROR, e.getMessage());
        throw new RuntimeException();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private String createMetadata() throws IllegalAccessException {
    StringBuilder rMetadata = new StringBuilder();
    rMetadata.append("metadata:\n");
    rMetadata.append(createCPAcheckerVersionEntry());
    rMetadata.append(createCreationTimeEntry());
    rMetadata.append(buildYmlTab(1)).append("input_files:\n");
    for (Path path : inputFilePaths) {
      rMetadata.append(createInputFileNameEntry(path.getFileName()));
      rMetadata.append(createInputFilePathEntry(path));
    }
    rMetadata.append("\n");
    rMetadata.append("algorithm_options:\n");
    for (Field field : options.getClass().getDeclaredFields()) {
      rMetadata.append(buildYmlTab(1)).append(field.getName()).append(": ");
      rMetadata.append(field.get(options)).append("\n");
    }
    return rMetadata.toString();
  }

  private String createCPAcheckerVersionEntry() {
    return buildYmlTab(1) + "cpachecker_version: " + CPAchecker.getPlainVersion() + "\n";
  }

  private String createCreationTimeEntry() {
    Instant now = Instant.now(); // retrieve current UTC time
    String date = DateTimeFormatter.ISO_INSTANT.format(now); // format in ISO 8601
    return buildYmlTab(1) + "utc_creation_time: " + date + "\n";
  }

  private String createInputFileNameEntry(Path pFileName) {
    return buildYmlTab(1) + "- name: " + pFileName + "\n";
  }

  private String createInputFilePathEntry(Path pPath) {
    return buildYmlTab(2) + "path: " + pPath + "\n";
  }

  private static String buildYmlTab(int pTabs) {
    checkArgument(pTabs >= 0, "pTabs must be >= 0");
    return " ".repeat(YML_TAB_SIZE).repeat(pTabs);
  }
}
