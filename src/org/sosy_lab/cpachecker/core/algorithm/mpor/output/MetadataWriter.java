// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.output;

import static org.sosy_lab.cpachecker.core.algorithm.mpor.output.MPORWriter.handleOutputMessage;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.output.MPORWriter.OutputMessage;

class MetadataWriter {

  private record InputFileRecord(
      @JsonProperty("name") String pName, @JsonProperty("path") String pPath) {}

  private record MetadataRecord(
      @JsonProperty("cpachecker_version") String pCpaCheckerVersion,
      @JsonProperty("utc_creation_time") String pUtcCreationTime,
      @JsonProperty("input_files") List<InputFileRecord> pInputFiles) {}

  private record RootRecord(
      @JsonProperty("metadata") MetadataRecord pMetadata,
      @JsonProperty("algorithm_options") Map<String, Object> pAlgorithmOptions) {}

  static void write(
      MPOROptions pOptions, String pMetadataPath, List<Path> pInputFilePaths, LogManager pLogger)
      throws IOException {

    YAMLMapper yamlMapper = new YAMLMapper();
    File metadataFile = new File(pMetadataPath);
    RootRecord yamlRoot = buildMetadataYamlRoot(pInputFilePaths, pOptions, pLogger);
    yamlMapper.writeValue(metadataFile, yamlRoot);
  }

  private static RootRecord buildMetadataYamlRoot(
      List<Path> pInputFilePaths, MPOROptions pOptions, LogManager pLogger) {

    MetadataRecord metadata = buildMetadataRecord(pInputFilePaths);
    ImmutableMap<String, Object> algorithmOptions = buildAlgorithmOptionMap(pOptions, pLogger);
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

  private static ImmutableMap<String, Object> buildAlgorithmOptionMap(
      MPOROptions pOptions, LogManager pLogger) {

    ImmutableMap.Builder<String, Object> rMap = ImmutableMap.builder();
    for (Field field : pOptions.getClass().getDeclaredFields()) {
      try {
        // allow access to the MPOROptions record private final fields
        field.setAccessible(true);
        rMap.put(field.getName(), field.get(pOptions));
      } catch (IllegalAccessException e) {
        handleOutputMessage(
            Level.SEVERE,
            OutputMessage.OPTION_ACCESS_ERROR,
            e.getMessage() + ". Field name: " + field.getName(),
            pLogger);
      }
    }
    return rMap.buildOrThrow();
  }
}
