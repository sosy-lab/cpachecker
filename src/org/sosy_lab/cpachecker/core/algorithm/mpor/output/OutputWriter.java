// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.output;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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
import org.sosy_lab.cpachecker.core.algorithm.mpor.output.metadata.InputFileRecord;
import org.sosy_lab.cpachecker.core.algorithm.mpor.output.metadata.MetadataRecord;
import org.sosy_lab.cpachecker.core.algorithm.mpor.output.metadata.RootRecord;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.validation.SeqValidator;
import org.sosy_lab.cpachecker.exceptions.ParserException;

/** A class to write the sequentialized program to a file. */
public class OutputWriter {

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

    } catch (IllegalAccessException e) {
      handleOutputMessage(Level.SEVERE, OutputMessageType.OPTION_ACCESS_ERROR, e.getMessage());
      throw new RuntimeException();
    }
  }

  private void handleOutputMessage(Level pLevel, OutputMessageType pType, String pMessage) {
    // TODO if pType is fail, throw assertion error
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

  private void handleMetadata(String pMetadataPath) throws IOException, IllegalAccessException {
    if (options.outputMetadata) {
      YAMLMapper yamlMapper = new YAMLMapper();
      File metadataFile = new File(pMetadataPath);
      RootRecord yamlRoot = buildMetadataYamlRoot(inputFilePaths, options);
      yamlMapper.writeValue(metadataFile, yamlRoot);
    }
  }

  private static RootRecord buildMetadataYamlRoot(List<Path> pInputFilePaths, MPOROptions pOptions)
      throws IllegalAccessException {

    MetadataRecord metadata = buildMetadataRecord(pInputFilePaths);
    ImmutableMap<String, Object> algorithmOptions = buildAlgorithmOptionMap(pOptions);
    return new RootRecord(metadata, algorithmOptions);
  }

  private static MetadataRecord buildMetadataRecord(List<Path> pInputFilePaths) {
    String utcCreationTime = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
    ImmutableList<InputFileRecord> inputFiles = buildInputFileRecords(pInputFilePaths);
    return new MetadataRecord(CPAchecker.getPlainVersion(), utcCreationTime, inputFiles);
  }

  private static ImmutableList<InputFileRecord> buildInputFileRecords(List<Path> pInputFilePaths) {
    ImmutableList.Builder<InputFileRecord> rInputFileRecords = ImmutableList.builder();
    for (Path path : pInputFilePaths) {
      rInputFileRecords.add(new InputFileRecord(path.getFileName().toString(), path.toString()));
    }
    return rInputFileRecords.build();
  }

  private static ImmutableMap<String, Object> buildAlgorithmOptionMap(MPOROptions pOptions)
      throws IllegalAccessException {

    ImmutableMap.Builder<String, Object> rMap = ImmutableMap.builder();
    for (Field field : pOptions.getClass().getDeclaredFields()) {
      rMap.put(field.getName(), field.get(pOptions));
    }
    return rMap.build();
  }
}
